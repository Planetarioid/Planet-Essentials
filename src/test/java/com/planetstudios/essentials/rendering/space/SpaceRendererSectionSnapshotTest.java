package com.planetstudios.essentials.rendering.space;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererSectionSnapshotTest {
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

    @Test void cacheIsByteBoundedAndEvictsLeastRecentlyUsed() {
        var cache = new SpaceRendererSectionSnapshotCache(SpaceRendererSectionSnapshot.STATE_COUNT * Long.BYTES + 1L);
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

    private static int index(int x, int y, int z) {
        return ((y + 1) * 18 + (z + 1)) * 18 + (x + 1);
    }
}
