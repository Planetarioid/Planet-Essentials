package com.planetstudios.essentials.rendering.space;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test void preservesVanillaRenderGroupLayerOrder() {
        assertEquals(List.of(ChunkSectionLayer.TRANSLUCENT, ChunkSectionLayer.CUTOUT),
                SpaceRendererDrawPlan.orderedLayers(
                        List.of(ChunkSectionLayer.TRANSLUCENT, ChunkSectionLayer.CUTOUT)));
    }

    @Test void rejectsDuplicateLayersInRenderGroup() {
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererDrawPlan.orderedLayers(
                List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.SOLID)));
    }

    @Test void rejectsNullLayerInRenderGroup() {
        assertThrows(NullPointerException.class, () -> SpaceRendererDrawPlan.orderedLayers(
                java.util.Arrays.asList(ChunkSectionLayer.SOLID, null)));
    }

    @Test void layerViewOfUnreadyPlanRemainsUnready() {
        var view = SpaceRendererDrawPlan.empty(9).forLayers(List.of(ChunkSectionLayer.SOLID));
        assertFalse(view.isReady());
        assertEquals(9, view.frameSequence());
        assertEquals(0, view.drawCount());
        assertTrue(view.batches().isEmpty());
    }

    @Test void groupViewUsesNativeGroupMembershipAndPreservesFrameIdentity() {
        var plan = SpaceRendererDrawPlan.empty(11);
        for (ChunkSectionLayerGroup group : ChunkSectionLayerGroup.values()) {
            var view = plan.forGroup(group);
            assertFalse(view.isReady());
            assertEquals(plan.frameSequence(), view.frameSequence());
            assertEquals(0, view.drawCount());
            assertTrue(view.batches().isEmpty());
        }
    }

    @Test void rejectsNullRenderGroup() {
        assertThrows(NullPointerException.class, () -> SpaceRendererDrawPlan.empty(12).forGroup(null));
    }
}
