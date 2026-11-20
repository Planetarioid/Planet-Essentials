package com.planetstudios.essentials.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionStatisticsTest {
    @Test
    void countsActiveTicksAndContinuousMovement() {
        SessionStatistics stats = new SessionStatistics();
        stats.start();
        stats.recordPlayerTick(0, 64, 0);
        stats.recordPlayerTick(1, 64, 0);
        stats.recordPlayerTick(1, 64, 1);

        var snapshot = stats.snapshot();
        assertTrue(snapshot.active());
        assertEquals(3, snapshot.elapsedTicks());
        assertEquals(2.0, snapshot.distanceTravelled(), 0.0001);
    }

    @Test
    void ignoresLargePositionDiscontinuitiesButContinuesTracking() {
        SessionStatistics stats = new SessionStatistics();
        stats.start();
        stats.recordPlayerTick(0, 64, 0);
        stats.recordPlayerTick(1000, 80, 1000);
        stats.recordPlayerTick(1001, 80, 1000);

        assertEquals(3, stats.snapshot().elapsedTicks());
        assertEquals(1.0, stats.snapshot().distanceTravelled(), 0.0001);
    }

    @Test
    void stopPreservesSnapshotAndPreventsFurtherCollection() {
        SessionStatistics stats = new SessionStatistics();
        stats.start();
        stats.recordPlayerTick(0, 0, 0);
        stats.stop();
        stats.recordPlayerTick(1, 0, 0);

        assertFalse(stats.snapshot().active());
        assertEquals(1, stats.snapshot().elapsedTicks());
        assertEquals(0.0, stats.snapshot().distanceTravelled(), 0.0001);
    }

    @Test
    void restartResetsPreviousSession() {
        SessionStatistics stats = new SessionStatistics();
        stats.start();
        stats.recordPlayerTick(0, 0, 0);
        stats.recordPlayerTick(1, 0, 0);
        stats.start();

        assertEquals(0, stats.snapshot().elapsedTicks());
        assertEquals(0.0, stats.snapshot().distanceTravelled(), 0.0001);
    }

    @Test
    void rejectsNonFiniteCoordinatesWithoutCountingTick() {
        SessionStatistics stats = new SessionStatistics();
        stats.start();
        stats.recordPlayerTick(Double.NaN, 0, 0);

        assertEquals(0, stats.snapshot().elapsedTicks());
    }
}
