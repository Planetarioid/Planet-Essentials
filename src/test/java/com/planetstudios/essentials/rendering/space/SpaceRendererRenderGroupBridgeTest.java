package com.planetstudios.essentials.rendering.space;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererRenderGroupBridgeTest {
    @Test
    void preflightNeverEnablesReplacementBeforeParityIsVerified() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                12, 8,
                SpaceRendererRenderGroupBridge.FallbackReason.RUNTIME_PARITY_UNVERIFIED,
                true);

        assertTrue(result.snapshotValidated());
        assertEquals(8, result.plannedDraws());
        assertFalse(result.mayReplaceVanilla());
    }

    @Test
    void capturedStateMismatchIsReportedAndImmutable() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                16, 5,
                SpaceRendererRenderGroupBridge.FallbackReason.CAPTURED_STATE_MISMATCH,
                true,
                Set.of("uniform:Fog"),
                Map.of(ChunkSectionLayer.TRANSLUCENT, List.of("blend")),
                false);

        assertTrue(result.snapshotValidated());
        assertEquals(Set.of("uniform:Fog"), result.missingResources());
        assertEquals(List.of("blend"),
                result.pipelineDifferences().get(ChunkSectionLayer.TRANSLUCENT));
        assertThrows(UnsupportedOperationException.class,
                () -> result.missingResources().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> result.pipelineDifferences().clear());
        assertFalse(result.mayReplaceVanilla());
    }

    @Test
    void unavailableSnapshotFailsClosed() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                13, 4,
                SpaceRendererRenderGroupBridge.FallbackReason.SNAPSHOT_UNAVAILABLE,
                false);

        assertFalse(result.mayReplaceVanilla());
        assertFalse(result.snapshotValidated());
    }

    @Test
    void incompleteCoverageFailsClosed() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                14, 2,
                SpaceRendererRenderGroupBridge.FallbackReason.INCOMPLETE_GROUP_COVERAGE,
                false);

        assertFalse(result.mayReplaceVanilla());
    }
}
