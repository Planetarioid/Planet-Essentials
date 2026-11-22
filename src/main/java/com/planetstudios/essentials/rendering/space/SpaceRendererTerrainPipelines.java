package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexLayout;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Native Minecraft pipelines for Space Renderer terrain.
 *
 * <p>Registration is deliberately separate from the Prism shader-pack runtime.
 * These pipelines are not used for terrain submission yet; vanilla remains the
 * active terrain renderer until the independent draw executor is complete.</p>
 */
public final class SpaceRendererTerrainPipelines {
    private static final String NAMESPACE = "planet_essentials";
    private static final RenderPipeline.Snippet TERRAIN_SNIPPET = RenderPipeline.builder()
            .withVertexShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/space_terrain"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(NAMESPACE, "core/space_terrain"))
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("SpaceRendererSection", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(PrismTerrainVertexLayout.minecraftVertexFormat(), VertexFormat.Mode.QUADS)
            .buildSnippet();

    public static final RenderPipeline SOLID = register("space_terrain_solid", TERRAIN_SNIPPET);
    public static final RenderPipeline CUTOUT = RenderPipelines.register(RenderPipeline.builder(TERRAIN_SNIPPET)
            .withLocation(location("space_terrain_cutout"))
            .withShaderDefine("ALPHA_CUTOUT", 0.5f)
            .build());
    public static final RenderPipeline TRANSLUCENT = RenderPipelines.register(RenderPipeline.builder(TERRAIN_SNIPPET)
            .withLocation(location("space_terrain_translucent"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withShaderDefine("ALPHA_CUTOUT", 0.01f)
            .withDepthWrite(false)
            .build());
    public static final RenderPipeline TRIPWIRE = RenderPipelines.register(RenderPipeline.builder(TERRAIN_SNIPPET)
            .withLocation(location("space_terrain_tripwire"))
            .withBlend(BlendFunction.TRANSLUCENT)
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .build());

    private SpaceRendererTerrainPipelines() {}

    private static RenderPipeline register(String path, RenderPipeline.Snippet snippet) {
        return RenderPipelines.register(RenderPipeline.builder(snippet).withLocation(location(path)).build());
    }

    private static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "pipeline/" + path);
    }

    /** Forces static pipeline registration during client initialization. */
    public static void initialize() {}

    public static List<RenderPipeline> all() {
        return List.of(SOLID, CUTOUT, TRANSLUCENT, TRIPWIRE);
    }
}
