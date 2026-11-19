package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBindings;

import java.util.Objects;
import java.util.Set;

/** Submits an uploaded V1 terrain mesh through a pipeline compiled for the matching vertex format. */
public final class PrismTerrainDrawExecutor {
    private PrismTerrainDrawExecutor() {}

    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            PrismTerrainSectionUniformAllocation sectionUniform) {
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(sectionUniform, "sectionUniform");
        PrismTerrainSectionUniform.requireDeclared(program.bindings()::uniformBuffers);
        MinecraftShaderBindings completeBindings = Objects.requireNonNull(bindings, "bindings")
                .withUniformBuffer(PrismTerrainSectionUniform.BINDING_NAME, sectionUniform.slice());
        return drawValidated(pass, program, program.pipeline(), mesh, completeBindings, Set.of(), null, null, 0);
    }

    /** Draws one section while accounting for captured vanilla default uniforms already bound to the pass. */
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            PrismTerrainSectionUniformAllocation sectionUniform,
            Set<String> externallyBoundUniforms) {
        Objects.requireNonNull(sectionUniform, "sectionUniform");
        Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms");
        PrismTerrainSectionUniform.requireDeclared(program.bindings()::uniformBuffers);
        MinecraftShaderBindings completeBindings = Objects.requireNonNull(bindings, "bindings")
                .withUniformBuffer(PrismTerrainSectionUniform.BINDING_NAME, sectionUniform.slice());
        return drawValidated(pass, program, program.pipeline(), mesh, completeBindings,
                externallyBoundUniforms, null, null, 0);
    }

    /** Draws using explicit bindings plus uniforms already installed by Minecraft. */
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            Set<String> externallyBoundUniforms) {
        return drawValidated(pass, program, program.pipeline(), mesh, bindings,
                Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms"), null, null, 0);
    }

    /** Draws a whole quad mesh into an already-open pass without injecting a section uniform. */
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings) {
        return drawValidated(pass, program, program.pipeline(), mesh, bindings, Set.of(), null, null, 0);
    }

    /**
     * Draws a section using a caller-supplied pipeline clone whose fixed-function
     * state matches the captured vanilla layer while retaining the Prism shader.
     */
    public static boolean drawWithPipeline(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            RenderPipeline drawPipeline,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            PrismTerrainSectionUniformAllocation sectionUniform,
            Set<String> externallyBoundUniforms) {
        Objects.requireNonNull(drawPipeline, "drawPipeline");
        Objects.requireNonNull(sectionUniform, "sectionUniform");
        Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms");
        PrismTerrainSectionUniform.requireDeclared(program.bindings()::uniformBuffers);
        MinecraftShaderBindings completeBindings = Objects.requireNonNull(bindings, "bindings")
                .withUniformBuffer(PrismTerrainSectionUniform.BINDING_NAME, sectionUniform.slice());
        return drawValidated(pass, program, drawPipeline, mesh, completeBindings,
                externallyBoundUniforms, null, null, 0);
    }

    /**
     * Draws with the sequential QUADS index buffer resolved during preflight.
     * This overload avoids allocating or growing the index buffer after the
     * caller has opened its render pass.
     */
    public static boolean drawWithPipeline(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            RenderPipeline drawPipeline,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            PrismTerrainSectionUniformAllocation sectionUniform,
            Set<String> externallyBoundUniforms,
            GpuBuffer preparedIndexBuffer,
            VertexFormat.IndexType preparedIndexType) {
        Objects.requireNonNull(drawPipeline, "drawPipeline");
        Objects.requireNonNull(sectionUniform, "sectionUniform");
        Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms");
        Objects.requireNonNull(preparedIndexBuffer, "preparedIndexBuffer");
        Objects.requireNonNull(preparedIndexType, "preparedIndexType");
        PrismTerrainSectionUniform.requireDeclared(program.bindings()::uniformBuffers);
        MinecraftShaderBindings completeBindings = Objects.requireNonNull(bindings, "bindings")
                .withUniformBuffer(PrismTerrainSectionUniform.BINDING_NAME, sectionUniform.slice());
        return drawValidated(pass, program, drawPipeline, mesh, completeBindings,
                externallyBoundUniforms, preparedIndexBuffer, preparedIndexType, 0);
    }

    /** Draws with a caller-owned SHORT index buffer, used for camera-sorted translucent quads. */
    public static boolean drawSorted(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            Set<String> externallyBoundUniforms,
            GpuBuffer indexBuffer,
            int firstIndex) {
        Objects.requireNonNull(indexBuffer, "indexBuffer");
        if (firstIndex < 0) throw new IllegalArgumentException("First terrain index cannot be negative");
        return drawValidated(pass, program, program.pipeline(), mesh, bindings,
                Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms"),
                indexBuffer, VertexFormat.IndexType.SHORT, firstIndex);
    }

    /**
     * Checks an indexed draw range against a prepared GPU index buffer without
     * mutating RenderPass state. firstIndex and indexCount are measured in
     * indices; GpuBuffer.size() is measured in bytes.
     */
    static boolean indexRangeFits(long bufferSizeBytes,
            VertexFormat.IndexType indexType, int firstIndex, int indexCount) {
        Objects.requireNonNull(indexType, "indexType");
        if (bufferSizeBytes < 0 || firstIndex < 0 || indexCount < 0) return false;
        long endIndex = (long) firstIndex + (long) indexCount;
        if (endIndex > Long.MAX_VALUE / indexType.bytes) return false;
        return endIndex * indexType.bytes <= bufferSizeBytes;
    }

    private static boolean drawValidated(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            RenderPipeline drawPipeline,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            Set<String> externallyBoundUniforms,
            GpuBuffer explicitIndexBuffer,
            VertexFormat.IndexType explicitIndexType,
            int firstIndex) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(drawPipeline, "drawPipeline");
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(bindings, "bindings");
        if (!program.supportsTerrainV1()) {
            throw new IllegalArgumentException("Prism terrain V1 data requires a terrain V1 compiled pipeline");
        }
        if (mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0) {
            throw new IllegalArgumentException("Prism terrain mesh must contain complete quads");
        }
        int indexCount = (mesh.vertexCount() / 4) * 6;
        // Validate the explicit index range before any binding or RenderPass
        // mutation. A rejected prepared buffer must leave the caller's pass
        // untouched so the caller can safely choose its preflight fallback.
        if (explicitIndexBuffer != null
                && !indexRangeFits(explicitIndexBuffer.size(), explicitIndexType, firstIndex, indexCount)) {
            throw new IllegalArgumentException("Prepared terrain index buffer cannot address the requested draw range");
        }
        // Validate/apply bindings only after all CPU-checkable draw inputs pass.
        bindings.apply(pass, program.bindings(), externallyBoundUniforms);
        pass.setPipeline(drawPipeline);
        pass.setVertexBuffer(0, mesh.vertexBuffer());
        if (explicitIndexBuffer == null) {
            var sequentialIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            pass.setIndexBuffer(sequentialIndices.getBuffer(indexCount), sequentialIndices.type());
        } else {
            if (explicitIndexType == null) throw new IllegalArgumentException("Explicit terrain index type is required");
            pass.setIndexBuffer(explicitIndexBuffer, explicitIndexType);
        }
        pass.drawIndexed(0, firstIndex, indexCount, 1);
        return true;
    }
}
