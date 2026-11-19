package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.planetstudios.essentials.rendering.shaders.ShaderBackend;
import com.planetstudios.essentials.rendering.shaders.ShaderCapabilities;
import com.planetstudios.essentials.rendering.shaders.ShaderCompileException;
import com.planetstudios.essentials.rendering.shaders.ShaderProgramSpec;
import com.planetstudios.essentials.rendering.shaders.ShaderStage;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/** Minecraft 1.21.11 GPU pipeline compiler. */
public final class MinecraftShaderBackend implements ShaderBackend {
    @Override
    public String name() {
        return RenderSystem.getBackendDescription();
    }

    @Override
    public ShaderCapabilities capabilities() {
        RenderSystem.assertOnRenderThread();
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) return ShaderCapabilities.conservative("minecraft-no-device");
        Set<String> features = new HashSet<>();
        features.add("minecraft-render-pipeline");
        features.add("custom-shader-source");
        features.add("position-vertex-format");
        features.add("uniform-buffers");
        features.add("texel-buffers");
        features.add("samplers");
        if (device.getMaxSupportedAnisotropy() > 1) features.add("anisotropic-sampling");
        return new ShaderCapabilities(name(), 0, 0, 0,
                Set.copyOf(device.getEnabledExtensions()), Set.copyOf(features));
    }

    @Override
    public CompiledProgram compile(ShaderProgramSpec spec) throws ShaderCompileException {
        RenderSystem.assertOnRenderThread();
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) {
            throw new ShaderCompileException(spec.id(), "Minecraft GPU device is unavailable",
                    "RenderSystem.tryGetDevice() returned null");
        }
        String suffix = spec.contentHash();
        Identifier pipelineId = Identifier.fromNamespaceAndPath("planet_essentials", "prism/generated/" + suffix);
        Identifier vertexId = Identifier.fromNamespaceAndPath(
                "planet_essentials", "prism/generated/" + suffix + ".vsh");
        Identifier fragmentId = Identifier.fromNamespaceAndPath(
                "planet_essentials", "prism/generated/" + suffix + ".fsh");

        RenderPipeline.Builder builder = RenderPipeline.builder(new RenderPipeline.Snippet[0])
                .withLocation(pipelineId)
                .withVertexShader(vertexId)
                .withFragmentShader(fragmentId)
                .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);
        spec.bindings().uniformBuffers().forEach(name -> builder.withUniform(name, UniformType.UNIFORM_BUFFER));
        spec.bindings().texelBuffers().forEach(name -> builder.withUniform(name, UniformType.TEXEL_BUFFER));
        spec.bindings().samplers().forEach(builder::withSampler);
        RenderPipeline pipeline = builder.build();

        try {
            CompiledRenderPipeline compiled = device.precompilePipeline(pipeline, (id, type) -> {
                if (type == ShaderType.VERTEX && vertexId.equals(id)) return spec.source(ShaderStage.VERTEX);
                if (type == ShaderType.FRAGMENT && fragmentId.equals(id)) return spec.source(ShaderStage.FRAGMENT);
                return null;
            });
            if (compiled == null) throw new IllegalStateException("Minecraft returned no compiled render pipeline");
            return new MinecraftCompiledProgram(spec.id(), spec.contentHash(), pipeline, compiled);
        } catch (RuntimeException failure) {
            throw new ShaderCompileException(spec.id(), "Minecraft shader pipeline compilation failed",
                    failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage()));
        }
    }

    private static final class MinecraftCompiledProgram implements CompiledProgram {
        private final String programId;
        private final String sourceHash;
        @SuppressWarnings("unused")
        private final RenderPipeline pipeline;
        @SuppressWarnings("unused")
        private final CompiledRenderPipeline compiled;

        private MinecraftCompiledProgram(String programId, String sourceHash,
                RenderPipeline pipeline, CompiledRenderPipeline compiled) {
            this.programId = programId;
            this.sourceHash = sourceHash;
            this.pipeline = pipeline;
            this.compiled = compiled;
        }

        @Override public String programId() { return programId; }
        @Override public String sourceHash() { return sourceHash; }

        @Override
        public void close() {
            // CompiledRenderPipeline is cached and owned by Minecraft's GpuDevice.
            // Clearing that cache here would invalidate unrelated vanilla/mod pipelines.
        }
    }
}
