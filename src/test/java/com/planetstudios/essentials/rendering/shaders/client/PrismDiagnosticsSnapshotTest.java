package com.planetstudios.essentials.rendering.shaders.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrismDiagnosticsSnapshotTest {
    @Test
    void snapshotDefensivelyCopiesCollections() {
        var errors = new ArrayList<>(List.of("bad manifest"));
        var hashes = new HashMap<>(Map.of("demo:blur", "abc123"));
        var renderer = PrismRendererCompatibility.detect(java.util.Set.of(), false);
        var snapshot = new PrismDiagnosticsSnapshot(
                false, "", "", 1, 0, errors, 2, "test", 1,
                hashes, "", renderer, 0);

        errors.add("later error");
        hashes.put("demo:other", "def456");

        assertEquals(List.of("bad manifest"), snapshot.discoveryErrors());
        assertEquals(Map.of("demo:blur", "abc123"), snapshot.programHashes());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.discoveryErrors().add("mutation"));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.programHashes().put("x:y", "z"));
    }

    @Test
    void rejectsNegativeCountsAndGenerations() {
        var renderer = PrismRendererCompatibility.detect(java.util.Set.of(), false);
        assertThrows(IllegalArgumentException.class, () -> new PrismDiagnosticsSnapshot(
                false, "", "", -1, 0, List.of(), 0, "test", 0,
                Map.of(), "", renderer, 0));
    }
}
