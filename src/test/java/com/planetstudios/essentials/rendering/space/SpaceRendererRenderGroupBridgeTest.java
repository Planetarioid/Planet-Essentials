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
    void invalidDrawPlanHasItsOwnFailClosedReason() {
        var result = new SpaceRendererRenderGroupBridge.Preflight(
                17, 3,
                SpaceRendererRenderGroupBridge.FallbackReason.DRAW_PLAN_INVALID,
                false);

        assertFalse(result.snapshotValidated());
        assertFalse(result.mayReplaceVanilla());
    }

    @Test
    void resourcePreflightRejectsInvalidCapacityBeforeAnyPassCanOpen() {
        var empty = SpaceRendererDrawPlan.empty(18);
        assertThrows(IllegalArgumentException.class,
                () -> SpaceRendererDrawExecutor.validatePlanResources(empty, 0));
        assertDoesNotThrow(
                () -> SpaceRendererDrawExecutor.validatePlanResources(empty, 1));
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

    @Test
    void exactVanillaDrawIdentitiesAreRequiredForEveryGroupLayer() {
        Object solidA = new Object();
        Object solidB = new Object();
        Object translucentA = new Object();
        var vanilla = Map.of(
                ChunkSectionLayer.SOLID, List.of(solidA, solidB),
                ChunkSectionLayer.TRANSLUCENT, List.of(translucentA));
        var planned = Map.of(
                ChunkSectionLayer.SOLID, List.of(solidB, solidA),
                ChunkSectionLayer.TRANSLUCENT, List.of(translucentA));

        assertTrue(SpaceRendererRenderGroupBridge.hasCompleteGroupCoverage(
                List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.TRANSLUCENT),
                vanilla, planned));
    }

    @Test
    void sameDrawCountWithDifferentIdentityFailsClosed() {
        Object vanillaDraw = new Object();
        Object differentPlannedDraw = new Object();
        var vanilla = Map.of(ChunkSectionLayer.SOLID, List.of(vanillaDraw));
        var planned = Map.of(ChunkSectionLayer.SOLID, List.of(differentPlannedDraw));

        assertFalse(SpaceRendererRenderGroupBridge.hasCompleteGroupCoverage(
                List.of(ChunkSectionLayer.SOLID), vanilla, planned));
    }

    @Test
    void duplicateDrawIdentityFailsClosed() {
        Object draw = new Object();
        var vanilla = Map.of(ChunkSectionLayer.SOLID, List.of(draw, draw));
        var planned = Map.of(ChunkSectionLayer.SOLID, List.of(draw, draw));

        assertFalse(SpaceRendererRenderGroupBridge.hasCompleteGroupCoverage(
                List.of(ChunkSectionLayer.SOLID), vanilla, planned));
    }

    @Test
    void plannedLayerOutsideNativeGroupFailsClosed() {
        Object draw = new Object();
        var vanilla = Map.of(ChunkSectionLayer.SOLID, List.of(draw));
        var planned = Map.of(
                ChunkSectionLayer.SOLID, List.of(draw),
                ChunkSectionLayer.TRANSLUCENT, List.of(new Object()));

        assertFalse(SpaceRendererRenderGroupBridge.hasCompleteGroupCoverage(
                List.of(ChunkSectionLayer.SOLID), vanilla, planned));
    }

    @Test
    void missingNativeLayerListFailsClosed() {
        var vanilla = Map.of(ChunkSectionLayer.SOLID, List.of());
        var planned = Map.<ChunkSectionLayer, List<Object>>of();

        assertFalse(SpaceRendererRenderGroupBridge.hasCompleteGroupCoverage(
                List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.TRANSLUCENT),
                vanilla, planned));
    }

}
