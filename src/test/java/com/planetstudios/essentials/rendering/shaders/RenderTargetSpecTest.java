package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderTargetSpecTest {
    @Test
    void acceptsBoundedPositiveDimensions() {
        var spec = new RenderTargetSpec(1920, 1080, true);
        assertEquals(1920, spec.width());
        assertEquals(1080, spec.height());
        assertTrue(spec.depthAttachment());
        assertDoesNotThrow(() -> spec.validateMaxTextureSize(8192));
    }

    @Test
    void rejectsInvalidDimensionsAndExcessivePixelBudgets() {
        assertThrows(IllegalArgumentException.class, () -> new RenderTargetSpec(0, 1080, false));
        assertThrows(IllegalArgumentException.class, () -> new RenderTargetSpec(8192, 8192, true));
    }

    @Test
    void rejectsDimensionsBeyondDeviceLimit() {
        var spec = new RenderTargetSpec(4096, 2160, false);
        assertThrows(IllegalArgumentException.class, () -> spec.validateMaxTextureSize(2048));
    }
}
