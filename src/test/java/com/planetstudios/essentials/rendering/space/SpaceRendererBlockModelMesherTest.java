package com.planetstudios.essentials.rendering.space;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    @Test
    void sectionSnapshotPreservesPackedBlockAndSkyLight() {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        int[] lights = new int[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(lights, LightTexture.pack(3, 11));

        var snapshot = SpaceRendererSectionSnapshot.fromStates(key, states, lights);
        assertEquals(LightTexture.pack(3, 11), snapshot.packedLight(0, 0, 0));
        assertEquals(LightTexture.pack(3, 11), snapshot.packedLight(-1, 16, 16));
    }
}
