package com.planetstudios.essentials.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComboCounterTest {
    @Test
    void outgoingHitsBuildPositiveStreak() {
        ComboCounter counter = new ComboCounter();

        assertEquals(1, counter.recordOutgoingHit());
        assertEquals(2, counter.recordOutgoingHit());
        assertEquals(3, counter.recordOutgoingHit());
        assertEquals(4, counter.recordOutgoingHit());
        assertEquals(5, counter.recordOutgoingHit());
    }

    @Test
    void firstIncomingHitBreaksPositiveStreakAndDisplaysMinusOne() {
        ComboCounter counter = new ComboCounter();
        for (int i = 0; i < 5; i++) counter.recordOutgoingHit();

        assertEquals(-1, counter.recordIncomingHit());
        assertEquals(-2, counter.recordIncomingHit());
        assertEquals(-3, counter.recordIncomingHit());
    }

    @Test
    void outgoingHitStartsNewPositiveStreakAfterNegativeSequence() {
        ComboCounter counter = new ComboCounter();
        counter.recordIncomingHit();
        counter.recordIncomingHit();

        assertEquals(1, counter.recordOutgoingHit());
        assertEquals(2, counter.recordOutgoingHit());
    }

    @Test
    void resetReturnsCounterToZero() {
        ComboCounter counter = new ComboCounter();
        counter.recordOutgoingHit();
        counter.reset();

        assertEquals(0, counter.value());
        assertEquals(1, counter.recordOutgoingHit());
    }
}
