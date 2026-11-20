package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainPreparedRenderBatchTest {
    @Test
    void acceptsExactVanillaLayerCoverageAndOrder() {
        assertDoesNotThrow(() -> PrismTerrainPreparedRenderBatch.validateLayerOrder(
                List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT),
                List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT)));
    }

    @Test
    void rejectsMissingExtraDuplicateAndReorderedLayers() {
        List<ChunkSectionLayer> expected = List.of(
                ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT, ChunkSectionLayer.TRANSLUCENT);
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderBatch.validateLayerOrder(expected,
                        List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT)));
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderBatch.validateLayerOrder(expected,
                        List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT,
                                ChunkSectionLayer.TRANSLUCENT, ChunkSectionLayer.TRANSLUCENT)));
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderBatch.validateLayerOrder(expected,
                        List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.SOLID,
                                ChunkSectionLayer.TRANSLUCENT)));
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderBatch.validateLayerOrder(expected,
                        List.of(ChunkSectionLayer.TRANSLUCENT, ChunkSectionLayer.CUTOUT,
                                ChunkSectionLayer.SOLID)));
    }

    @Test
    void drawValidationRequiresExactObjectIdentityAndOrder() {
        Object first = new Object();
        Object second = new Object();
        assertTrue(PrismTerrainPreparedRenderBatch.hasExactDrawIdentityOrder(
                List.of(first, second), List.of(first, second)));
        assertFalse(PrismTerrainPreparedRenderBatch.hasExactDrawIdentityOrder(
                List.of(first, second), List.of(second, first)));
        assertFalse(PrismTerrainPreparedRenderBatch.hasExactDrawIdentityOrder(
                List.of(first, second), List.of(first, new Object())));
        assertFalse(PrismTerrainPreparedRenderBatch.hasExactDrawIdentityOrder(
                List.of(first, second), List.of(first)));
    }
}
