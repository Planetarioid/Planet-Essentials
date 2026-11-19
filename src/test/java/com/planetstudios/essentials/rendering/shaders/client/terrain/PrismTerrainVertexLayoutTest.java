package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVertexLayoutTest {
    @Test
    void minecraftFormatMatchesSerializedStrideAndAttributeOffsets() {
        var format = PrismTerrainVertexLayout.minecraftVertexFormat();

        assertEquals(36, format.getVertexSize());
        assertEquals(0, format.getOffset(VertexFormatElement.POSITION));
        assertEquals(12, format.getOffset(VertexFormatElement.UV0));
        assertEquals(20, format.getOffset(VertexFormatElement.COLOR));
        assertEquals(24, format.getOffset(VertexFormatElement.UV2));
        assertEquals(28, format.getOffset(VertexFormatElement.UV1));
        assertEquals(32, format.getOffset(VertexFormatElement.NORMAL));
        assertEquals(6, format.getElements().size());
    }
}
