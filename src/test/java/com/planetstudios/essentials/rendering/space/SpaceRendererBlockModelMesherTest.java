package com.planetstudios.essentials.rendering.space;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SpaceRendererBlockModelMesherTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

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
        BlockState[] states = airStates();
        int[] lights = new int[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(lights, packLight(3, 11));

        var snapshot = SpaceRendererSectionSnapshot.fromStates(key, states, lights);
        assertEquals(packLight(3, 11), snapshot.packedLight(0, 0, 0));
        assertEquals(packLight(3, 11), snapshot.packedLight(-1, 16, 16));
    }

    @Test
    void snapshotRetainsPerBlockTintIndexAndResolvedColor() {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        int[][] tints = new int[SpaceRendererSectionSnapshot.STATE_COUNT][];
        tints[index(2, 3, 4)] = new int[]{0, 0x63B34A, 7, 0xD9A441};
        var snapshot = SpaceRendererSectionSnapshot.fromStates(
                key, airStates(), uniformLights(packLight(0, 15)), tints);

        assertEquals(0x63B34A, snapshot.tintColor(2, 3, 4, 0));
        assertEquals(0xD9A441, snapshot.tintColor(2, 3, 4, 7));
        assertEquals(-1, snapshot.tintColor(2, 3, 4, 1));
        assertEquals(-1, snapshot.tintColor(3, 3, 4, 0));
    }

    @Test
    void tintMultipliesRgbWithoutChangingAoAlpha() {
        int ao = 0xC2C2C2C2;
        int tinted = SpaceRendererBlockModelMesher.multiplyTintAndAo(0x63B34A, ao);
        assertEquals(0xC24B8838, tinted);
        assertEquals(ao, SpaceRendererBlockModelMesher.multiplyTintAndAo(-1, ao));
    }

    @Test
    void vertexLightingPreservesUniformLightWithoutOcclusion() {
        var snapshot = snapshot(airStates(), uniformLights(packLight(4, 8)));
        var result = SpaceRendererBlockModelMesher.vertexLighting(snapshot, 4, 4, 4,
                0.0f, 0.0f, 0.0f, Direction.UP, packLight(4, 8));
        assertEquals(packLight(4, 8), result.packedLight());
        assertEquals(0xFFFFFFFF, result.color());
    }

    @Test
    void vertexLightingAveragesBlockAndSkyLightIndependently() {
        int[] lights = uniformLights(packLight(0, 0));
        lights[index(4, 5, 4)] = packLight(0, 4);
        lights[index(3, 5, 4)] = packLight(4, 8);
        lights[index(4, 5, 3)] = packLight(8, 12);
        lights[index(3, 5, 3)] = packLight(12, 4);
        var snapshot = snapshot(airStates(), lights);

        var result = SpaceRendererBlockModelMesher.vertexLighting(snapshot, 4, 4, 4,
                0.0f, 0.0f, 0.0f, Direction.UP, packLight(0, 0));

        assertEquals(packLight(6, 7), result.packedLight());
    }

    @Test
    void vertexLightingDarkensCornersWithTwoOccludingSideSamples() {
        BlockState[] states = airStates();
        states[index(3, 5, 4)] = Blocks.STONE.defaultBlockState();
        states[index(4, 5, 3)] = Blocks.STONE.defaultBlockState();
        var snapshot = snapshot(states, uniformLights(packLight(4, 8)));

        var result = SpaceRendererBlockModelMesher.vertexLighting(snapshot, 4, 4, 4,
                0.0f, 0.0f, 0.0f, Direction.UP, packLight(4, 8));

        assertNotEquals(0xFFFFFFFF, result.color());
        assertEquals(0xC2C2C2C2, result.color());
    }

    private static SpaceRendererSectionSnapshot snapshot(BlockState[] states, int[] lights) {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        return SpaceRendererSectionSnapshot.fromStates(key, states, lights);
    }

    private static BlockState[] airStates() {
        BlockState[] states = new BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return states;
    }

    private static int[] uniformLights(int value) {
        int[] lights = new int[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(lights, value);
        return lights;
    }

    private static int index(int x, int y, int z) {
        return ((y + 1) * SpaceRendererSectionSnapshot.EDGE + (z + 1))
                * SpaceRendererSectionSnapshot.EDGE + (x + 1);
    }

    private static int packLight(int block, int sky) {
        return (block << 4) | (sky << 20);
    }
}
