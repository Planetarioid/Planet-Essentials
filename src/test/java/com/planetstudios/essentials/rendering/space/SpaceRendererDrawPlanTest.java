package com.planetstudios.essentials.rendering.space;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererDrawPlanTest {
    @Test void emptyPlanIsNeverReady() {
        var plan = SpaceRendererDrawPlan.empty(7);
        assertFalse(plan.isReady());
        assertEquals(7, plan.frameSequence());
        assertEquals(0, plan.drawCount());
        assertTrue(plan.batches().isEmpty());
    }

    @Test void rejectsNonPositiveFrameSequence() {
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererDrawPlan.empty(0));
    }

    @Test void computesQuadIndexCountWithoutLosingTriangles() {
        assertEquals(6, SpaceRendererDrawPlan.indexCount(4));
        assertEquals(60, SpaceRendererDrawPlan.indexCount(40));
        assertEquals(98304, SpaceRendererDrawPlan.indexCount(65536));
    }

    @Test void rejectsIncompleteQuadVertexStreams() {
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererDrawPlan.indexCount(0));
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererDrawPlan.indexCount(5));
    }

    @Test void onlyTranslucentLayersRequireCameraSortedIndices() {
        assertFalse(SpaceRendererDrawPlan.requiresSortedIndices(ChunkSectionLayer.SOLID));
        assertTrue(SpaceRendererDrawPlan.requiresSortedIndices(ChunkSectionLayer.TRANSLUCENT));
        assertTrue(SpaceRendererDrawPlan.requiresSortedIndices(ChunkSectionLayer.TRIPWIRE));
    }
}
