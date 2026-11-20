package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainMeshBuilderTest {
    @Test
    void buildsImmutableSectionLocalQuad() {
        var mesh = new PrismTerrainMeshBuilder(1, 2, 3, "example:stone", PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(0,0,0, 16,0,0, 16,16,0, 0,16,0).build();
        assertEquals(4, mesh.vertexCount());
        assertEquals(1, mesh.quadCount());
        float[] copy = mesh.positions();
        copy[0] = 99;
        assertEquals(0, mesh.positions()[0]);
        assertEquals(1, mesh.sectionX());
    }

    @Test
    void rejectsInvalidCoordinatesWithoutCorruptingBuilder() {
        var builder = new PrismTerrainMeshBuilder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES);
        assertThrows(IllegalArgumentException.class, () ->
                builder.addQuad(-1,0,0, 1,0,0, 1,1,0, 0,1,0));
        var mesh = builder.addQuad(0,0,0, 1,0,0, 1,1,0, 0,1,0).build();
        assertEquals(4, mesh.vertexCount());
    }

    @Test
    void rejectsUnsupportedStagesAndEmptyMeshes() {
        assertThrows(IllegalArgumentException.class, () ->
                new PrismTerrainMeshBuilder(0,0,0,"example:stone",PrismFrameStage.END_MAIN));
        var empty = new PrismTerrainMeshBuilder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES);
        assertThrows(IllegalStateException.class, empty::build);
    }

    @Test
    void builderCanOnlyBeConsumedOnce() {
        var builder = new PrismTerrainMeshBuilder(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(0,0,0, 1,0,0, 1,1,0, 0,1,0);
        builder.build();
        assertThrows(IllegalStateException.class, builder::build);
    }
}
