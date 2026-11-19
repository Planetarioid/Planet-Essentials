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
     * Draws a whole quad mesh into an already-open pass.
     * The caller is responsible for selecting the correct world stage and camera-relative transform.
     */
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
        bindings.validate(program.bindings());
        pass.setPipeline(program.pipeline());
        bindings.applyValidated(pass);
        pass.setVertexBuffer(0,mesh.vertexBuffer());
        pass.draw(0,mesh.vertexCount());
        return true;
    }
}
