package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;
import java.nio.ByteOrder;
import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererSortedIndexBufferTest {
    @Test void encodesUnsignedShortIndicesInLittleEndianOrder() {
        var bytes = SpaceRendererSortedIndexBuffer.encode(new short[]{0, 1, 255, (short) 0xFFFF, 2, 3});
        assertEquals(ByteOrder.LITTLE_ENDIAN, bytes.order());
        assertEquals(12, bytes.remaining());
        assertEquals(0, bytes.getShort());
        assertEquals(1, bytes.getShort());
        assertEquals(255, bytes.getShort());
        assertEquals((short) 0xFFFF, bytes.getShort());
        assertEquals(2, bytes.getShort());
        assertEquals(3, bytes.getShort());
    }

    @Test void rejectsEmptyOrIncompleteIndexStreams() {
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererSortedIndexBuffer.encode(new short[0]));
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererSortedIndexBuffer.encode(new short[]{0,1,2}));
    }
}
