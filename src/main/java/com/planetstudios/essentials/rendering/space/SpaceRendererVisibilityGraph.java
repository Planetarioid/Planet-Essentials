package com.planetstudios.essentials.rendering.space;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Conservative section-connectivity traversal for terrain visibility.
 *
 * <p>Each section stores the faces through which its interior is connected.
 * Traversal crosses a boundary only when both sections report an opening.
 * Missing connectivity data marks the result incomplete so callers can fall
 * back to frustum-only visibility rather than accidentally hiding terrain.</p>
 */
public final class SpaceRendererVisibilityGraph {
    public static final int NEGATIVE_X = 1;
    public static final int POSITIVE_X = 1 << 1;
    public static final int NEGATIVE_Y = 1 << 2;
    public static final int POSITIVE_Y = 1 << 3;
    public static final int NEGATIVE_Z = 1 << 4;
    public static final int POSITIVE_Z = 1 << 5;
    public static final int ALL_FACES = (1 << 6) - 1;

    private static final Direction[] DIRECTIONS = {
            new Direction(-1, 0, 0, NEGATIVE_X, POSITIVE_X),
            new Direction(1, 0, 0, POSITIVE_X, NEGATIVE_X),
            new Direction(0, -1, 0, NEGATIVE_Y, POSITIVE_Y),
            new Direction(0, 1, 0, POSITIVE_Y, NEGATIVE_Y),
            new Direction(0, 0, -1, NEGATIVE_Z, POSITIVE_Z),
            new Direction(0, 0, 1, POSITIVE_Z, NEGATIVE_Z)
    };

    private final Map<SpaceRendererSectionManager.SectionKey, Integer> connectivity = new HashMap<>();

    public synchronized void update(SpaceRendererSectionManager.SectionKey section, int openFaces) {
        Objects.requireNonNull(section, "section");
        if ((openFaces & ~ALL_FACES) != 0) {
            throw new IllegalArgumentException("Connectivity mask contains unknown face bits");
        }
        connectivity.put(section, openFaces);
    }

    public synchronized boolean remove(SpaceRendererSectionManager.SectionKey section) {
        return connectivity.remove(Objects.requireNonNull(section, "section")) != null;
    }

    public synchronized int removeDimension(String dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        int removed = 0;
        var iterator = connectivity.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().dimensionId().equals(dimensionId)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Traverses connected section interiors. If any reachable open face leads
     * to an unknown section, the result is marked incomplete and must not be
     * used as an occlusion-only visibility answer.
     */
    public synchronized VisibilityResult visibleFrom(
            SpaceRendererSectionManager.SectionKey origin, int maximumVisited) {
        Objects.requireNonNull(origin, "origin");
        if (maximumVisited <= 0) throw new IllegalArgumentException("Visit limit must be positive");
        Integer originMask = connectivity.get(origin);
        if (originMask == null) return new VisibilityResult(Set.of(), true, 0);

        Set<SpaceRendererSectionManager.SectionKey> visible = new HashSet<>();
        ArrayDeque<SpaceRendererSectionManager.SectionKey> queue = new ArrayDeque<>();
        visible.add(origin);
        queue.add(origin);
        boolean incomplete = false;

        while (!queue.isEmpty()) {
            if (visible.size() >= maximumVisited) {
                incomplete = !queue.isEmpty();
                break;
            }
            var current = queue.removeFirst();
            int currentMask = connectivity.get(current);
            for (Direction direction : DIRECTIONS) {
                if ((currentMask & direction.faceBit()) == 0) continue;
                var neighbor = new SpaceRendererSectionManager.SectionKey(
                        current.dimensionId(),
                        current.x() + direction.dx(),
                        current.y() + direction.dy(),
                        current.z() + direction.dz());
                Integer neighborMask = connectivity.get(neighbor);
                if (neighborMask == null) {
                    incomplete = true;
                    continue;
                }
                if ((neighborMask & direction.oppositeBit()) == 0) continue;
                if (visible.add(neighbor)) queue.addLast(neighbor);
            }
        }
        return new VisibilityResult(Set.copyOf(visible), incomplete, visible.size());
    }

    public synchronized int trackedSectionCount() {
        return connectivity.size();
    }

    public record VisibilityResult(Set<SpaceRendererSectionManager.SectionKey> visibleSections,
            boolean requiresFrustumFallback, int visitedSectionCount) {
        public VisibilityResult {
            visibleSections = Set.copyOf(Objects.requireNonNull(visibleSections, "visibleSections"));
            if (visitedSectionCount < 0 || visitedSectionCount != visibleSections.size()) {
                throw new IllegalArgumentException("Visited count must match the visible section set");
            }
        }

        public boolean isComplete() {
            return !requiresFrustumFallback;
        }
    }

    private record Direction(int dx, int dy, int dz, int faceBit, int oppositeBit) {}
}
