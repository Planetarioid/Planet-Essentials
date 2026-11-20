package com.planetstudios.essentials.rendering;

/** Camera-aware scoring for pending chunk rebuilds; look direction is published on the client tick. */
public final class ChunkSchedulingPolicy {
    /** Keep the per-poll scan bounded so prioritization cannot become an O(queue-size) hot path. */
    private static final int MAX_TASKS_PER_POLL = 32;

    public record Look(double x, double y, double z) {}
    private static volatile Look look = new Look(0.0, 0.0, 1.0);

    private ChunkSchedulingPolicy() {}

    public static void updateLook(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-9) return;
        look = new Look(x / length, y / length, z / length);
    }

    public static Look look() {
        return look;
    }

    /** Returns the maximum number of queue entries that may be inspected during one poll. */
    public static int maxTasksToInspect(int pendingTasks) {
        return Math.max(0, Math.min(pendingTasks, MAX_TASKS_PER_POLL));
    }

    /**
     * Distance remains the primary factor. A modest penalty moves chunks behind
     * the camera down the queue while nearby forward-facing chunks are prepared first.
     */
    public static double score(double dx, double dy, double dz, Look direction) {
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared < 1.0E-9) return 0.0;
        double dot = (dx * direction.x() + dy * direction.y() + dz * direction.z())
                / Math.sqrt(distanceSquared);
        double behindPenalty = Math.max(0.0, -dot) * 0.45;
        double forwardCredit = Math.max(0.0, dot) * Math.min(64.0, distanceSquared * 0.02);
        return Math.max(0.0, distanceSquared * (1.0 + behindPenalty) - forwardCredit);
    }
}
