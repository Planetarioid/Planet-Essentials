package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
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
        return drawValidated(pass, program, mesh, completeBindings, Set.of(), null, null, 0);
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
        return drawValidated(pass, program, mesh, completeBindings,
                externallyBoundUniforms, null, null, 0);
    }

    /** Draws using explicit bindings plus uniforms already installed by Minecraft. */
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            Set<String> externallyBoundUniforms) {
        return drawValidated(pass, program, mesh, bindings,
                Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms"), null, null, 0);
    }

    /** Draws a whole quad mesh into an already-open pass without injecting a section uniform. */
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings) {
        return drawValidated(pass, program, mesh, bindings, Set.of(), null, null, 0);
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
        return drawValidated(pass, program, mesh, bindings,
                Objects.requireNonNull(externallyBoundUniforms, "externallyBoundUniforms"),
                indexBuffer, VertexFormat.IndexType.SHORT, firstIndex);
    }

    private static boolean drawValidated(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings,
            Set<String> externallyBoundUniforms,
            GpuBuffer explicitIndexBuffer,
            VertexFormat.IndexType explicitIndexType,
            int firstIndex) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(bindings, "bindings");
        if (!program.supportsTerrainV1()) {
            throw new IllegalArgumentException("Prism terrain V1 data requires a terrain V1 compiled pipeline");
        }
        if (mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0) {
            throw new IllegalArgumentException("Prism terrain mesh must contain complete quads");
        }
        // Validate the complete binding set before mutating RenderPass state.
        bindings.apply(pass, program.bindings(), externallyBoundUniforms);
        pass.setPipeline(program.pipeline());
        pass.setVertexBuffer(0, mesh.vertexBuffer());
        int indexCount = (mesh.vertexCount() / 4) * 6;
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
