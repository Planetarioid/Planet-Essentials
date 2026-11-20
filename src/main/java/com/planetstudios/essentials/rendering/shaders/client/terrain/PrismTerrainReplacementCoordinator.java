package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.ArrayList;
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

    /**
     * Completes the sequential-index portion of render-group preflight for a
     * previously prepared all-or-nothing plan. The returned buffer/type pair
     * must be retained and passed to every draw in that group. An empty result
     * means the caller must leave vanilla rendering in control.
     */
    public java.util.Optional<PrismTerrainVanillaPassSnapshot.PreparedIndexBuffer>
            prepareSequentialIndexBuffer(ClientLevel level,
                    PrismTerrainRenderGroupBoundary.Snapshot snapshot,
                    PrismTerrainReplacementPlan.Plan plan,
                    ChunkSectionLayer layer) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(layer, "layer");
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (!plan.mayReplace() || level == null || Minecraft.getInstance().level != level) {
            return java.util.Optional.empty();
        }
        PrismTerrainSectionCache cpuCache = PrismTerrainChunkLifecycleEvents.cache(level);
        PrismTerrainGpuCache gpuCache = worldState(level).gpuCache;
        int maximumIndexCount = 0;
        for (PrismTerrainReplacementPlan.Entry entry : plan.entries()) {
            var descriptor = entry.descriptor();
            if (descriptor.layer() != layer
                    || !PrismTerrainReplacementReadiness.isReady(descriptor, cpuCache, gpuCache)) {
                return java.util.Optional.empty();
            }
            String material = PrismTerrainReplacementReadiness.materialFor(layer);
            var stage = PrismTerrainReplacementReadiness.stageFor(layer);
            PrismTerrainGpuMesh mesh = gpuCache.get(descriptor.sectionX(), descriptor.sectionY(),
                    descriptor.sectionZ(), material, stage);
            if (mesh == null) return java.util.Optional.empty();
            int drawIndexCount = PrismTerrainVanillaPassSnapshot.IndexBufferInput
                    .indexCountForQuadVertexCount(mesh.vertexCount());
            maximumIndexCount = Math.max(maximumIndexCount, drawIndexCount);
        }
        try {
            return snapshot.passInputs().indexBuffer().resolveBufferForIndexCount(maximumIndexCount);
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.warn(
                    "Prism could not resolve sequential index capacity for terrain layer {}; vanilla remains in control",
                    layer.label(), failure);
            return java.util.Optional.empty();
        }
    }

    /**
     * Resolves one shared sequential-index allocation for every layer plan in
     * a captured vanilla render group. Call this once before preparing any
     * layer's draw resources, then pass the returned object to each layer.
     * This prevents a later layer from growing AutoStorageIndexBuffer and
     * closing the allocation already captured by an earlier layer.
     */
    public java.util.Optional<PrismTerrainVanillaPassSnapshot.PreparedIndexBuffer>
            prepareSequentialIndexBuffer(ClientLevel level,
                    PrismTerrainRenderGroupBoundary.Snapshot snapshot,
                    List<PrismTerrainReplacementPlan.Plan> plans) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(plans, "plans");
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (plans.isEmpty() || level == null || Minecraft.getInstance().level != level
                || plans.stream().anyMatch(plan -> plan == null || !plan.mayReplace())) {
            return java.util.Optional.empty();
        }
        PrismTerrainSectionCache cpuCache = PrismTerrainChunkLifecycleEvents.cache(level);
        PrismTerrainGpuCache gpuCache = worldState(level).gpuCache;
        int maximumIndexCount = 0;
        for (PrismTerrainReplacementPlan.Plan plan : plans) {
            for (PrismTerrainReplacementPlan.Entry entry : plan.entries()) {
                var descriptor = entry.descriptor();
                if (!PrismTerrainReplacementReadiness.isReady(descriptor, cpuCache, gpuCache)) {
                    return java.util.Optional.empty();
                }
                String material = PrismTerrainReplacementReadiness.materialFor(descriptor.layer());
                var stage = PrismTerrainReplacementReadiness.stageFor(descriptor.layer());
                PrismTerrainGpuMesh mesh = gpuCache.get(descriptor.sectionX(), descriptor.sectionY(),
                        descriptor.sectionZ(), material, stage);
                if (mesh == null || mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0) {
                    return java.util.Optional.empty();
                }
                maximumIndexCount = Math.max(maximumIndexCount,
                        PrismTerrainVanillaPassSnapshot.IndexBufferInput
                                .indexCountForQuadVertexCount(mesh.vertexCount()));
            }
        }
        try {
            return snapshot.passInputs().indexBuffer().resolveBufferForIndexCount(maximumIndexCount);
        } catch (RuntimeException failure) {
            PlanetEssentials.LOGGER.warn(
                    "Prism could not resolve shared sequential index capacity for the complete terrain group; vanilla remains in control",
                    failure);
            return java.util.Optional.empty();
        }
    }

    /**
     * Prepares a complete captured render-group batch in vanilla layer order.
     * All plans must be supplied before the shared sequential index buffer is
     * resolved, ensuring no later layer can invalidate an earlier allocation.
     * Any failed layer closes all already-prepared groups and returns empty.
     */
    public java.util.Optional<PrismTerrainPreparedRenderBatch> prepareDrawBatch(
            ClientLevel level,
            PrismTerrainRenderGroupBoundary.Snapshot snapshot,
            Map<ChunkSectionLayer, PrismTerrainReplacementPlan.Plan> plans,
            Map<ChunkSectionLayer, String> programIds) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(plans, "plans");
        Objects.requireNonNull(programIds, "programIds");
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (level == null || Minecraft.getInstance().level != level) {
            return java.util.Optional.empty();
        }

        ArrayList<ChunkSectionLayer> expectedLayers = new ArrayList<>();
        ArrayList<PrismTerrainReplacementPlan.Plan> orderedPlans = new ArrayList<>();
        for (ChunkSectionLayer layer : snapshot.group().layers()) {
            var input = snapshot.passInputs().layers().get(layer);
            if (input == null || !input.pipelineApplied() || input.orderedDraws().isEmpty()) continue;
            var plan = plans.get(layer);
            if (plan == null || !plan.mayReplace() || !programIds.containsKey(layer)
                    || programIds.get(layer) == null || programIds.get(layer).isBlank()) {
                return java.util.Optional.empty();
            }
            expectedLayers.add(layer);
            orderedPlans.add(plan);
        }
        if (expectedLayers.isEmpty() || plans.size() != expectedLayers.size()) {
            return java.util.Optional.empty();
        }

        var sharedIndexBuffer = prepareSequentialIndexBuffer(level, snapshot, orderedPlans);
        if (sharedIndexBuffer.isEmpty()) return java.util.Optional.empty();

        ArrayList<PrismTerrainPreparedRenderGroup> preparedLayers =
                new ArrayList<>(expectedLayers.size());
        try {
            for (int i = 0; i < expectedLayers.size(); i++) {
                ChunkSectionLayer layer = expectedLayers.get(i);
                var prepared = prepareDrawResources(level, snapshot, orderedPlans.get(i),
                        layer, programIds.get(layer), sharedIndexBuffer.get());
                if (prepared.isEmpty()) {
                    throw new IllegalStateException("Terrain layer preflight failed for " + layer.label());
                }
                preparedLayers.add(prepared.get());
            }
            return java.util.Optional.of(new PrismTerrainPreparedRenderBatch(level, snapshot, preparedLayers));
        } catch (RuntimeException | Error failure) {
            for (PrismTerrainPreparedRenderGroup prepared : preparedLayers) {
                try {
                    prepared.close();
                } catch (RuntimeException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            PlanetEssentials.LOGGER.warn(
                    "Prism could not prepare the complete terrain render-group batch; vanilla remains in control",
                    failure);
            return java.util.Optional.empty();
        }
    }

    /**
     * Resolves every draw-time resource for one replacement layer before a
     * Prism RenderPass is opened: state-matched pipeline variant, captured
     * samplers/default-uniform coverage, current GPU meshes, and immutable
     * camera-relative section uniform allocations. Any failure rolls back the
     * allocations made here and returns empty so vanilla remains in control.
     */
    public java.util.Optional<PrismTerrainPreparedRenderGroup> prepareDrawResources(
            ClientLevel level,
            PrismTerrainRenderGroupBoundary.Snapshot snapshot,
            PrismTerrainReplacementPlan.Plan plan,
            ChunkSectionLayer layer,
            String programId) {
        var sharedIndexBuffer = prepareSequentialIndexBuffer(level, snapshot, plan, layer);
        if (sharedIndexBuffer.isEmpty()) return java.util.Optional.empty();
        return prepareDrawResources(level, snapshot, plan, layer, programId, sharedIndexBuffer.get());
    }

    /**
     * Prepares one layer using an index allocation resolved for the complete
     * multi-layer group. The supplied allocation is never grown or replaced
     * here; insufficient capacity fails closed before publication.
     */
    public java.util.Optional<PrismTerrainPreparedRenderGroup> prepareDrawResources(
            ClientLevel level,
            PrismTerrainRenderGroupBoundary.Snapshot snapshot,
            PrismTerrainReplacementPlan.Plan plan,
            ChunkSectionLayer layer,
            String programId,
            PrismTerrainVanillaPassSnapshot.PreparedIndexBuffer sharedIndexBuffer) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(programId, "programId");
        Objects.requireNonNull(sharedIndexBuffer, "sharedIndexBuffer");
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (!plan.mayReplace() || level == null || Minecraft.getInstance().level != level) {
            return java.util.Optional.empty();
        }

        java.util.ArrayList<PrismTerrainSectionUniformAllocation> allocated = new java.util.ArrayList<>();
        try {
            var program = ShaderPackRuntime.terrainProgram(programId);
            if (program == null || !program.supportsTerrainV1()) return java.util.Optional.empty();
            PrismTerrainSectionUniform.requireDeclared(program.bindings()::uniformBuffers);

            long shaderGeneration = ShaderPackRuntime.pipelineSnapshot().generation();
            if (shaderGeneration <= 0 || !ShaderPackRuntime.isEnabled()) return java.util.Optional.empty();

            PrismTerrainVanillaPassSnapshot.LayerInput layerInput =
                    snapshot.passInputs().layers().get(layer);
            if (layerInput == null || !layerInput.pipelineApplied()) return java.util.Optional.empty();

            Set<String> requiredUniforms = new HashSet<>(program.bindings().uniformBuffers());
            requiredUniforms.remove(PrismTerrainSectionUniform.BINDING_NAME);
            Set<String> capturedUniforms = snapshot.passInputs().defaultUniforms().availableNames();
            if (!capturedUniforms.containsAll(requiredUniforms)) {
                PlanetEssentials.LOGGER.warn(
                        "Prism terrain program {} requires uniforms not captured from vanilla: {}",
                        programId, difference(requiredUniforms, capturedUniforms));
                return java.util.Optional.empty();
            }

            Set<String> requiredSamplers = Set.copyOf(program.bindings().samplers());
            var samplerBindings = snapshot.passInputs().samplerBindingsFor(layer, requiredSamplers);
            var state = snapshot.renderStateFor(layer);
            if (!state.isCompleteFor(layer, requiredSamplers)) return java.util.Optional.empty();

            // Pipeline compilation is deliberately completed before any pass opens.
            var pipeline = PrismTerrainVanillaPipelineCache.resolve(program, layerInput);
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            if (camera == null || camera.position() == null) return java.util.Optional.empty();
            var cameraPosition = camera.position();

            PrismTerrainSectionCache cpuCache = PrismTerrainChunkLifecycleEvents.cache(level);
            PrismTerrainGpuCache gpuCache = worldState(level).gpuCache;
            java.util.ArrayList<PrismTerrainPreparedRenderGroup.Draw> draws =
                    new java.util.ArrayList<>(plan.entries().size());
            int maximumIndexCount = 0;

            for (PrismTerrainReplacementPlan.Entry entry : plan.entries()) {
                var descriptor = entry.descriptor();
                if (descriptor.layer() != layer
                        || !PrismTerrainReplacementReadiness.isReady(descriptor, cpuCache, gpuCache)) {
                    throw new IllegalStateException("Terrain section became unavailable during draw-resource preflight");
                }
                String material = PrismTerrainReplacementReadiness.materialFor(layer);
                var stage = PrismTerrainReplacementReadiness.stageFor(layer);
                PrismTerrainGpuMesh mesh = gpuCache.get(descriptor.sectionX(), descriptor.sectionY(),
                        descriptor.sectionZ(), material, stage);
                if (mesh == null || mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0) {
                    throw new IllegalStateException("Terrain GPU mesh is missing or has an invalid quad layout");
                }
                maximumIndexCount = Math.max(maximumIndexCount,
                        PrismTerrainVanillaPassSnapshot.IndexBufferInput.indexCountForQuadVertexCount(
                                mesh.vertexCount()));
                var transform = PrismTerrainCameraTransform.forSection(
                        descriptor.sectionX(), descriptor.sectionY(), descriptor.sectionZ(),
                        cameraPosition.x, cameraPosition.y, cameraPosition.z);
                var uniform = PrismTerrainSectionUniformAllocation.upload(RenderSystem.getDevice(), transform);
                allocated.add(uniform);
                draws.add(new PrismTerrainPreparedRenderGroup.Draw(
                        entry.vanillaDraw(), descriptor, mesh, uniform));
            }

            if (draws.size() != plan.requiredDraws()
                    || snapshot.sequence() <= 0
                    || shaderGeneration != ShaderPackRuntime.pipelineSnapshot().generation()
                    || !ShaderPackRuntime.isEnabled()
                    || Minecraft.getInstance().level != level) {
                throw new IllegalStateException("Terrain draw-resource preflight became stale before publication");
            }

            if (sharedIndexBuffer.indexCount() < maximumIndexCount
                    || !PrismTerrainDrawExecutor.indexRangeFits(sharedIndexBuffer.buffer().size(),
                            sharedIndexBuffer.type(), 0, maximumIndexCount)) {
                throw new IllegalStateException("Shared sequential index allocation cannot cover this terrain layer");
            }
            PrismTerrainPreparedRenderGroup prepared = new PrismTerrainPreparedRenderGroup(
                    level, snapshot.sequence(), shaderGeneration, layer, program, pipeline,
                    sharedIndexBuffer, samplerBindings, capturedUniforms, draws);
            allocated.clear(); // Ownership transferred to the prepared group.
            return java.util.Optional.of(prepared);
        } catch (RuntimeException | Error failure) {
            for (PrismTerrainSectionUniformAllocation uniform : allocated) {
                try { uniform.close(); } catch (RuntimeException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            PlanetEssentials.LOGGER.warn(
                    "Prism could not finish draw-resource preflight for terrain layer {}; vanilla remains in control",
                    layer.label(), failure);
            return java.util.Optional.empty();
        }
    }

    private static Set<String> difference(Set<String> required, Set<String> available) {
        Set<String> missing = new HashSet<>(required);
        missing.removeAll(available);
        return Set.copyOf(missing);
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
