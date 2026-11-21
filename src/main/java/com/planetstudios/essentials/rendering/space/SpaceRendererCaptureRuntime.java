package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.PlanetEssentials;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Bridges vanilla section rebuild output into bounded Space Renderer CPU workers and upload handoff. */
public final class SpaceRendererCaptureRuntime {
    private static final int WORKERS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    private static final int MAX_IN_FLIGHT = 96;
    private static final int MAX_UPLOAD_ENTRIES = 8192;
    private static final long MAX_UPLOAD_BYTES = 128L * 1024 * 1024;
    private static final SpaceRendererSectionManager SECTIONS = new SpaceRendererSectionManager();
    private static final SpaceRendererMeshBuildScheduler<SpaceRendererMesh> SCHEDULER =
            new SpaceRendererMeshBuildScheduler<>(WORKERS, MAX_IN_FLIGHT, threadFactory());
    private static final SpaceRendererGpuUploadQueue<SpaceRendererMesh> UPLOADS =
            new SpaceRendererGpuUploadQueue<>(MAX_UPLOAD_ENTRIES, MAX_UPLOAD_BYTES);
    private static final AtomicLong REJECTED_UPLOADS = new AtomicLong();
    private static final AtomicLong FAILED_GPU_UPLOADS = new AtomicLong();
    private static final SpaceRendererGpuCache GPU_CACHE = new SpaceRendererGpuCache();
    private static volatile boolean closed;

    private SpaceRendererCaptureRuntime() {}

    public static boolean isCaptureEnabled() { return !closed; }

    public static SpaceRendererSectionManager.BuildToken beginRebuild(ClientLevel level, SectionPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        var key = key(level, pos);
        UPLOADS.removeSection(key);
        return SECTIONS.beginBuild(key);
    }

    public static boolean publish(SpaceRendererSectionManager.BuildToken token, PrismTerrainVertexData data) {
        if (closed || token == null || data == null || !SECTIONS.isCurrent(token)) return false;
        return SCHEDULER.submit(token, 0.0, () -> SpaceRendererMesh.build(data));
    }

    /** Drain on render thread; revision checks discard obsolete worker results before queueing uploads. */
    public static int drainCompletions() {
        if (closed) return 0;
        var results = SCHEDULER.drainCompletions(MAX_IN_FLIGHT);
        int queued = 0;
        for (var result : results) {
            if (!result.succeeded()) {
                PlanetEssentials.LOGGER.warn("Space Renderer CPU mesh preparation failed for {}",
                        result.token().key(), result.failure());
                continue;
            }
            if (!SECTIONS.isCurrent(result.token())) continue;
            SpaceRendererMesh mesh = result.value();
            String queueMaterialKey = mesh.materialId() + "#" + mesh.source().stage().name();
            if (UPLOADS.offer(result.token(), queueMaterialKey, mesh, mesh.source().estimatedCpuBytes())) {
                queued++;
            } else {
                REJECTED_UPLOADS.incrementAndGet();
            }
        }
        return queued;
    }

    /** Render-thread consumer takes a bounded batch and performs the actual graphics upload. */
    public static List<SpaceRendererGpuUploadQueue.UploadRequest<SpaceRendererMesh>> drainUploadBatch(
            int maxRequests, long maxBytes) {
        if (closed) return List.of();
        return UPLOADS.pollBatch(maxRequests, maxBytes);
    }

    public static int uploadPending(GpuDevice device, int maxRequests, long maxBytes) {
        RenderSystem.assertOnRenderThread();
        if (closed) return 0;
        int uploaded = 0;
        for (var request : drainUploadBatch(maxRequests, maxBytes)) {
            if (!SECTIONS.isCurrent(request.token())) continue;
            try {
                if (GPU_CACHE.upload(device, request.token(), request.payload())) uploaded++;
                else REJECTED_UPLOADS.incrementAndGet();
            } catch (RuntimeException failure) {
                FAILED_GPU_UPLOADS.incrementAndGet();
                PlanetEssentials.LOGGER.warn("Space Renderer GPU upload failed for {}", request.token().key(), failure);
            }
        }
        return uploaded;
    }
    public static int gpuMeshCount() { RenderSystem.assertOnRenderThread(); return GPU_CACHE.size(); }
    public static long gpuBytes() { RenderSystem.assertOnRenderThread(); return GPU_CACHE.currentBytes(); }
    public static long failedGpuUploadCount() { return FAILED_GPU_UPLOADS.get(); }

    public static int pendingUploadCount() { return UPLOADS.size(); }
    public static long pendingUploadBytes() { return UPLOADS.queuedBytes(); }
    public static long rejectedUploadCount() { return REJECTED_UPLOADS.get(); }

    public static void unloadChunk(ClientLevel level, int chunkX, int minSectionY, int maxSectionY, int chunkZ) {
        String dimension = dimensionId(level);
        for (int y = minSectionY; y < maxSectionY; y++) {
            var key = new SpaceRendererSectionManager.SectionKey(dimension, chunkX, y, chunkZ);
            SECTIONS.unload(key);
            UPLOADS.removeSection(key);
        }
        queueGpu(() -> GPU_CACHE.invalidateChunk(dimension, chunkX, chunkZ));
    }

    public static void clearWorld(ClientLevel level) {
        if (level == null) return;
        String dimension = dimensionId(level);
        SECTIONS.unloadDimension(dimension);
        UPLOADS.removeDimension(dimension);
        queueGpu(() -> GPU_CACHE.clearDimension(dimension));
    }

    public static void clearAll() {
        SECTIONS.clearAll();
        UPLOADS.clear();
        queueGpu(GPU_CACHE::clear);
    }

    public static void shutdown() {
        if (closed) return;
        closed = true;
        clearAll();
        SCHEDULER.close();
    }

    private static void queueGpu(Runnable action) {
        if (RenderSystem.isOnRenderThread()) action.run();
        else RenderSystem.queueFencedTask(action::run);
    }
    private static SpaceRendererSectionManager.SectionKey key(ClientLevel level, SectionPos pos) {
        return new SpaceRendererSectionManager.SectionKey(dimensionId(level), pos.x(), pos.y(), pos.z());
    }

    private static String dimensionId(ClientLevel level) { return level.dimension().toString(); }

    private static ThreadFactory threadFactory() {
        AtomicInteger index = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "Space Renderer Mesh Builder-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
