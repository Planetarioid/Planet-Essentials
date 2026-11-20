package com.planetstudios.essentials.combat;

/**
 * Tracks the local player's consecutive hit streak.
 *
 * Positive values count consecutive confirmed hits landed by the local player.
 * Negative values count consecutive confirmed hits received by the local player.
 * The first incoming hit always switches the display to -1, breaking a positive streak.
 */
public final class ComboCounter {
    private int value;

    public int value() {
        return value;
    }

    /** Records a confirmed hit landed by the local player. */
    public int recordOutgoingHit() {
        value = value < 0 ? 1 : incrementSaturated(value);
        return value;
    }

    /** Records a confirmed hit received by the local player. */
    public int recordIncomingHit() {
        value = value > 0 ? -1 : decrementSaturated(value);
        return value;
    }

    /** Clears the streak at an explicitly chosen combat/session boundary. */
    public void reset() {
        value = 0;
    }

    private static int incrementSaturated(int current) {
        return current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1;
    }

    private static int decrementSaturated(int current) {
        return current == Integer.MIN_VALUE ? Integer.MIN_VALUE : current - 1;
    }
}
