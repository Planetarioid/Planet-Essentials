package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBackend;
import net.minecraft.resources.Identifier;
import com.planetstudios.essentials.rendering.shaders.ShaderCompileException;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds Prism-shader pipelines with the captured vanilla layer's fixed-function
 * state while preserving the Prism program's shader sources, bindings, and V1
 * vertex format. Cache keys use object identity so reloads cannot reuse stale
 * shader pipelines.
 */
public final class PrismTerrainVanillaPipelineCache {
    private static final AtomicLong IDS = new AtomicLong();
    private static final Map<RenderPipeline, IdentityHashMap<RenderPipeline, RenderPipeline>> CACHE =
            new WeakHashMap<>();

    private PrismTerrainVanillaPipelineCache() {}

    public static RenderPipeline resolve(MinecraftShaderBackend.MinecraftCompiledProgram program,
            PrismTerrainVanillaPassSnapshot.LayerInput vanillaLayer) throws ShaderCompileException {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(vanillaLayer, "vanillaLayer");
        if (!program.supportsTerrainV1()) throw new IllegalArgumentException("A terrain V1 program is required");
        RenderPipeline prismShaderPipeline = program.pipeline();
        if (!vanillaLayer.pipelineApplied() || vanillaLayer.state() == null) {
            throw new IllegalArgumentException("Cannot clone state for a vanilla layer with no draws");
        }

        synchronized (CACHE) {
            IdentityHashMap<RenderPipeline, RenderPipeline> byVanilla =
                    CACHE.computeIfAbsent(prismShaderPipeline, ignored -> new IdentityHashMap<>());
            RenderPipeline cached = byVanilla.get(vanillaLayer.pipeline());
            if (cached != null) return cached;

            RenderPipeline created = buildForVanillaState(prismShaderPipeline, vanillaLayer.state());
            MinecraftShaderBackend.precompileVariant(program, created);
            byVanilla.put(vanillaLayer.pipeline(), created);
            return created;
        }
    }

    static RenderPipeline buildForVanillaState(RenderPipeline shader, PrismTerrainVanillaPassSnapshot.PipelineState state) {
        long id = IDS.incrementAndGet();
        Identifier location = Identifier.fromNamespaceAndPath(
                "planet_essentials", "prism/terrain/vanilla_state/" + id);

        RenderPipeline.Snippet shaderSnippet = new RenderPipeline.Snippet(
                Optional.of(shader.getVertexShader()),
                Optional.of(shader.getFragmentShader()),
                Optional.of(shader.getShaderDefines()),
                Optional.of(shader.getSamplers()),
                Optional.of(shader.getUniforms()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        RenderPipeline.Builder builder = RenderPipeline.builder(shaderSnippet)
                .withLocation(location)
                .withDepthTestFunction(state.depthTest())
                .withPolygonMode(state.polygonMode())
                .withCull(state.cull())
                .withColorWrite(state.writeColor(), state.writeAlpha())
                .withDepthWrite(state.writeDepth())
                .withColorLogic(state.colorLogic())
                .withDepthBias(state.depthBiasScale(), state.depthBiasConstant())
                // The Prism shader consumes the V1 format, not vanilla's BLOCK format.
                .withVertexFormat(shader.getVertexFormat(), shader.getVertexFormatMode());

        if (state.blend().isPresent()) builder.withBlend(state.blend().get());
        else builder.withoutBlend();
        return builder.build();
    }

    /** Clears cached clones during orderly client shutdown. */
    public static void clear() {
        RenderSystem.assertOnRenderThread();
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
