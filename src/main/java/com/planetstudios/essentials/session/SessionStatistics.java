package com.planetstudios.essentials.session;

/**
 * Lightweight local-only statistics for the current multiplayer or single-player session.
 * Data is held in memory and is never transmitted or persisted.
 */
public final class SessionStatistics {
    private static final double MAX_CONTIGUOUS_STEP = 8.0;

    private boolean active;
    private long elapsedTicks;
    private double distanceTravelled;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean hasPosition;

    /** Starts a fresh session, discarding the previous session's counters. */
    public synchronized void start() {
        elapsedTicks = 0L;
        distanceTravelled = 0.0;
        hasPosition = false;
        active = true;
    }

    /** Stops collection while preserving the final snapshot until the next start. */
    public synchronized void stop() {
        active = false;
        hasPosition = false;
    }

    /**
     * Records one active client tick and the player's current position.
     * Large discontinuities (such as world teleports) are treated as a new position
     * baseline rather than movement, avoiding inflated distance totals.
     */
    public synchronized void recordPlayerTick(double x, double y, double z) {
        if (!active || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        elapsedTicks++;
        if (hasPosition) {
            double dx = x - lastX;
            double dy = y - lastY;
            double dz = z - lastZ;
            double stepSquared = dx * dx + dy * dy + dz * dz;
            if (Double.isFinite(stepSquared) && stepSquared <= MAX_CONTIGUOUS_STEP * MAX_CONTIGUOUS_STEP) {
                distanceTravelled += Math.sqrt(stepSquared);
            }
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        hasPosition = true;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(active, elapsedTicks, distanceTravelled);
    }

    public record Snapshot(boolean active, long elapsedTicks, double distanceTravelled) {
        public long elapsedSeconds() {
            return elapsedTicks / 20L;
        }
    }
}
