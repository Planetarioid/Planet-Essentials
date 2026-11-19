package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.planetstudios.essentials.rendering.shaders.ShaderBackend;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;
import com.planetstudios.essentials.rendering.shaders.ShaderCapabilities;
import com.planetstudios.essentials.rendering.shaders.ShaderCompileException;
import com.planetstudios.essentials.rendering.shaders.ShaderProgramSpec;
import com.planetstudios.essentials.rendering.shaders.ShaderStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexLayout;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/** Minecraft 1.21.11 GPU pipeline compiler. */
public final class MinecraftShaderBackend implements ShaderBackend {
    @Override public String name() { return RenderSystem.getBackendDescription(); }

    @Override
    public ShaderCapabilities capabilities() {
        RenderSystem.assertOnRenderThread();
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) return ShaderCapabilities.conservative("minecraft-no-device");
        Set<String> features = new HashSet<>();
        features.add("minecraft-render-pipeline");
        features.add("custom-shader-source");
        features.add("position-vertex-format");
        features.add("terrain-v1-vertex-format");
        features.add("uniform-buffers");
        features.add("texel-buffers");
        features.add("samplers");
        if (device.getMaxSupportedAnisotropy() > 1) features.add("anisotropic-sampling");
        return new ShaderCapabilities(name(), 0, 0, 0,
                Set.copyOf(device.getEnabledExtensions()), Set.copyOf(features));
    }

    @Override
    public CompiledProgram compile(ShaderProgramSpec spec) throws ShaderCompileException {
        return compileWithFormat(spec, DefaultVertexFormat.POSITION, false);
    }

    /** Compiles a terrain V1 program whose shader inputs match Prism's full vertex contract. */
    public MinecraftCompiledProgram compileTerrain(ShaderProgramSpec spec) throws ShaderCompileException {
        return compileWithFormat(spec, PrismTerrainVertexLayout.minecraftVertexFormat(), true);
    }

    private MinecraftCompiledProgram compileWithFormat(
            ShaderProgramSpec spec, VertexFormat vertexFormat, boolean terrainV1)
            throws ShaderCompileException {
        RenderSystem.assertOnRenderThread();
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) {
            throw new ShaderCompileException(spec.id(), "Minecraft GPU device is unavailable",
                    "RenderSystem.tryGetDevice() returned null");
        }
        String suffix = spec.contentHash();
        String formatPath = terrainV1 ? "terrain/" : "general/";
        Identifier pipelineId = Identifier.fromNamespaceAndPath(
                "planet_essentials", "prism/generated/" + formatPath + suffix);
        Identifier vertexId = Identifier.fromNamespaceAndPath(
                "planet_essentials", "prism/generated/" + formatPath + suffix + ".vsh");
        Identifier fragmentId = Identifier.fromNamespaceAndPath(
                "planet_essentials", "prism/generated/" + formatPath + suffix + ".fsh");

        RenderPipeline.Builder builder = RenderPipeline.builder(new RenderPipeline.Snippet[0])
                .withLocation(pipelineId)
                .withVertexShader(vertexId)
                .withFragmentShader(fragmentId)
                .withVertexFormat(vertexFormat, VertexFormat.Mode.QUADS);
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
            return new MinecraftCompiledProgram(
                    spec.id(), spec.contentHash(), spec.bindings(), pipeline, compiled, terrainV1);
        } catch (RuntimeException failure) {
            throw new ShaderCompileException(spec.id(), "Minecraft shader pipeline compilation failed",
                    failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage()));
        }
    }

    /** Public read-only handle used by Prism draw executors and renderer adapters. */
    public static final class MinecraftCompiledProgram implements CompiledProgram {
        private final String programId;
        private final String sourceHash;
        private final ShaderBindingLayout bindings;
        private final RenderPipeline pipeline;
        private final boolean terrainV1;
        @SuppressWarnings("unused")
        private final CompiledRenderPipeline compiled;

        private MinecraftCompiledProgram(String programId, String sourceHash,
                ShaderBindingLayout bindings, RenderPipeline pipeline,
                CompiledRenderPipeline compiled, boolean terrainV1) {
            this.programId=programId; this.sourceHash=sourceHash; this.bindings=bindings;
            this.pipeline=pipeline; this.compiled=compiled; this.terrainV1=terrainV1;
        }
        public RenderPipeline pipeline(){return pipeline;}
        public ShaderBindingLayout bindings(){return bindings;}
        public boolean supportsTerrainV1(){return terrainV1;}
        @Override public String programId(){return programId;}
        @Override public String sourceHash(){return sourceHash;}
        @Override public void close(){
            // CompiledRenderPipeline is cached and owned by Minecraft's GpuDevice.
        }
    }
}
