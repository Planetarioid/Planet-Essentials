package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBindings;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Fully allocated, immutable per-layer draw resources prepared before a Prism
 * render pass is opened. Owns section uniform allocations, but borrows cached
 * terrain meshes, the compiled program, and the pipeline variant.
 */
public final class PrismTerrainPreparedRenderGroup implements AutoCloseable {
    private final ClientLevel level;
    private final long snapshotSequence;
    private final long shaderGeneration;
    private final ChunkSectionLayer layer;
    private final MinecraftShaderBackend.MinecraftCompiledProgram program;
    private final RenderPipeline pipeline;
    private final PrismTerrainVanillaPassSnapshot.PreparedIndexBuffer indexBuffer;
    private final MinecraftShaderBindings samplerBindings;
    private final Set<String> externallyBoundUniforms;
    private final List<Draw> draws;
    private final PrismTerrainSubmissionState submissionState = new PrismTerrainSubmissionState();
    private boolean closed;

    public PrismTerrainPreparedRenderGroup(ClientLevel level, long snapshotSequence,
            long shaderGeneration, ChunkSectionLayer layer,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            RenderPipeline pipeline, PrismTerrainVanillaPassSnapshot.PreparedIndexBuffer indexBuffer,
            MinecraftShaderBindings samplerBindings, Set<String> externallyBoundUniforms, List<Draw> draws) {
        this.level = Objects.requireNonNull(level, "level");
        if (snapshotSequence <= 0 || shaderGeneration <= 0) {
            throw new IllegalArgumentException("Prepared render-group generations must be positive");
        }
        this.snapshotSequence = snapshotSequence;
        this.shaderGeneration = shaderGeneration;
        this.layer = Objects.requireNonNull(layer, "layer");
        this.program = Objects.requireNonNull(program, "program");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.indexBuffer = Objects.requireNonNull(indexBuffer, "indexBuffer");
        this.samplerBindings = Objects.requireNonNull(samplerBindings, "samplerBindings");
        this.externallyBoundUniforms = Set.copyOf(externallyBoundUniforms);
        this.draws = List.copyOf(draws);
        if (this.draws.isEmpty()) throw new IllegalArgumentException("Prepared render group cannot be empty");
        java.util.ArrayList<Integer> vertexCounts = new java.util.ArrayList<>(this.draws.size());
        for (Draw draw : this.draws) {
            if (draw.descriptor().layer() != layer) {
                throw new IllegalArgumentException("Prepared draw belongs to a different terrain layer");
            }
            vertexCounts.add(draw.mesh().vertexCount());
        }
        int requiredIndexCount = requiredIndexCount(vertexCounts);
        if (indexBuffer.indexCount() < requiredIndexCount
                || !PrismTerrainDrawExecutor.indexRangeFits(indexBuffer.buffer().size(),
                        indexBuffer.type(), 0, requiredIndexCount)) {
            throw new IllegalArgumentException("Prepared sequential index buffer cannot cover every draw in the group");
        }
    }

    static int requiredIndexCount(List<Integer> vertexCounts) {
        Objects.requireNonNull(vertexCounts, "vertexCounts");
        if (vertexCounts.isEmpty()) throw new IllegalArgumentException("Prepared draw group cannot be empty");
        int maximum = 0;
        for (int vertexCount : vertexCounts) {
            if (vertexCount <= 0) throw new IllegalArgumentException("Prepared terrain mesh must contain vertices");
            maximum = Math.max(maximum,
                    PrismTerrainVanillaPassSnapshot.IndexBufferInput.indexCountForQuadVertexCount(vertexCount));
        }
        return maximum;
    }

    /**
     * Submits this prepared group exactly once into a caller-opened render pass.
     * Every predictable resource and index-capacity check must have completed
     * before this method is called. If submission fails, inspect
     * mayFallBackToVanilla(): it is false after any draw was issued.
     *
     * <p>The caller owns the RenderPass and must close it. Closing a pass does
     * not roll back draw commands already submitted by this method.</p>
     */
    public void submitDraws(RenderPass pass) {
        submitDraws(pass, null);
    }

    private void submitDraws(RenderPass pass, GpuBufferSlice[] chunkSectionInfos) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        Objects.requireNonNull(pass, "pass");
        if (!hasLiveResources()) {
            throw new IllegalStateException("Prepared terrain group contains an evicted mesh or closed uniform allocation");
        }
        if (Minecraft.getInstance().level != level || !ShaderPackRuntime.isEnabled()
                || ShaderPackRuntime.pipelineSnapshot().generation() != shaderGeneration) {
            throw new IllegalStateException("Prepared terrain group became stale before submission");
        }

        submissionState.begin();
        try {
            for (Draw draw : draws) {
                pass.setUniform("ChunkSection", draw.chunkSectionUniform());
                boolean submitted = PrismTerrainDrawExecutor.drawPreparedWithPipeline(
                        pass, program, pipeline, draw.mesh(), samplerBindings,
                        draw.sectionUniform(), externallyBoundUniforms,
                        indexBuffer.buffer(), indexBuffer.type(), submissionState);
                if (!submitted) {
                    throw new IllegalStateException("Prism terrain draw executor rejected a prepared draw");
                }
            }
            submissionState.complete();
        } catch (RuntimeException | Error failure) {
            submissionState.fail();
            throw failure;
        }
    }

    /**
     * Opens a pass against the captured vanilla target, rebinds the captured
     * default uniforms, submits this prepared group, and closes the pass.
     *
     * <p>This method deliberately does not cancel or suppress vanilla rendering.
     * If it throws, callers must consult mayFallBackToVanilla(): failures before
     * the first indexed draw permit vanilla fallback; failures after submission
     * begins do not. Closing a pass never rolls back submitted draws.</p>
     */
    public void submitToCapturedPass(
            PrismTerrainRenderGroupBoundary.Snapshot snapshot,
            com.mojang.blaze3d.systems.GpuDevice device,
            String debugLabel) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (!isValidFor(level, snapshot)) {
            throw new IllegalStateException("Prepared terrain group no longer matches the captured render group");
        }
        if (java.util.Arrays.stream(snapshot.group().layers()).noneMatch(candidate -> candidate == layer)) {
            throw new IllegalStateException("Prepared terrain layer is not part of the captured vanilla group");
        }
        // Vanilla opens one pass for the complete layer group. Reopening a pass
        // per layer can reapply attachment clear operations and erase earlier
        // layer submissions. Multi-layer groups require a shared-pass batch.
        if (snapshot.group().layers().length != 1) {
            throw new IllegalStateException(
                    "A multi-layer vanilla terrain group must be submitted through one shared render pass");
        }
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(debugLabel, "debugLabel");
        if (!hasLiveResources()) {
            throw new IllegalStateException("Prepared terrain group contains an evicted mesh or closed uniform allocation");
        }

        try (RenderPass pass = snapshot.passInputs().openCapturedTargetPass(device, debugLabel)) {
            Set<String> boundUniforms = snapshot.passInputs().bindCapturedDefaultUniforms(pass);
            if (!boundUniforms.equals(externallyBoundUniforms)) {
                throw new IllegalStateException("Captured default-uniform bindings changed after terrain preflight");
            }
            submitDraws(pass, snapshot.chunkSectionInfos());
        }
    }

    /** Submits this layer as part of a caller-managed shared vanilla-group pass. */
    void submitDrawsInBatch(RenderPass pass, PrismTerrainSubmissionState batchState) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(batchState, "batchState");
        if (!hasLiveResources()) {
            throw new IllegalStateException("Prepared terrain layer contains an evicted mesh or closed uniform allocation");
        }
        if (Minecraft.getInstance().level != level || !ShaderPackRuntime.isEnabled()
                || ShaderPackRuntime.pipelineSnapshot().generation() != shaderGeneration) {
            throw new IllegalStateException("Prepared terrain layer became stale before batch submission");
        }
        for (Draw draw : draws) {
            pass.setUniform("ChunkSection", draw.chunkSectionUniform());
            boolean submitted = PrismTerrainDrawExecutor.drawPreparedWithPipeline(
                    pass, program, pipeline, draw.mesh(), samplerBindings,
                    draw.sectionUniform(), externallyBoundUniforms,
                    indexBuffer.buffer(), indexBuffer.type(), batchState);
            if (!submitted) {
                throw new IllegalStateException("Prism terrain draw executor rejected a batched draw");
            }
        }
    }

    public boolean mayFallBackToVanilla() {
        return submissionState.mayFallBackToVanilla();
    }

    public PrismTerrainSubmissionState.Phase submissionPhase() {
        return submissionState.phase();
    }

    public int submittedDrawCount() {
        return submissionState.drawSubmissionAttempts();
    }

    public ClientLevel level() { return level; }
    public long snapshotSequence() { return snapshotSequence; }
    public long shaderGeneration() { return shaderGeneration; }
    public ChunkSectionLayer layer() { return layer; }
    public MinecraftShaderBackend.MinecraftCompiledProgram program() { return program; }
    public RenderPipeline pipeline() { return pipeline; }
    public PrismTerrainVanillaPassSnapshot.PreparedIndexBuffer indexBuffer() { return indexBuffer; }
    public MinecraftShaderBindings samplerBindings() { return samplerBindings; }
    public Set<String> externallyBoundUniforms() { return externallyBoundUniforms; }
    public List<Draw> draws() { ensureOpen(); return draws; }
    public boolean isClosed() { return closed; }

    /** Rejects stale world/snapshot/shader resources before a pass is opened. */
    public boolean isValidFor(ClientLevel currentLevel,
            PrismTerrainRenderGroupBoundary.Snapshot snapshot) {
        return !closed && currentLevel == level && Minecraft.getInstance().level == level
                && snapshot != null && snapshot.sequence() == snapshotSequence
                && ShaderPackRuntime.isEnabled()
                && ShaderPackRuntime.pipelineSnapshot().generation() == shaderGeneration;
    }

    /**
     * Checks borrowed GPU meshes and owned uniform allocations immediately before
     * pass creation/submission. Cache eviction or chunk invalidation closes meshes,
     * so a prepared batch must fail before issuing its first draw.
     */
    boolean hasLiveResources() {
        if (closed) return false;
        for (Draw draw : draws) {
            if (!draw.mesh().isOpen() || !draw.sectionUniform().isOpen()) return false;
        }
        return true;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Prepared Prism terrain render group is closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        for (Draw draw : draws) {
            try {
                draw.sectionUniform().close();
            } catch (RuntimeException closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
        }
        if (failure != null) throw failure;
    }

    public record Draw(Object vanillaDraw,
            PrismTerrainVanillaDrawIndex.DrawDescriptor descriptor,
            PrismTerrainGpuMesh mesh,
            PrismTerrainSectionUniformAllocation sectionUniform,
            GpuBufferSlice chunkSectionUniform) {
        public Draw {
            Objects.requireNonNull(vanillaDraw, "vanillaDraw");
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(sectionUniform, "sectionUniform");
            Objects.requireNonNull(chunkSectionUniform, "chunkSectionUniform");
        }
    }
}
