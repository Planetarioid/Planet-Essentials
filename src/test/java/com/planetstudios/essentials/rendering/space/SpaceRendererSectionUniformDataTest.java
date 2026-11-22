package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpaceRendererSectionUniformDataTest {
    @Test
    void encodesCameraRelativeSectionOriginAsStd140Vec4() {
        var buffer = SpaceRendererSectionUniformData.encode(
                2, -1, 3, 30.5, -12.25, 40.75);

        assertEquals(SpaceRendererSectionUniformData.BYTE_SIZE, buffer.remaining());
        assertEquals(ByteOrder.nativeOrder(), buffer.order());
        assertEquals(1.5f, buffer.getFloat(), 0.0001f);
        assertEquals(-3.75f, buffer.getFloat(), 0.0001f);
        assertEquals(7.25f, buffer.getFloat(), 0.0001f);
        assertEquals(0.0f, buffer.getFloat(), 0.0001f);
    }

    @Test
    void rejectsNonFiniteCameraCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                SpaceRendererSectionUniformData.encode(0, 0, 0, Double.NaN, 0, 0));
    }

    @Test
    void rejectsOriginsOutsideFloatRange() {
        assertThrows(IllegalArgumentException.class, () ->
                SpaceRendererSectionUniformData.encode(Integer.MAX_VALUE, 0, 0, -Double.MAX_VALUE, 0, 0));
    }
}
