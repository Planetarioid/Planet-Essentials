package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererVisibilityGraphTest {
    private static SpaceRendererSectionManager.SectionKey key(int x, int y, int z) {
        return new SpaceRendererSectionManager.SectionKey("minecraft:overworld", x, y, z);
    }

    @Test
    void traversesOnlyMutuallyOpenSectionFaces() {
        var graph = new SpaceRendererVisibilityGraph();
        graph.update(key(0, 0, 0), SpaceRendererVisibilityGraph.POSITIVE_X);
        graph.update(key(1, 0, 0), SpaceRendererVisibilityGraph.NEGATIVE_X);
        graph.update(key(2, 0, 0), SpaceRendererVisibilityGraph.ALL_FACES);

        var result = graph.visibleFrom(key(0, 0, 0), 32);

        assertTrue(result.visibleSections().contains(key(0, 0, 0)));
        assertTrue(result.visibleSections().contains(key(1, 0, 0)));
        assertFalse(result.visibleSections().contains(key(2, 0, 0)));
        assertTrue(result.isComplete());
    }

    @Test
    void missingNeighborMarksResultForSafeFallback() {
        var graph = new SpaceRendererVisibilityGraph();
        graph.update(key(0, 0, 0), SpaceRendererVisibilityGraph.ALL_FACES);

        var result = graph.visibleFrom(key(0, 0, 0), 32);

        assertEquals(1, result.visitedSectionCount());
        assertTrue(result.requiresFrustumFallback());
    }

    @Test
    void unknownOriginFailsOpen() {
        var graph = new SpaceRendererVisibilityGraph();
        var result = graph.visibleFrom(key(0, 0, 0), 32);

        assertTrue(result.visibleSections().isEmpty());
        assertTrue(result.requiresFrustumFallback());
    }

    @Test
    void visitLimitMarksTruncatedTraversalIncomplete() {
        var graph = new SpaceRendererVisibilityGraph();
        graph.update(key(0, 0, 0), SpaceRendererVisibilityGraph.POSITIVE_X);
        graph.update(key(1, 0, 0), SpaceRendererVisibilityGraph.NEGATIVE_X | SpaceRendererVisibilityGraph.POSITIVE_X);
        graph.update(key(2, 0, 0), SpaceRendererVisibilityGraph.NEGATIVE_X);

        var result = graph.visibleFrom(key(0, 0, 0), 1);

        assertEquals(1, result.visitedSectionCount());
        assertTrue(result.requiresFrustumFallback());
    }

    @Test
    void dimensionUnloadRemovesOnlyMatchingGraphNodes() {
        var graph = new SpaceRendererVisibilityGraph();
        graph.update(key(0, 0, 0), SpaceRendererVisibilityGraph.ALL_FACES);
        graph.update(new SpaceRendererSectionManager.SectionKey("minecraft:the_nether", 0, 0, 0),
                SpaceRendererVisibilityGraph.ALL_FACES);

        assertEquals(1, graph.removeDimension("minecraft:overworld"));
        assertEquals(1, graph.trackedSectionCount());
    }

    @Test
    void rejectsUnknownFaceBitsAndInvalidVisitLimits() {
        var graph = new SpaceRendererVisibilityGraph();
        assertThrows(IllegalArgumentException.class, () -> graph.update(key(0, 0, 0), 1 << 7));
        assertThrows(IllegalArgumentException.class, () -> graph.visibleFrom(key(0, 0, 0), 0));
    }
}
