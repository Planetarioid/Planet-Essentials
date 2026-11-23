package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererVanillaPassSnapshotTest {
    @Test void viewportRequiresPositiveDimensionsAndNonNegativeOrigin() {
        assertDoesNotThrow(() -> new SpaceRendererVanillaPassSnapshot.ViewportInput(0, 0, 1920, 1080));
        assertDoesNotThrow(() -> new SpaceRendererVanillaPassSnapshot.ViewportInput(12, 24, 800, 600));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererVanillaPassSnapshot.ViewportInput(-1, 0, 800, 600));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererVanillaPassSnapshot.ViewportInput(0, -1, 800, 600));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererVanillaPassSnapshot.ViewportInput(0, 0, 0, 600));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererVanillaPassSnapshot.ViewportInput(0, 0, 800, 0));
    }

    @Test void scissorRequiresPositiveDimensionsOnlyWhenEnabled() {
        assertDoesNotThrow(() -> new SpaceRendererVanillaPassSnapshot.ScissorInput(false, 0, 0, 0, 0));
        assertDoesNotThrow(() -> new SpaceRendererVanillaPassSnapshot.ScissorInput(true, 0, 0, 10, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererVanillaPassSnapshot.ScissorInput(true, 0, 0, 0, 10));
    }
    @Test void pipelineParityReportOnlyMatchesWhenEveryLayerHasNoDifferences() {
        var matching = new SpaceRendererVanillaPassSnapshot.PipelineParityReport(Map.of());
        assertTrue(matching.matches());

        var mismatching = new SpaceRendererVanillaPassSnapshot.PipelineParityReport(
                Map.of(net.minecraft.client.renderer.chunk.ChunkSectionLayer.SOLID,
                        List.of("depth-test", "blend")));
        assertFalse(mismatching.matches());
        assertEquals(List.of("depth-test", "blend"),
                mismatching.differences().get(net.minecraft.client.renderer.chunk.ChunkSectionLayer.SOLID));
        assertThrows(UnsupportedOperationException.class,
                () -> mismatching.differences().clear());
    }

}
