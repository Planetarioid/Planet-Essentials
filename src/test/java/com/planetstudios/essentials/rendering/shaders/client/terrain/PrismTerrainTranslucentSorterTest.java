package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainTranslucentSorterTest {
    private static PrismTerrainVertex v(float x,float y,float z) {
        return new PrismTerrainVertex(x,y,z,0,0,0xFFFFFFFF,0,0,0);
    }

    @Test void sortsQuadIndicesFarToNearLikeVanilla() {
        var data=PrismTerrainVertexData.builder(0,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0),v(1,0,0),v(1,1,0),v(0,1,0))
                .addQuad(v(0,0,8),v(1,0,8),v(1,1,8),v(0,1,8))
                .quadCenters(new float[]{0,0,1, 0,0,8})
                .build();
        short[] indices=PrismTerrainTranslucentSorter.sortedIndices(data,0,0,0);
        assertArrayEquals(new short[]{4,5,6,6,7,4, 0,1,2,2,3,0},indices);
    }

    @Test void sortsRetainedSectionLocalCentersUsingWorldCameraCoordinates() {
        short[] indices = PrismTerrainTranslucentSorter.sortedIndices(
                new float[]{0,0,1, 0,0,8}, 2,0,0, 32,0,0);
        assertArrayEquals(new short[]{4,5,6,6,7,4, 0,1,2,2,3,0}, indices);
    }

    @Test void rejectsInvalidRetainedCenterArrays() {
        assertThrows(IllegalArgumentException.class, () ->
                PrismTerrainTranslucentSorter.sortedIndices(new float[]{0,1},0,0,0,0,0,0));
        assertThrows(IllegalArgumentException.class, () ->
                PrismTerrainTranslucentSorter.sortedIndices(new float[]{0,Float.NaN,0},0,0,0,0,0,0));
    }

    @Test void rejectsPayloadsWithoutSortMetadata() {
        var data=PrismTerrainVertexData.builder(0,0,0,"example:glass",PrismFrameStage.BEFORE_TRANSLUCENT)
                .addQuad(v(0,0,0),v(1,0,0),v(1,1,0),v(0,1,0)).build();
        assertThrows(IllegalArgumentException.class,
                ()->PrismTerrainTranslucentSorter.sortedIndices(data,0,0,0));
    }
}
