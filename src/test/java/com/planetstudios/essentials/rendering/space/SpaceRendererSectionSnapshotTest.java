package com.planetstudios.essentials.rendering.space;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererSectionSnapshotTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void storesCenterAndNeighborBorderWithStableCoordinates() {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 2, 3, 4);
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        states[index(0, 0, 0)] = Blocks.STONE.defaultBlockState();
        states[index(-1, 16, 16)] = Blocks.WATER.defaultBlockState();

        var snapshot = SpaceRendererSectionSnapshot.fromStates(key, states);
        assertEquals(Blocks.STONE, snapshot.blockState(0, 0, 0).getBlock());
        assertEquals(Blocks.WATER, snapshot.blockState(-1, 16, 16).getBlock());
        assertEquals(Blocks.WATER.defaultBlockState().getFluidState(), snapshot.fluidState(-1, 16, 16));
    }

    @Test void storesImmutableFluidGeometryInputs() {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        var fluids = new SpaceRendererSectionSnapshot.FluidSample[SpaceRendererSectionSnapshot.STATE_COUNT];
        fluids[index(4, 5, 6)] = new SpaceRendererSectionSnapshot.FluidSample(
                "minecraft:water", 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, true);

        var snapshot = SpaceRendererSectionSnapshot.fromStates(key, states,
                fullBrightLights(), new int[SpaceRendererSectionSnapshot.STATE_COUNT][], fluids);
        assertEquals(new SpaceRendererSectionSnapshot.FluidSample(
                "minecraft:water", 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, true),
                snapshot.fluidSample(4, 5, 6));
        assertNull(snapshot.fluidSample(0, 0, 0));
        fluids[index(4, 5, 6)] = null;
        assertNotNull(snapshot.fluidSample(4, 5, 6), "snapshot must defensively copy fluid samples");
        assertTrue(snapshot.estimatedBytes() > (long) SpaceRendererSectionSnapshot.STATE_COUNT
                * (Long.BYTES + Integer.BYTES));
    }

    @Test void preservesFluidSamplesAcrossTheSectionBorder() {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        var fluids = new SpaceRendererSectionSnapshot.FluidSample[SpaceRendererSectionSnapshot.STATE_COUNT];
        fluids[index(16, 8, 8)] = new SpaceRendererSectionSnapshot.FluidSample(
                "minecraft:lava", 0.875f, 0.875f, 0.25f, 0.0f, -0.5f, false);
        var snapshot = SpaceRendererSectionSnapshot.fromStates(key, states,
                fullBrightLights(), new int[SpaceRendererSectionSnapshot.STATE_COUNT][], fluids);
        assertEquals("minecraft:lava", snapshot.fluidSample(16, 8, 8).typeId());
        assertEquals(0.875f, snapshot.fluidSample(16, 8, 8).height());
        assertEquals(0.25f, snapshot.fluidSample(16, 8, 8).flowX());
        assertEquals(-0.5f, snapshot.fluidSample(16, 8, 8).flowZ());
        assertFalse(snapshot.fluidSample(16, 8, 8).source());
        assertNull(snapshot.fluidSample(15, 8, 8));
    }

    @Test void storesImmutableFluidSpriteUvAndTintMetadata() {
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        var fluids = new SpaceRendererSectionSnapshot.FluidSample[SpaceRendererSectionSnapshot.STATE_COUNT];
        var visuals = new SpaceRendererSectionSnapshot.FluidVisual[SpaceRendererSectionSnapshot.STATE_COUNT];
        fluids[index(3, 4, 5)] = new SpaceRendererSectionSnapshot.FluidSample(
                "minecraft:water", 1, 1, 0, 0, 0, true);
        var still = new SpaceRendererSectionSnapshot.FluidSprite("minecraft:textures/atlas/blocks.png",
                0.1f, 0.2f, 0.3f, 0.4f);
        var flowing = new SpaceRendererSectionSnapshot.FluidSprite("minecraft:textures/atlas/blocks.png",
                0.4f, 0.5f, 0.6f, 0.7f);
        var overlay = new SpaceRendererSectionSnapshot.FluidSprite("minecraft:textures/atlas/blocks.png",
                0.7f, 0.1f, 0.9f, 0.3f);
        visuals[index(3, 4, 5)] = new SpaceRendererSectionSnapshot.FluidVisual(still, flowing, overlay, 0x7FABCDEF);

        var snapshot = SpaceRendererSectionSnapshot.fromStates(key, states, fullBrightLights(),
                new int[SpaceRendererSectionSnapshot.STATE_COUNT][], fluids, visuals);
        assertEquals(still, snapshot.fluidVisual(3, 4, 5).still());
        assertEquals(flowing, snapshot.fluidVisual(3, 4, 5).flowing());
        assertEquals(overlay, snapshot.fluidVisual(3, 4, 5).overlay());
        assertEquals(0x7FABCDEF, snapshot.fluidVisual(3, 4, 5).tintColor());
        visuals[index(3, 4, 5)] = null;
        assertNotNull(snapshot.fluidVisual(3, 4, 5), "snapshot must defensively copy visual metadata");
        assertNull(snapshot.fluidVisual(0, 0, 0));
    }

    @Test void rejectsInvalidFluidSpriteMetadata() {
        assertThrows(NullPointerException.class, () -> new SpaceRendererSectionSnapshot.FluidSprite(null, 0, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererSectionSnapshot.FluidSprite("atlas", 0.8f, 0, 0.2f, 1));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererSectionSnapshot.FluidSprite("atlas", 0, 0, Float.NaN, 1));
    }

    @Test void rejectsInvalidFluidMetadata() {
        assertThrows(NullPointerException.class, () -> new SpaceRendererSectionSnapshot.FluidSample(null, 1, 1, 0, 0, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererSectionSnapshot.FluidSample(" ", 1, 1, 0, 0, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererSectionSnapshot.FluidSample("minecraft:water", Float.NaN, 1, 0, 0, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererSectionSnapshot.FluidSample("minecraft:water", 1, 1, 0, Float.POSITIVE_INFINITY, 0, true));
    }

    @Test void cacheIsByteBoundedAndEvictsLeastRecentlyUsed() {
        var cache = new SpaceRendererSectionSnapshotCache(
                SpaceRendererSectionSnapshot.STATE_COUNT * (Long.BYTES + Integer.BYTES) + 1L);
        var first = snapshot("minecraft:overworld", 0);
        var second = snapshot("minecraft:overworld", 1);
        assertTrue(cache.put(first));
        assertNotNull(cache.get(first.key()));
        assertTrue(cache.put(second));
        assertNull(cache.get(first.key()));
        assertNotNull(cache.get(second.key()));
        assertTrue(cache.currentBytes() <= cache.maxBytes());
    }

    @Test void rejectsCoordinatesOutsideOneBlockBorder() {
        var snapshot = snapshot("minecraft:overworld", 0);
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.blockState(-2, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.blockState(0, 17, 0));
    }

    private static SpaceRendererSectionSnapshot snapshot(String dimension, int x) {
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return SpaceRendererSectionSnapshot.fromStates(
                new SpaceRendererSectionManager.SectionKey(dimension, x, 0, 0), states);
    }

    private static int[] fullBrightLights() {
        int[] lights = new int[SpaceRendererSectionSnapshot.STATE_COUNT];
        java.util.Arrays.fill(lights, 0x00F000F0);
        return lights;
    }

    private static int index(int x, int y, int z) {
        return ((y + 1) * 18 + (z + 1)) * 18 + (x + 1);
    }
}
