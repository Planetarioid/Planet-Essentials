package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVanillaPipelineCacheTest {
    private static RenderPipeline pipeline(String path, VertexFormat format,
            DepthTestFunction depth, PolygonMode polygon, boolean cull,
            boolean writeColor, boolean writeAlpha, boolean writeDepth) {
        return RenderPipeline.builder(new RenderPipeline.Snippet[0])
                .withLocation(Identifier.fromNamespaceAndPath("planet_essentials", path))
                .withVertexShader(Identifier.fromNamespaceAndPath("planet_essentials", path + ".vsh"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("planet_essentials", path + ".fsh"))
                .withSampler("Sampler0")
                .withUniform("PrismSection", UniformType.UNIFORM_BUFFER)
                .withDepthTestFunction(depth)
                .withPolygonMode(polygon)
                .withCull(cull)
                .withoutBlend()
                .withColorWrite(writeColor, writeAlpha)
                .withDepthWrite(writeDepth)
                .withVertexFormat(format, VertexFormat.Mode.QUADS)
                .build();
    }

    @Test
    void clonedPipelineKeepsPrismShadersAndBindingsButCopiesVanillaFixedFunctionState() {
        RenderPipeline shader = pipeline("test/prism", PrismTerrainVertexLayout.minecraftVertexFormat(),
                DepthTestFunction.LEQUAL_DEPTH_TEST, PolygonMode.FILL, true, true, true, true);
        RenderPipeline vanilla = pipeline("test/vanilla", DefaultVertexFormat.BLOCK,
                DepthTestFunction.NO_DEPTH_TEST, PolygonMode.WIREFRAME, false, false, true, false);

        RenderPipeline clone = PrismTerrainVanillaPipelineCache.buildForVanillaState(
                shader, PrismTerrainVanillaPassSnapshot.PipelineState.capture(vanilla));

        assertNotSame(shader, clone);
        assertEquals(shader.getVertexShader(), clone.getVertexShader());
        assertEquals(shader.getFragmentShader(), clone.getFragmentShader());
        assertEquals(shader.getSamplers(), clone.getSamplers());
        assertEquals(shader.getUniforms(), clone.getUniforms());
        assertSame(shader.getVertexFormat(), clone.getVertexFormat());
        assertEquals(shader.getVertexFormatMode(), clone.getVertexFormatMode());

        assertEquals(vanilla.getDepthTestFunction(), clone.getDepthTestFunction());
        assertEquals(vanilla.getPolygonMode(), clone.getPolygonMode());
        assertEquals(vanilla.isCull(), clone.isCull());
        assertEquals(vanilla.getBlendFunction(), clone.getBlendFunction());
        assertEquals(vanilla.isWriteColor(), clone.isWriteColor());
        assertEquals(vanilla.isWriteAlpha(), clone.isWriteAlpha());
        assertEquals(vanilla.isWriteDepth(), clone.isWriteDepth());
        assertEquals(vanilla.getColorLogic(), clone.getColorLogic());
        assertEquals(vanilla.getDepthBiasScaleFactor(), clone.getDepthBiasScaleFactor(), 0.0f);
        assertEquals(vanilla.getDepthBiasConstant(), clone.getDepthBiasConstant(), 0.0f);
    }

    @Test
    void clonedPipelineDoesNotReuseVanillaVertexFormat() {
        RenderPipeline shader = pipeline("test/prism-format", PrismTerrainVertexLayout.minecraftVertexFormat(),
                DepthTestFunction.LEQUAL_DEPTH_TEST, PolygonMode.FILL, true, true, true, true);
        RenderPipeline vanilla = pipeline("test/vanilla-format", DefaultVertexFormat.BLOCK,
                DepthTestFunction.LEQUAL_DEPTH_TEST, PolygonMode.FILL, true, true, true, true);

        RenderPipeline clone = PrismTerrainVanillaPipelineCache.buildForVanillaState(
                shader, PrismTerrainVanillaPassSnapshot.PipelineState.capture(vanilla));

        assertSame(PrismTerrainVertexLayout.minecraftVertexFormat(), clone.getVertexFormat());
        assertNotSame(DefaultVertexFormat.BLOCK, clone.getVertexFormat());
    }
}
