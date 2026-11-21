package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.space.SpaceRendererCaptureRuntime;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects the revision-safe CPU section lifecycle to Fabric's real client
 * chunk lifecycle. State is isolated by ClientLevel identity so coordinates
 * from different dimensions/world sessions can never share a cache.
 */
public final class PrismTerrainChunkLifecycleEvents {
    private static final Map<ClientLevel, WorldState> WORLDS = new IdentityHashMap<>();
    private static final CopyOnWriteArrayList<GpuInvalidator> GPU_INVALIDATORS = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private PrismTerrainChunkLifecycleEvents() {}

    public static void registerFabricEvents() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> state(level));
        ClientChunkEvents.CHUNK_UNLOAD.register(PrismTerrainChunkLifecycleEvents::unload);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearAllWorlds());
    }

    public static PrismTerrainSectionLifecycle lifecycle(ClientLevel level) {
        return state(Objects.requireNonNull(level, "level")).lifecycle;
    }

    public static PrismTerrainSectionLifecycle.BuildToken beginRebuild(
            ClientLevel level, int sectionX, int sectionY, int sectionZ) {
        return state(Objects.requireNonNull(level, "level")).lifecycle
                .beginRebuild(sectionX, sectionY, sectionZ);
    }

    public static PrismTerrainSectionCache cache(ClientLevel level) {
        return state(Objects.requireNonNull(level, "level")).cache;
    }

    /** Returns a stable point-in-time list of captured vanilla terrain for this world. */
    public static List<PrismTerrainVertexData> snapshot(ClientLevel level) {
        return cache(Objects.requireNonNull(level, "level")).snapshot();
    }

    public static Registration registerGpuInvalidator(GpuInvalidator invalidator) {
        GpuInvalidator value = Objects.requireNonNull(invalidator, "invalidator");
        GPU_INVALIDATORS.add(value);
        return () -> GPU_INVALIDATORS.remove(value);
    }

    public static void clearWorld(ClientLevel level) {
        if (level == null) return;
        WorldState removed;
        synchronized (WORLDS) {
            removed = WORLDS.remove(level);
        }
        if (removed == null) return;
        removed.cache.clear();
        queueGpu(() -> GPU_INVALIDATORS.forEach(invalidator -> safeClear(invalidator, level)));
    }

    private static WorldState state(ClientLevel level) {
        synchronized (WORLDS) {
            return WORLDS.computeIfAbsent(level, ignored -> {
                PrismTerrainSectionCache cache = new PrismTerrainSectionCache();
                return new WorldState(cache, new PrismTerrainSectionLifecycle(cache));
            });
        }
    }

    private static void unload(ClientLevel level, LevelChunk chunk) {
        WorldState world = state(level);
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int minSectionY = level.getMinSectionY();
        int maxSectionY = level.getMaxSectionY();
        try {
            world.lifecycle.unloadChunk(chunkX, minSectionY, maxSectionY, chunkZ);
            SpaceRendererCaptureRuntime.unloadChunk(level, chunkX, minSectionY, maxSectionY, chunkZ);
            PrismTerrainFrameCoordinator.invalidateChunk(chunkX, chunkZ);
            queueGpu(() -> GPU_INVALIDATORS.forEach(invalidator ->
                    safeInvalidate(invalidator, level, chunkX, chunkZ, minSectionY, maxSectionY)));
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.error(
                    "Failed to invalidate Prism terrain data for unloaded chunk {}, {}", chunkX, chunkZ, failure);
        }
    }

    private static void clearAllWorlds() {
        SpaceRendererCaptureRuntime.clearAll();
        PrismTerrainFrameCoordinator.clearPublishedFrame();
        PrismTerrainActiveFrustum.clear();
        Map<ClientLevel, WorldState> removed;
        synchronized (WORLDS) {
            removed = new IdentityHashMap<>(WORLDS);
            WORLDS.clear();
        }
        for (Map.Entry<ClientLevel, WorldState> entry : removed.entrySet()) {
            entry.getValue().cache.clear();
            ClientLevel level = entry.getKey();
            queueGpu(() -> GPU_INVALIDATORS.forEach(invalidator -> safeClear(invalidator, level)));
        }
    }

    private static void queueGpu(Runnable callback) {
        RenderSystem.queueFencedTask(callback::run);
    }

    private static void safeInvalidate(GpuInvalidator invalidator, ClientLevel level,
            int chunkX, int chunkZ, int minSectionY, int maxSectionY) {
        try {
            invalidator.invalidateChunk(level, chunkX, chunkZ, minSectionY, maxSectionY);
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.error("A Prism renderer GPU invalidator failed for chunk {}, {}",
                    chunkX, chunkZ, failure);
        }
    }

    private static void safeClear(GpuInvalidator invalidator, ClientLevel level) {
        try {
            invalidator.clearWorld(level);
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.error("A Prism renderer GPU invalidator failed while clearing a world", failure);
        }
    }

    private record WorldState(PrismTerrainSectionCache cache, PrismTerrainSectionLifecycle lifecycle) {}

    public interface GpuInvalidator {
        void invalidateChunk(ClientLevel level, int chunkX, int chunkZ, int minSectionY, int maxSectionY);
        void clearWorld(ClientLevel level);
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override void close();
    }
}
