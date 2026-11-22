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
    void appliesCapturedStillAndFlowingAtlasUvsAndFluidTintToGeometry() {
        var spriteAtlas = "minecraft:textures/atlas/blocks.png";
        var still = new SpaceRendererSectionSnapshot.FluidSprite(spriteAtlas, 0.1f, 0.2f, 0.3f, 0.4f);
        var flowing = new SpaceRendererSectionSnapshot.FluidSprite(spriteAtlas, 0.5f, 0.6f, 0.9f, 0.8f);
        var visual = new SpaceRendererSectionSnapshot.FluidVisual(still, flowing, null, 0xFF74AADD);
        var snapshot = snapshotWithVisual(cell(4, 5, 6, "minecraft:water", 1.0f), visual);

        var geometry = SpaceRendererFluidMesher.build(snapshot).getFirst();
        var top = geometry.quads().stream().filter(q -> q.a().normalY() == 1.0f).findFirst().orElseThrow();
        var side = geometry.quads().stream().filter(q -> q.a().normalY() == 0.0f).findFirst().orElseThrow();

        assertEquals(spriteAtlas, top.atlasId());
        assertEquals(0xFF74AADD, top.tintColor());
        assertEquals(0.1f, top.a().u(), 0.0001f);
        assertEquals(0.2f, top.a().v(), 0.0001f);
        assertEquals(spriteAtlas, side.atlasId());
        assertEquals(0xFF74AADD, side.tintColor());
        assertTrue(side.a().u() >= flowing.u0() && side.a().u() <= flowing.u1());
        assertTrue(side.a().v() >= flowing.v0() && side.a().v() <= flowing.v1());
    }

    @Test
    void convertsFluidGeometryIntoSharedTranslucentTerrainPayload() {
        var atlas = "minecraft:textures/atlas/blocks.png";
        var still = new SpaceRendererSectionSnapshot.FluidSprite(atlas, 0.1f, 0.2f, 0.3f, 0.4f);
        var flowing = new SpaceRendererSectionSnapshot.FluidSprite(atlas, 0.5f, 0.6f, 0.9f, 0.8f);
        var visual = new SpaceRendererSectionSnapshot.FluidVisual(still, flowing, null, 0xFF74AADD);
        var snapshot = snapshotWithVisual(cell(4, 5, 6, "minecraft:water", 1.0f), visual);

        var payload = SpaceRendererFluidMesher.buildTerrainPayload(snapshot).orElseThrow();

        assertEquals(net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT, payload.layer());
        assertEquals(com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage.BEFORE_TRANSLUCENT,
                payload.data().stage());
        assertEquals(atlas, payload.data().materialId());
        assertEquals(5, payload.data().quadCount());
        assertTrue(payload.data().hasQuadCenters());
        assertEquals(15, payload.data().quadCount() * 3);
        var vertices = payload.data().readOnlyBuffer().order(java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(0.1f, vertices.getFloat(12), 0.0001f);
        assertEquals(0.2f, vertices.getFloat(16), 0.0001f);
        assertEquals(0xFF74AADD, vertices.getInt(20));
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
    void suppressesSharedSideBetweenUnequalAdjacentCellsOfTheSameFluid() {
        var geometry = SpaceRendererFluidMesher.build(snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f),
                cell(5, 5, 6, "minecraft:water", 0.5f))).getFirst();

        assertEquals(8, geometry.quadCount(),
                "same-fluid neighbors do not render an internal wall, even at different levels");
    }

    @Test
    void smoothsCornerUsingWeightedDiagonalAndCardinalFluidHeights() {
        var snapshot = snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f),
                cell(4, 5, 5, "minecraft:water", 0.5f),
                cell(5, 5, 6, "minecraft:water", 0.25f),
                cell(5, 5, 5, "minecraft:water", 0.75f));

        var heights = SpaceRendererFluidMesher.cornerHeights(snapshot, "minecraft:water", 4, 5, 6);

        assertEquals(11.5f / 13.0f, heights.northEast(), 0.0001f);
    }

    @Test
    void fullHeightCardinalNeighborKeepsSharedCornerAtFullHeight() {
        var snapshot = snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 0.4f),
                cell(4, 5, 5, "minecraft:water", 1.0f));

        var heights = SpaceRendererFluidMesher.cornerHeights(snapshot, "minecraft:water", 4, 5, 6);

        assertEquals(1.0f, heights.northWest());
        assertEquals(1.0f, heights.northEast());
    }

    @Test
    void topQuadUsesFourIndependentSmoothedCornerHeights() {
        var snapshot = snapshotWithFluids(
                cell(4, 5, 6, "minecraft:water", 1.0f),
                cell(4, 5, 5, "minecraft:water", 0.5f),
                cell(5, 5, 6, "minecraft:water", 0.25f),
                cell(5, 5, 5, "minecraft:water", 0.75f));

        var geometry = SpaceRendererFluidMesher.build(snapshot).getFirst();
        var top = geometry.quads().stream().filter(q -> q.a().normalY() == 1.0f)
                .filter(q -> q.a().x() == 4.0f && q.a().z() == 6.0f)
                .findFirst().orElseThrow();

        assertEquals(5.0f + 11.5f / 13.0f, top.d().y(), 0.0001f);
        assertNotEquals(top.a().y(), top.b().y());
        assertNotEquals(top.b().y(), top.c().y());
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

    private static SpaceRendererSectionSnapshot snapshotWithVisual(Cell cell,
            SpaceRendererSectionSnapshot.FluidVisual visual) {
        var states = new net.minecraft.world.level.block.state.BlockState[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        states[index(cell.x, cell.y, cell.z)] = Blocks.WATER.defaultBlockState();
        var samples = new SpaceRendererSectionSnapshot.FluidSample[SpaceRendererSectionSnapshot.STATE_COUNT];
        samples[index(cell.x, cell.y, cell.z)] = new SpaceRendererSectionSnapshot.FluidSample(
                cell.typeId, cell.height, cell.height, 0.0f, 0.0f, 0.0f, cell.height == 1.0f);
        var visuals = new SpaceRendererSectionSnapshot.FluidVisual[SpaceRendererSectionSnapshot.STATE_COUNT];
        visuals[index(cell.x, cell.y, cell.z)] = visual;
        int[] lights = new int[SpaceRendererSectionSnapshot.STATE_COUNT];
        Arrays.fill(lights, 0x00F000F0);
        return SpaceRendererSectionSnapshot.fromStates(
                new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0),
                states, lights, new int[SpaceRendererSectionSnapshot.STATE_COUNT][], samples, visuals);
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
