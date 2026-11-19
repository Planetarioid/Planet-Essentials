package com.planetstudios.essentials.rendering.shaders.client.terrain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainPreparedRenderGroupTest {
    @Test
    void groupIndexRequirementUsesLargestMesh() {
        assertEquals(24, PrismTerrainPreparedRenderGroup.requiredIndexCount(List.of(4, 16, 8)));
    }

    @Test
    void groupIndexRequirementRejectsEmptyAndMalformedMeshes() {
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderGroup.requiredIndexCount(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderGroup.requiredIndexCount(List.of(4, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderGroup.requiredIndexCount(List.of(4, 6)));
    }

    @Test
    void groupIndexRequirementRejectsIndexCountOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> PrismTerrainPreparedRenderGroup.requiredIndexCount(
                        List.of(Integer.MAX_VALUE - 3)));
    }
}
