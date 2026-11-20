package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainSectionUniformTest {
    @Test
    void encodesCameraRelativeOriginAsStd140Vec4() {
        var transform = PrismTerrainCameraTransform.forSection(
                4, -2, 7, 63.5, -32.0, 112.25);
        ByteBuffer data = PrismTerrainSectionUniform.encode(transform);

        assertEquals(PrismTerrainSectionUniform.BYTE_SIZE, data.remaining());
        assertEquals(ByteOrder.nativeOrder(), data.order());
        assertEquals(0.5f, data.getFloat(PrismTerrainSectionUniform.ORIGIN_X_OFFSET), 0.0001f);
        assertEquals(0.0f, data.getFloat(PrismTerrainSectionUniform.ORIGIN_Y_OFFSET), 0.0001f);
        assertEquals(-0.25f, data.getFloat(PrismTerrainSectionUniform.ORIGIN_Z_OFFSET), 0.0001f);
        assertEquals(1.0f, data.getFloat(PrismTerrainSectionUniform.RESERVED_OFFSET), 0.0001f);
    }

    @Test
    void requiresPrismSectionUniformDeclaration() {
        var layout = new ShaderBindingLayout(List.of("PrismSection"), List.of(), List.of());
        assertDoesNotThrow(() -> PrismTerrainSectionUniform.requireDeclared(layout::uniformBuffers));
        assertThrows(IllegalArgumentException.class, () ->
                PrismTerrainSectionUniform.requireDeclared(ShaderBindingLayout.EMPTY::uniformBuffers));
    }

    @Test
    void returnsIndependentDirectBuffers() {
        var transform = PrismTerrainCameraTransform.forSection(0, 0, 0, 1, 2, 3);
        ByteBuffer first = PrismTerrainSectionUniform.encode(transform);
        ByteBuffer second = PrismTerrainSectionUniform.encode(transform);

        assertTrue(first.isDirect());
        assertNotSame(first, second);
        assertEquals(PrismTerrainSectionUniform.BYTE_SIZE, first.capacity());
    }
}
