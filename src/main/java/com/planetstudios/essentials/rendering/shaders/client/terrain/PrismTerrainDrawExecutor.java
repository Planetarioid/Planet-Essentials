package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBindings;

import java.util.Objects;

/** Submits an uploaded V1 terrain mesh through a pipeline compiled for the matching vertex format. */
public final class PrismTerrainDrawExecutor {
    private PrismTerrainDrawExecutor() {}

    /**
     * Draws a whole quad mesh and supplies its per-section origin uniform.
     * The allocation must remain open until the submitted GPU work no longer
     * references it. The renderer owns that lifetime.
     */
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
        return drawValidated(pass, program, mesh, completeBindings);
    }

    /**
     * Draws a whole quad mesh into an already-open pass without injecting
     * a section uniform. Kept for non-section terrain pipelines.
     */
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings) {
        return drawValidated(pass, program, mesh, bindings);
    }

    private static boolean drawValidated(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings) {
    public static boolean draw(RenderPass pass,
            MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainGpuMesh mesh,
            MinecraftShaderBindings bindings) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass,"pass");
        Objects.requireNonNull(program,"program");
        Objects.requireNonNull(mesh,"mesh");
        Objects.requireNonNull(bindings,"bindings");
        if(!program.supportsTerrainV1()) {
            throw new IllegalArgumentException("Prism terrain V1 data requires a terrain V1 compiled pipeline");
        }
        if(mesh.vertexCount()<=0 || mesh.vertexCount()%4!=0) {
            throw new IllegalArgumentException("Prism terrain mesh must contain complete quads");
        }
        // Validate the complete binding set before mutating RenderPass state.
        bindings.apply(pass, program.bindings());
        pass.setPipeline(program.pipeline());
        pass.setVertexBuffer(0,mesh.vertexBuffer());
        pass.draw(0,mesh.vertexCount());
        return true;
    }
}
