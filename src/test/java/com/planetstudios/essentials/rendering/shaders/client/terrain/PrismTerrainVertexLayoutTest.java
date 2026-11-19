package com.planetstudios.essentials.rendering.shaders.client.terrain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVertexLayoutTest {
    @Test void minecraftFormatMatchesSerializedStrideAndAttributeOffsets() {
        var format=PrismTerrainVertexLayout.minecraftVertexFormat();
        assertEquals(36,format.getVertexSize());
        assertEquals(0,format.getOffset(0));
        assertEquals(12,format.getOffset(1));
        assertEquals(20,format.getOffset(2));
        assertEquals(24,format.getOffset(3));
        assertEquals(28,format.getOffset(4));
        assertEquals(32,format.getOffset(5));
        assertEquals(6,format.getElements().size());
    }
}
