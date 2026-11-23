package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererRenderGroupBridgeTest {
    @Test
    void preflightNeverEnablesReplacementBeforeParityIsVerified() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                12, 8,
                SpaceRendererRenderGroupBridge.FallbackReason.VANILLA_STATE_PARITY_UNVERIFIED,
                false);

        assertFalse(result.snapshotValidated());
        assertEquals(8, result.plannedDraws());
        assertFalse(result.mayReplaceVanilla());
    }

    @Test
    void parityPreflightDefersUnusedSnapshotCapture() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                15, 6,
                SpaceRendererRenderGroupBridge.FallbackReason.VANILLA_STATE_PARITY_UNVERIFIED,
                false);

        assertFalse(result.snapshotValidated());
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
