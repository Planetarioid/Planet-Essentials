package com.planetstudios.essentials.rendering.space;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererFluidMesherTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void emitsTopAndFourExposedSidesForAnIsolatedFluidCell() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f))).getFirst();

        assertEquals("minecraft:water", geometry.typeId());
        assertEquals(5, geometry.quadCount());
        assertEquals(1, countFacing(geometry, 0, 1, 0));
        assertEquals(4, countVertical(geometry));
    }

    @Test
    void removesSharedSideBetweenEqualHeightAdjacentFluidCells() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f),
                cell(5, 5, 6, "minecraft:water", 1.0f))).getFirst();

        assertEquals(8, geometry.quadCount(), "two tops plus six exposed outer sides");
    }

    @Test
    void emitsOnlyTheExposedHeightDifferenceBetweenUnequalAdjacentCells() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f),
                cell(5, 5, 6, "minecraft:water", 0.5f))).getFirst();

        assertEquals(9, geometry.quadCount());
        var sharedWall = geometry.quads().stream()
                .filter(q -> q.a().normalX() == 1.0f && q.a().x() == 5.0f)
                .findFirst().orElseThrow();
        assertEquals(5.5f, minY(sharedWall));
        assertEquals(6.0f, maxY(sharedWall));
    }

    @Test
    void suppressesTopSurfaceWhenSameFluidContinuesAbove() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:lava", 1.0f),
                cell(4, 6, 6, "minecraft:lava", 1.0f))).getFirst();

        assertEquals(9, geometry.quadCount());
        assertEquals(1, countFacing(geometry, 0, 1, 0),
                "only the upper fluid cell has a visible top");
    }

    @Test
    void keepsDifferentFluidTypesInSeparateGeometryGroups() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f),
                cell(5, 5, 6, "minecraft:lava", 1.0f)));

        assertEquals(2, geometry.size());
        assertEquals("minecraft:water", geometry.get(0).typeId());
        assertEquals("minecraft:lava", geometry.get(1).typeId());
    }

    @Test
    void emitsOutwardFacingNormalsForAllFourSideDirections() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f))).getFirst();

        assertEquals(1, countFacing(geometry, -1, 0, 0));
        assertEquals(1, countFacing(geometry, 1, 0, 0));
        assertEquals(1, countFacing(geometry, 0, 0, -1));
        assertEquals(1, countFacing(geometry, 0, 0, 1));
    }

    private static int countFacing(SpaceRendererFluidMesher.FluidGeometry geometry,
            float nx, float ny, float nz) {
        return (int) geometry.quads().stream().filter(q ->
                q.a().normalX() == nx && q.a().normalY() == ny && q.a().normalZ() == nz).count();
    }

    private static int countVertical(SpaceRendererFluidMesher.FluidGeometry geometry) {
        return (int) geometry.quads().stream().filter(q ->
                q.a().normalY() == 0.0f && (q.a().normalX() != 0.0f || q.a().normalZ() != 0.0f)).count();
    }

    private static float minY(SpaceRendererFluidMesher.Quad quad) {
        return Math.min(Math.min(quad.a().y(), quad.b().y()), Math.min(quad.c().y(), quad.d().y()));
    }

    private static float maxY(SpaceRendererFluidMesher.Quad quad) {
        return Math.max(Math.max(quad.a().y(), quad.b().y()), Math.max(quad.c().y(), quad.d().y()));
    }

    private static SpaceRendererSectionSnapshot snapshotWithFluids(Cell... cells) {
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        var samples = new SpaceRendererSectionSnapshot.FluidSample[SpaceRendererSectionSnapshot.STATE_COUNT];
        for (Cell cell : cells) {
            states[index(cell.x, cell.y, cell.z)] = cell.typeId.equals("minecraft:water")
                    ? Blocks.WATER.defaultBlockState() : Blocks.LAVA.defaultBlockState();
            samples[index(cell.x, cell.y, cell.z)] = new SpaceRendererSectionSnapshot.FluidSample(
                    cell.typeId, cell.height, cell.height, 0.0f, 0.0f, 0.0f, cell.height == 1.0f);
        }
        int[] lights = new int[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(lights, 0x00F000F0);
        return SpaceRendererSectionSnapshot.fromStates(
                new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0),
                states, lights, new int[SpaceRendererSectionSnapshot.STATE_COUNT][], samples);
    }

    private static Cell cell(int x, int y, int z, String typeId, float height) {
        return new Cell(x, y, z, typeId, height);
    }

    private static int index(int x, int y, int z) {
        return ((y + 1) * 18 + (z + 1)) * 18 + (x + 1);
    }

    private record Cell(int x, int y, int z, String typeId, float height) {}
}
