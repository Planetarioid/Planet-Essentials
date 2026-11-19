package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
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
            PrismTerrainSectionUniformAllocation sectionUniform) {
        public Draw {
            Objects.requireNonNull(vanillaDraw, "vanillaDraw");
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(sectionUniform, "sectionUniform");
        }
    }
}
