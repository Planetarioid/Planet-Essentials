package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingValidator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, non-owning resource bindings for one Prism draw.
 *
 * GPU resources remain owned by their creators. A binding set only references them
 * for the duration of a render pass and never closes or mutates the resources.
 */
public final class MinecraftShaderBindings {
    public record SampledTexture(GpuTextureView view, GpuSampler sampler) {
        public SampledTexture {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(sampler, "sampler");
        }
    }

    private final Map<String, GpuBufferSlice> uniformBuffers;
    private final Map<String, GpuBuffer> texelBuffers;
    private final Map<String, SampledTexture> samplers;

    private MinecraftShaderBindings(Builder builder) {
        uniformBuffers = Map.copyOf(builder.uniformBuffers);
        texelBuffers = Map.copyOf(builder.texelBuffers);
        samplers = Map.copyOf(builder.samplers);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Validate all declared bindings before mutating the active render pass. */
    public void apply(RenderPass pass, ShaderBindingLayout layout) {
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(layout, "layout");
        ShaderBindingValidator.validate(layout,
                uniformBuffers.keySet(), texelBuffers.keySet(), samplers.keySet());

        uniformBuffers.forEach(pass::setUniform);
        // Minecraft's TEXEL_BUFFER uniform type is backed by a GpuBuffer, not a sampled texture.
        texelBuffers.forEach(pass::setUniform);
        samplers.forEach((name, binding) ->
                pass.bindTexture(name, binding.view(), binding.sampler()));
    }

    public static final class Builder {
        private final Map<String, GpuBufferSlice> uniformBuffers = new LinkedHashMap<>();
        private final Map<String, GpuBuffer> texelBuffers = new LinkedHashMap<>();
        private final Map<String, SampledTexture> samplers = new LinkedHashMap<>();

        private Builder() {}

        public Builder uniformBuffer(String name, GpuBufferSlice slice) {
            putUnique(uniformBuffers, name, slice, "uniform buffer");
            return this;
        }

        public Builder texelBuffer(String name, GpuBuffer buffer) {
            putUnique(texelBuffers, name, buffer, "texel buffer");
            return this;
        }

        public Builder sampler(String name, GpuTextureView view, GpuSampler sampler) {
            putUnique(samplers, name, new SampledTexture(view, sampler), "sampler");
            return this;
        }

        public MinecraftShaderBindings build() {
            return new MinecraftShaderBindings(this);
        }

        private static <T> void putUnique(Map<String, T> target, String name, T value, String kind) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, kind);
            if (target.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Duplicate " + kind + " binding: " + name);
            }
        }
    }
}
