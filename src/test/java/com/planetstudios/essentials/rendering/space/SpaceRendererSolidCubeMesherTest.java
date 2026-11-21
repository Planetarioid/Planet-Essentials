package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererSolidCubeMesherTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void emitsSixFacesForOneIsolatedStoneCube() {
        var states = air();
        states[index(4, 5, 6)] = Blocks.STONE.defaultBlockState();

        var mesh = SpaceRendererSolidCubeMesher.build(snapshot(states)).orElseThrow();

        assertEquals(24, mesh.vertexCount());
        assertEquals(6, mesh.quadCount());
        assertEquals(4, mesh.sectionX());
        assertEquals(PrismFrameStage.BEFORE_ENTITIES, mesh.stage());
        assertEquals(SpaceRendererSolidCubeMesher.MATERIAL_ID, mesh.materialId());
    }

    @Test
    void removesSharedFaceBetweenAdjacentStoneCubes() {
        var states = air();
        states[index(4, 5, 6)] = Blocks.STONE.defaultBlockState();
        states[index(5, 5, 6)] = Blocks.STONE.defaultBlockState();

        var mesh = SpaceRendererSolidCubeMesher.build(snapshot(states)).orElseThrow();

        assertEquals(10, mesh.quadCount());
        assertEquals(40, mesh.vertexCount());
    }

    @Test
    void usesCapturedBorderToCullFaceAcrossSectionBoundary() {
        var states = air();
        states[index(15, 7, 7)] = Blocks.STONE.defaultBlockState();
        states[index(16, 7, 7)] = Blocks.STONE.defaultBlockState();

        var mesh = SpaceRendererSolidCubeMesher.build(snapshot(states)).orElseThrow();

        assertEquals(5, mesh.quadCount());
    }

    @Test
    void ignoresAirAndNonSolidFluidStates() {
        var states = air();
        states[index(2, 2, 2)] = Blocks.WATER.defaultBlockState();

        assertTrue(SpaceRendererSolidCubeMesher.build(snapshot(states)).isEmpty());
    }

    private static SpaceRendererSectionSnapshot snapshot(net.minecraft.world.level.block.state.BlockState[] states) {
        return SpaceRendererSectionSnapshot.fromStates(
                new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 4, 0, 0), states);
    }

    private static net.minecraft.world.level.block.state.BlockState[] air() {
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        return states;
    }

    private static int index(int x, int y, int z) {
        return ((y + 1) * 18 + (z + 1)) * 18 + (x + 1);
    }
}
