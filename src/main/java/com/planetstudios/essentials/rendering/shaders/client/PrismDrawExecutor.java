package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;

import java.util.Objects;

/** Executes one validated Prism program draw into an already-open Minecraft render pass. */
public final class PrismDrawExecutor {
    private PrismDrawExecutor() {}

    public static boolean draw(
            RenderPass pass,
            String programId,
            GpuBuffer vertexBuffer,
            int vertexOffset,
            int vertexCount,
            MinecraftShaderBindings bindings,
            ShaderBindingLayout layout) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(programId, "programId");
        Objects.requireNonNull(vertexBuffer, "vertexBuffer");
        Objects.requireNonNull(bindings, "bindings");
        Objects.requireNonNull(layout, "layout");
        if (vertexOffset < 0 || vertexCount <= 0) {
            throw new IllegalArgumentException("Invalid Prism vertex range");
        }
        if (!ShaderPackRuntime.isEnabled()) return false;
        RenderPipeline pipeline = ShaderPackRuntime.renderPipeline(programId);
        if (pipeline == null) return false;

        // Validate bindings before setting pipeline state so invalid packs fail atomically.
        bindings.validate(layout);
        pass.setPipeline(pipeline);
        RenderSystem.bindDefaultUniforms(pass);
        bindings.applyValidated(pass);
        pass.setVertexBuffer(0, vertexBuffer);
        pass.draw(vertexOffset, vertexCount);
        return true;
    }
}
