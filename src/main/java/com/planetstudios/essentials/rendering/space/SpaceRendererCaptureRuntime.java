package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.PlanetEssentials;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVanillaDrawIndex;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final long MAX_SNAPSHOT_BYTES = 32L * 1024 * 1024;
    private static final long MAX_SORTED_INDEX_BYTES_PER_FRAME = 32L * 1024 * 1024;
    private static final int MAX_SORTED_INDEX_DRAWS_PER_FRAME = 4096;
    private static final SpaceRendererSectionManager SECTIONS = new SpaceRendererSectionManager();
    private static final SpaceRendererSectionSnapshotCache SNAPSHOTS =
            new SpaceRendererSectionSnapshotCache(MAX_SNAPSHOT_BYTES);
    private static final SpaceRendererMeshBuildScheduler<SpaceRendererMesh> SCHEDULER =
            new SpaceRendererMeshBuildScheduler<>(WORKERS, MAX_IN_FLIGHT, threadFactory());
    private static final SpaceRendererMeshBuildScheduler<java.util.Optional<List<SpaceRendererMesh>>> MODEL_SCHEDULER =
            new SpaceRendererMeshBuildScheduler<>(WORKERS, MAX_IN_FLIGHT, threadFactory());
    private static final SpaceRendererGpuUploadQueue<SpaceRendererMesh> UPLOADS =
            new SpaceRendererGpuUploadQueue<>(MAX_UPLOAD_ENTRIES, MAX_UPLOAD_BYTES);
    private static final AtomicLong REJECTED_UPLOADS = new AtomicLong();
    private static final AtomicLong REJECTED_MESH_BUILDS = new AtomicLong();
    private static final AtomicLong FAILED_GPU_UPLOADS = new AtomicLong();
    private static final AtomicLong FRAME_SEQUENCE = new AtomicLong();
    private static final SpaceRendererGpuCache GPU_CACHE = new SpaceRendererGpuCache();
    private static volatile SpaceRendererFrameSnapshot currentFrame = SpaceRendererFrameSnapshot.empty(1);
    private static final List<SpaceRendererSortedIndexBuffer> SORTED_INDEX_BUFFERS = new ArrayList<>();
    private static long sortedIndexBytes;
    private static volatile boolean closed;

    private SpaceRendererCaptureRuntime() {}

    public static boolean isCaptureEnabled() { return !closed; }

    public static SpaceRendererSectionManager.BuildToken beginRebuild(ClientLevel level, SectionPos pos) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(pos, "pos");
        var key = key(level, pos);
        SNAPSHOTS.remove(key);
        UPLOADS.removeSection(key);
        queueGpu(() -> GPU_CACHE.invalidateSection(key));
        return SECTIONS.beginBuild(key);
    }

    /** Publishes a copied block-state snapshot only while its section revision remains current. */
    public static boolean publishSnapshot(SpaceRendererSectionManager.BuildToken token,
            SpaceRendererSectionSnapshot snapshot) {
        if (closed || token == null || snapshot == null || !SECTIONS.isCurrent(token)) return false;
        if (!token.key().equals(snapshot.key())) {
            throw new IllegalArgumentException("Section snapshot does not match its build token");
        }
        if (!SNAPSHOTS.put(snapshot)) return false;
        if (!MODEL_SCHEDULER.submit(token, 0.0, () -> buildSectionMeshes(snapshot))) {
            REJECTED_MESH_BUILDS.incrementAndGet();
        }
        return true;
    }

    private static java.util.Optional<List<SpaceRendererMesh>> buildSectionMeshes(
            SpaceRendererSectionSnapshot snapshot) {
        List<SpaceRendererBlockModelMesher.LayeredPayload> batches = new ArrayList<>(
                SpaceRendererBlockModelMesher.build(snapshot).orElse(List.of()));
        SpaceRendererFluidMesher.buildTerrainPayload(snapshot).ifPresent(batches::add);
        if (batches.isEmpty()) return java.util.Optional.empty();

        Map<ChunkSectionLayer, List<PrismTerrainVertexData>> byLayer = new java.util.EnumMap<>(ChunkSectionLayer.class);
        for (var batch : batches) {
            byLayer.computeIfAbsent(batch.layer(), ignored -> new ArrayList<>()).add(batch.data());
        }
        List<SpaceRendererMesh> meshes = new ArrayList<>(byLayer.size());
        for (var entry : byLayer.entrySet()) {
            List<PrismTerrainVertexData> payloads = entry.getValue();
            PrismTerrainVertexData data;
            if (payloads.size() == 1) {
                data = payloads.getFirst();
            } else {
                PrismTerrainVertexData first = payloads.getFirst();
                var builder = PrismTerrainVertexData.builder(first.sectionX(), first.sectionY(), first.sectionZ(),
                        first.materialId(), first.stage());
                for (PrismTerrainVertexData payload : payloads) builder.append(payload);
                data = builder.build();
            }
            meshes.add(SpaceRendererMesh.build(data, entry.getKey()));
        }
        return java.util.Optional.of(List.copyOf(meshes));
    }

    public static SpaceRendererSectionSnapshot sectionSnapshot(SpaceRendererSectionManager.SectionKey key) {
        return SNAPSHOTS.get(key);
    }

    public static int sectionSnapshotCount() { return SNAPSHOTS.size(); }
    public static long sectionSnapshotBytes() { return SNAPSHOTS.currentBytes(); }

    public static boolean publish(SpaceRendererSectionManager.BuildToken token, PrismTerrainVertexData data) {
        if (closed || token == null || data == null || !SECTIONS.isCurrent(token)) return false;
        return SCHEDULER.submit(token, 0.0, () -> SpaceRendererMesh.build(data));
    }

    /** Drain on render thread; revision checks discard obsolete worker results before queueing uploads. */
    public static int drainCompletions() {
        if (closed) return 0;
        var results = SCHEDULER.drainCompletions(MAX_IN_FLIGHT);
        var modelResults = MODEL_SCHEDULER.drainCompletions(MAX_IN_FLIGHT);
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
        for (var result : modelResults) {
            if (!result.succeeded()) {
                PlanetEssentials.LOGGER.warn("Space Renderer baked-model meshing failed for {}",
                        result.token().key(), result.failure());
                continue;
            }
            if (!SECTIONS.isCurrent(result.token()) || result.value().isEmpty()) continue;
            for (SpaceRendererMesh mesh : result.value().orElseThrow()) {
                String queueMaterialKey = mesh.materialId() + "#" + mesh.layer().name()
                        + "#" + mesh.source().stage().name();
                if (UPLOADS.offer(result.token(), queueMaterialKey, mesh, mesh.source().estimatedCpuBytes())) {
                    queued++;
                } else {
                    REJECTED_UPLOADS.incrementAndGet();
                }
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

    /**
     * Captures an immutable view of the current vanilla draw list and resolves
     * each draw to a Space Renderer-owned GPU mesh. This is preparation only:
     * incomplete coverage always leaves vanilla rendering in control.
     */
    public static SpaceRendererFrameSnapshot captureVisibleFrame(ClientLevel level,
            ChunkSectionsToRender rendered, PrismTerrainVanillaDrawIndex.Snapshot drawIndex,
            double cameraX, double cameraY, double cameraZ) {
        RenderSystem.assertOnRenderThread();
        long sequence = FRAME_SEQUENCE.incrementAndGet();
        closeSortedIndexBuffers();
        if (sequence <= 0) throw new IllegalStateException("Space Renderer frame sequence exhausted");
        if (closed || level == null || rendered == null || drawIndex == null) {
            currentFrame = SpaceRendererFrameSnapshot.empty(sequence);
            return currentFrame;
        }

        Map<?, ?> drawsPerLayer = (Map<?, ?>) rendered.drawsPerLayer();
        ArrayList<Object> allDraws = new ArrayList<>();
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            Object value = drawsPerLayer.get(layer);
            if (!(value instanceof List<?> draws)) {
                currentFrame = SpaceRendererFrameSnapshot.empty(sequence);
                return currentFrame;
            }
            allDraws.addAll(draws);
        }
        if (!drawIndex.isCompleteFor(allDraws)) {
            currentFrame = SpaceRendererFrameSnapshot.empty(sequence);
            return currentFrame;
        }

        ArrayList<SpaceRendererFrameSnapshot.DrawInput> inputs = new ArrayList<>(allDraws.size());
        String dimension = dimensionId(level);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            List<?> draws = (List<?>) drawsPerLayer.get(layer);
            PrismFrameStage stage = layer == ChunkSectionLayer.TRANSLUCENT || layer == ChunkSectionLayer.TRIPWIRE
                    ? PrismFrameStage.BEFORE_TRANSLUCENT : PrismFrameStage.BEFORE_ENTITIES;
            for (int index = 0; index < draws.size(); index++) {
                Object draw = draws.get(index);
                PrismTerrainVanillaDrawIndex.DrawDescriptor descriptor = drawIndex.byDraw().get(draw);
                if (descriptor == null || descriptor.layer() != layer || descriptor.layerDrawIndex() != index) {
                    currentFrame = SpaceRendererFrameSnapshot.empty(sequence);
                    return currentFrame;
                }
                var section = new SpaceRendererSectionManager.SectionKey(dimension,
                        descriptor.sectionX(), descriptor.sectionY(), descriptor.sectionZ());
                inputs.add(new SpaceRendererFrameSnapshot.DrawInput(draw, section,
                        descriptor.materialId(), stage, layer.label(), index));
            }
        }
        currentFrame = SpaceRendererFrameSnapshot.capture(sequence, inputs,
                input -> GPU_CACHE.meshFor(input.section(), input.materialId(), input.stage(), layerFromLabel(input.layerId())),
                cameraX, cameraY, cameraZ);
        uploadSortedIndexBuffers(RenderSystem.getDevice());
        return currentFrame;
    }

    /**
     * Uploads camera-sorted index plans as bounded, frame-owned GPU resources.
     * This prepares indexed submission; it does not itself draw terrain.
     */
    private static void uploadSortedIndexBuffers(GpuDevice device) {
        RenderSystem.assertOnRenderThread();
        if (device == null || !currentFrame.isComplete()) return;
        List<SpaceRendererFrameSnapshot.SortedTranslucentDraw> sorted =
                currentFrame.preparedTranslucentDraws();
        if (sorted.isEmpty() || sorted.size() > MAX_SORTED_INDEX_DRAWS_PER_FRAME) return;
        long requiredBytes = 0;
        for (var draw : sorted) {
            requiredBytes += (long) draw.indices().length * Short.BYTES;
            if (requiredBytes > MAX_SORTED_INDEX_BYTES_PER_FRAME) return;
        }
        try {
            for (var draw : sorted) {
                SpaceRendererSortedIndexBuffer uploaded = SpaceRendererSortedIndexBuffer.upload(device, draw);
                SORTED_INDEX_BUFFERS.add(uploaded);
                sortedIndexBytes += uploaded.gpuBytes();
            }
        } catch (RuntimeException | Error failure) {
            closeSortedIndexBuffers();
            PlanetEssentials.LOGGER.warn("Space Renderer sorted translucent index upload failed", failure);
        }
    }

    private static void closeSortedIndexBuffers() {
        RenderSystem.assertOnRenderThread();
        RuntimeException failure = null;
        for (SpaceRendererSortedIndexBuffer buffer : SORTED_INDEX_BUFFERS) {
            try { buffer.close(); }
            catch (RuntimeException closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
        }
        SORTED_INDEX_BUFFERS.clear();
        sortedIndexBytes = 0;
        if (failure != null) throw failure;
    }

    /** Borrowed resources valid only until the next visible-frame capture. */
    public static List<SpaceRendererSortedIndexBuffer> currentSortedIndexBuffers() {
        RenderSystem.assertOnRenderThread();
        return List.copyOf(SORTED_INDEX_BUFFERS);
    }

    public static long currentSortedIndexBytes() {
        RenderSystem.assertOnRenderThread();
        return sortedIndexBytes;
    }

    private static ChunkSectionLayer layerFromLabel(String label) {
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            if (layer.label().equals(label)) return layer;
        }
        throw new IllegalArgumentException("Unknown vanilla chunk layer: " + label);
    }

    public static SpaceRendererFrameSnapshot currentFrameSnapshot() { return currentFrame; }

    public static com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainGpuMesh gpuMesh(
            SpaceRendererSectionManager.SectionKey section, String material,
            PrismFrameStage stage) {
        return GPU_CACHE.meshFor(section, material, stage);
    }
    public static int gpuMeshCount() { RenderSystem.assertOnRenderThread(); return GPU_CACHE.size(); }
    public static long gpuBytes() { RenderSystem.assertOnRenderThread(); return GPU_CACHE.currentBytes(); }
    public static long failedGpuUploadCount() { return FAILED_GPU_UPLOADS.get(); }

    public static int pendingUploadCount() { return UPLOADS.size(); }
    public static long pendingUploadBytes() { return UPLOADS.queuedBytes(); }
    public static long rejectedUploadCount() { return REJECTED_UPLOADS.get(); }
    public static long rejectedMeshBuildCount() { return REJECTED_MESH_BUILDS.get(); }

    public static void unloadChunk(ClientLevel level, int chunkX, int minSectionY, int maxSectionY, int chunkZ) {
        String dimension = dimensionId(level);
        for (int y = minSectionY; y < maxSectionY; y++) {
            var key = new SpaceRendererSectionManager.SectionKey(dimension, chunkX, y, chunkZ);
            SECTIONS.unload(key);
            SNAPSHOTS.remove(key);
            UPLOADS.removeSection(key);
        }
        queueGpu(() -> GPU_CACHE.invalidateChunk(dimension, chunkX, chunkZ));
    }

    public static void clearWorld(ClientLevel level) {
        if (level == null) return;
        String dimension = dimensionId(level);
        SECTIONS.unloadDimension(dimension);
        SNAPSHOTS.removeDimension(dimension);
        UPLOADS.removeDimension(dimension);
        closeSortedIndexBuffers();
        currentFrame = SpaceRendererFrameSnapshot.empty(Math.max(1, FRAME_SEQUENCE.incrementAndGet()));
        queueGpu(() -> GPU_CACHE.clearDimension(dimension));
    }

    public static void clearAll() {
        SECTIONS.clearAll();
        SNAPSHOTS.clear();
        UPLOADS.clear();
        if (RenderSystem.isOnRenderThread()) closeSortedIndexBuffers();
        else queueGpu(SpaceRendererCaptureRuntime::closeSortedIndexBuffers);
        currentFrame = SpaceRendererFrameSnapshot.empty(Math.max(1, FRAME_SEQUENCE.incrementAndGet()));
        queueGpu(GPU_CACHE::clear);
    }

    public static void shutdown() {
        if (closed) return;
        closed = true;
        clearAll();
        SCHEDULER.close();
        MODEL_SCHEDULER.close();
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
