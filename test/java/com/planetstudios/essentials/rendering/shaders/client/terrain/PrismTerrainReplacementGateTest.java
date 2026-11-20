package com.planetstudios.essentials.rendering.shaders.client.terrain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainReplacementGateTest {
    private static PrismTerrainReplacementGate.Readiness ready(
            int required, int available, boolean program, boolean device, boolean frame) {
        return new PrismTerrainReplacementGate.Readiness(required, available, program, device, frame);
    }

    @Test
    void permitsReplacementOnlyWhenEveryDrawIsReady() {
        var value = ready(12, 12, true, true, true);
        assertTrue(PrismTerrainReplacementGate.mayReplace(value));
        assertEquals(PrismTerrainReplacementGate.FallbackReason.NONE,
                PrismTerrainReplacementGate.evaluate(value));
    }

    @Test
    void incompleteCoverageFallsBack() {
        assertEquals(PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE,
                PrismTerrainReplacementGate.evaluate(ready(12, 11, true, true, true)));
    }

    @Test
    void unavailableProgramDeviceOrFrameFallsBack() {
        assertEquals(PrismTerrainReplacementGate.FallbackReason.PROGRAM_UNAVAILABLE,
                PrismTerrainReplacementGate.evaluate(ready(1, 1, false, true, true)));
        assertEquals(PrismTerrainReplacementGate.FallbackReason.INCOMPATIBLE_DEVICE,
                PrismTerrainReplacementGate.evaluate(ready(1, 1, true, false, true)));
        assertEquals(PrismTerrainReplacementGate.FallbackReason.FRAME_NOT_READY,
                PrismTerrainReplacementGate.evaluate(ready(1, 1, true, true, false)));
    }

    @Test
    void emptyGroupsAreNotClaimedAsReplaced() {
        assertEquals(PrismTerrainReplacementGate.FallbackReason.EMPTY_GROUP,
                PrismTerrainReplacementGate.evaluate(ready(0, 0, true, true, true)));
    }

    @Test
    void rejectsImpossibleCoverage() {
        assertThrows(IllegalArgumentException.class, () -> ready(2, 3, true, true, true));
        assertThrows(IllegalArgumentException.class, () -> ready(-1, 0, true, true, true));
    }
}
