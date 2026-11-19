package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainDrawExecutorTest {
    @Test
    void preparedIndexRangeUsesIndexTypeByteWidth() {
        assertTrue(PrismTerrainDrawExecutor.indexRangeFits(24,
                VertexFormat.IndexType.INT, 0, 6));
        assertFalse(PrismTerrainDrawExecutor.indexRangeFits(23,
                VertexFormat.IndexType.INT, 0, 6));
        assertTrue(PrismTerrainDrawExecutor.indexRangeFits(12,
                VertexFormat.IndexType.SHORT, 0, 6));
        assertFalse(PrismTerrainDrawExecutor.indexRangeFits(11,
                VertexFormat.IndexType.SHORT, 0, 6));
    }

    @Test
    void preparedIndexRangeAccountsForFirstIndexOffset() {
        assertTrue(PrismTerrainDrawExecutor.indexRangeFits(32,
                VertexFormat.IndexType.INT, 2, 6));
        assertFalse(PrismTerrainDrawExecutor.indexRangeFits(31,
                VertexFormat.IndexType.INT, 2, 6));
    }

    @Test
    void invalidAndOutOfCapacityIndexRangesAreRejected() {
        assertFalse(PrismTerrainDrawExecutor.indexRangeFits(64,
                VertexFormat.IndexType.INT, -1, 1));
        assertFalse(PrismTerrainDrawExecutor.indexRangeFits(64,
                VertexFormat.IndexType.INT, 0, -1));
        assertFalse(PrismTerrainDrawExecutor.indexRangeFits(64,
                VertexFormat.IndexType.INT, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
