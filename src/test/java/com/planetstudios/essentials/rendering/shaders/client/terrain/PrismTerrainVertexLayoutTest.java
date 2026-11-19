package com.planetstudios.essentials.rendering.shaders.client.terrain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVertexLayoutTest {
    @Test void minecraftFormatMatchesSerializedStrideAndAttributeOffsets() {
        var format=PrismTerrainVertexLayout.minecraftVertexFormat();
        int[] offsets=format.getOffsetsByElement();
        assertEquals(36,format.getVertexSize());
        assertArrayEquals(new int[]{0,12,20,24,28,32},offsets);
        assertEquals(6,format.getElements().size());
    }
}
