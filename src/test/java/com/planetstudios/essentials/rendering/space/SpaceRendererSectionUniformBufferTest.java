package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpaceRendererSectionUniformBufferTest {
    @Test
    void alignsStd140Vec4ToDeviceUniformOffsetAlignment() {
        assertEquals(16, SpaceRendererSectionUniformBuffer.alignedStride(16));
        assertEquals(256, SpaceRendererSectionUniformBuffer.alignedStride(256));
        assertEquals(24, SpaceRendererSectionUniformBuffer.alignedStride(24));
        assertEquals(16, SpaceRendererSectionUniformBuffer.alignedStride(8));
    }

    @Test
    void rejectsInvalidAlignment() {
        assertThrows(IllegalArgumentException.class,
                () -> SpaceRendererSectionUniformBuffer.alignedStride(0));
        assertThrows(IllegalArgumentException.class,
                () -> SpaceRendererSectionUniformBuffer.alignedStride(-256));
    }
}
