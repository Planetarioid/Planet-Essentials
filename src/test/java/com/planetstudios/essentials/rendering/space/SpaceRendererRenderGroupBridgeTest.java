package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererRenderGroupBridgeTest {
    @Test
    void preflightNeverEnablesReplacementBeforeParityIsVerified() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                12, 8,
                SpaceRendererRenderGroupBridge.FallbackReason.VANILLA_STATE_PARITY_UNVERIFIED,
                true);

        assertTrue(result.snapshotValidated());
        assertEquals(8, result.plannedDraws());
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
