package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Render-thread coordinator for preparing all-or-nothing vanilla terrain
 * replacement plans. CPU and GPU state is isolated by ClientLevel identity.
 *
 * <p>This class prepares plans only. It deliberately does not intercept
 * vanilla rendering or suppress a vanilla draw group.</p>
 */
public final class PrismTerrainReplacementCoordinator implements AutoCloseable {
    private final Map<ClientLevel, WorldState> worlds = new IdentityHashMap<>();
    private final PrismTerrainChunkLifecycleEvents.Registration invalidatorRegistration;
    private boolean closed;

    public PrismTerrainReplacementCoordinator() {
        invalidatorRegistration = PrismTerrainChunkLifecycleEvents.registerGpuInvalidator(
                new PrismTerrainChunkLifecycleEvents.GpuInvalidator() {
                    @Override
                    public void invalidateChunk(ClientLevel level, int chunkX, int chunkZ,
                            int minSectionY, int maxSectionY) {
                        synchronized (PrismTerrainReplacementCoordinator.this) {
                            WorldState state = worlds.get(level);
                            if (state == null) return;
                            state.gpuCache.invalidateChunk(chunkX, chunkZ);
                        }
                    }

                    @Override
                    public void clearWorld(ClientLevel level) {
                        synchronized (PrismTerrainReplacementCoordinator.this) {
                            WorldState state = worlds.remove(level);
                            if (state != null) state.gpuCache.close();
                        }
                    }
                });
    }

    /**
     * Uploads the exact captured section payloads needed by this draw group,
     * then builds a complete replacement plan. Any missing or stale resource
     * yields an empty fallback plan, leaving vanilla responsible for drawing.
     *
     * Must be called on the render thread at the synchronous vanilla
     * render-group boundary, with the current draw list and matching identity
     * index snapshot.
     */
    public PrismTerrainReplacementPlan.Plan prepare(ClientLevel level,
            List<?> vanillaDraws,
            PrismTerrainVanillaDrawIndex.Snapshot drawIndex,
            ChunkSectionLayer layer,
            PrismTerrainRenderPassState renderState,
            String programId,
            boolean frameReady,
            boolean reverseOrder) {
        Objects.requireNonNull(vanillaDraws, "vanillaDraws");
        Objects.requireNonNull(drawIndex, "drawIndex");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(programId, "programId");
        RenderSystem.assertOnRenderThread();
        ensureOpen();

        int required = vanillaDraws.size();
        if (required == 0) {
            return fallback(0, PrismTerrainReplacementGate.FallbackReason.EMPTY_GROUP);
        }
        if (level == null || Minecraft.getInstance().level != level || !frameReady) {
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.FRAME_NOT_READY);
        }
        if (!drawIndex.isCompleteFor(vanillaDraws)) {
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE);
        }

        GpuDevice device = RenderSystem.getDevice();
        if (device == null) {
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.INCOMPATIBLE_DEVICE);
        }

        MinecraftShaderBackend.MinecraftCompiledProgram program =
                ShaderPackRuntime.terrainProgram(programId);
        if (program == null || !program.supportsTerrainV1()) {
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.PROGRAM_UNAVAILABLE);
        }
        if (renderState == null || !renderState.isCompleteFor(layer,
                Set.copyOf(program.bindings().samplers()))) {
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.RENDER_STATE_NOT_READY);
        }
        long shaderGeneration = ShaderPackRuntime.pipelineSnapshot().generation();
        if (shaderGeneration <= 0 || !ShaderPackRuntime.isEnabled()) {
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.PROGRAM_UNAVAILABLE);
        }

        PrismTerrainSectionCache cpuCache = PrismTerrainChunkLifecycleEvents.cache(level);
        PrismTerrainGpuCache gpuCache = worldState(level).gpuCache;
        try {
            // Prepare each unique section/layer once. Cache misses are uploaded
            // before readiness is evaluated; failed uploads remain fail-open.
            Set<SectionIdentity> prepared = new HashSet<>();
            for (Object draw : vanillaDraws) {
                PrismTerrainVanillaDrawIndex.DrawDescriptor descriptor = drawIndex.byDraw().get(draw);
                if (descriptor == null || descriptor.layer() != layer) {
                    return fallback(required, PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE);
                }
                SectionIdentity key = new SectionIdentity(descriptor.sectionX(),
                        descriptor.sectionY(), descriptor.sectionZ(), descriptor.layer());
                if (!prepared.add(key)) continue;
                String material = PrismTerrainReplacementReadiness.materialFor(layer);
                var stage = PrismTerrainReplacementReadiness.stageFor(layer);
                PrismTerrainVertexData payload = cpuCache.get(descriptor.sectionX(),
                        descriptor.sectionY(), descriptor.sectionZ(), material, stage);
                if (payload == null) continue;
                try {
                    gpuCache.getOrUpload(device, payload);
                } catch (RuntimeException uploadFailure) {
                    PlanetEssentials.LOGGER.warn(
                            "Prism could not prepare terrain GPU data for section {}, {}, {}",
                            descriptor.sectionX(), descriptor.sectionY(), descriptor.sectionZ(), uploadFailure);
                }
            }

            // Shader generation is captured before planning and checked again
            // afterwards, preventing publication against a replaced pipeline.
            PrismTerrainReplacementPlan.Plan plan = PrismTerrainReplacementPlan.build(
                    vanillaDraws, drawIndex, layer,
                    descriptor -> PrismTerrainReplacementReadiness.isReady(descriptor, cpuCache, gpuCache),
                    true, true, frameReady && Minecraft.getInstance().level == level,
                    reverseOrder);
            if (shaderGeneration != ShaderPackRuntime.pipelineSnapshot().generation()
                    || !ShaderPackRuntime.isEnabled()) {
                return fallback(required, PrismTerrainReplacementGate.FallbackReason.PROGRAM_UNAVAILABLE);
            }
            return plan;
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.error("Prism terrain replacement preparation failed for layer {}",
                    layer.label(), failure);
            return fallback(required, PrismTerrainReplacementGate.FallbackReason.FRAME_NOT_READY);
        }
    }

    /**
     * Prepares a replacement plan directly from the live vanilla render-group
     * snapshot. The captured ordered draw list is already in vanilla submission
     * order (including translucent reversal), so this overload never reverses it
     * a second time. It remains preparation-only; vanilla rendering is untouched.
     */
    public PrismTerrainReplacementPlan.Plan prepare(ClientLevel level,
            PrismTerrainRenderGroupBoundary.Snapshot snapshot,
            ChunkSectionLayer layer,
            String programId,
            boolean frameReady) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(layer, "layer");
        PrismTerrainVanillaPassSnapshot.LayerInput layerInput =
                snapshot.passInputs().layers().get(layer);
        if (layerInput == null || !layerInput.pipelineApplied()) {
            return fallback(0, PrismTerrainReplacementGate.FallbackReason.EMPTY_GROUP);
        }
        return prepare(level, layerInput.orderedDraws(),
                PrismTerrainVanillaDrawIndex.snapshot(), layer,
                snapshot.renderStateFor(layer), programId, frameReady, false);
    }

    public synchronized int cachedWorldCount() {
        return worlds.size();
    }

    public synchronized long cachedGpuBytes(ClientLevel level) {
        WorldState state = worlds.get(level);
        if (state == null) return 0;
        return state.gpuCache.currentBytes();
    }

    private synchronized WorldState worldState(ClientLevel level) {
        ensureOpen();
        return worlds.computeIfAbsent(level, ignored -> new WorldState(new PrismTerrainGpuCache()));
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Prism terrain replacement coordinator is closed");
    }

    private static PrismTerrainReplacementPlan.Plan fallback(int required,
            PrismTerrainReplacementGate.FallbackReason reason) {
        return new PrismTerrainReplacementPlan.Plan(List.of(), reason, required, 0);
    }

    @Override
    public void close() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.queueFencedTask(this::close);
            return;
        }
        synchronized (this) {
            if (closed) return;
            closed = true;
            invalidatorRegistration.close();
            for (WorldState state : worlds.values()) state.gpuCache.close();
            worlds.clear();
        }
    }

    private record WorldState(PrismTerrainGpuCache gpuCache) {}
    private record SectionIdentity(int x, int y, int z, ChunkSectionLayer layer) {}
}
