package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaceRendererBlockModelMesherTest {
    @Test
    void unpacksAtlasUvCoordinatesFromMinecraftPackedLong() {
        float u = 0.3125f;
        float v = 0.6875f;
        long packed = ((long) Float.floatToIntBits(u) << 32)
                | (Float.floatToIntBits(v) & 0xFFFFFFFFL);

        assertEquals(u, SpaceRendererBlockModelMesher.unpackU(packed));
        assertEquals(v, SpaceRendererBlockModelMesher.unpackV(packed));
    }
}
