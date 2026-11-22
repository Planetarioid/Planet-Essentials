package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceRendererTerrainPipelinesTest {
    @Test
    void registersIndependentLayerPipelinesWithExpectedVertexContract() {
        assertEquals(4, SpaceRendererTerrainPipelines.all().size());
        for (RenderPipeline pipeline : SpaceRendererTerrainPipelines.all()) {
            assertNotNull(pipeline);
            assertEquals("planet_essentials", pipeline.getLocation().getNamespace());
            assertEquals("planet_essentials", pipeline.getVertexShader().getNamespace());
            assertEquals("planet_essentials", pipeline.getFragmentShader().getNamespace());
            assertEquals(36, pipeline.getVertexFormat().getVertexSize());
            assertEquals(VertexFormat.Mode.QUADS, pipeline.getVertexFormatMode());
            assertTrue(pipeline.getSamplers().contains("Sampler0"));
            assertTrue(pipeline.getSamplers().contains("Sampler2"));
        }
    }

    @Test
    void translucentPipelineBlendsWithoutWritingDepth() {
        assertTrue(SpaceRendererTerrainPipelines.TRANSLUCENT.getBlendFunction().isPresent());
        assertFalse(SpaceRendererTerrainPipelines.TRANSLUCENT.isWriteDepth());
    }

    @Test
    void solidPipelineKeepsDepthWritesEnabled() {
        assertTrue(SpaceRendererTerrainPipelines.SOLID.isWriteDepth());
    }
}
