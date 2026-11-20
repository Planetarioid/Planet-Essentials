package com.planetstudios.essentials.rendering.shaders.client.terrain;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainVanillaDrawIndexTest {
    @Test void emptySnapshotIsNeverCompleteForNonEmptyDraws() {
        var snapshot = PrismTerrainVanillaDrawIndex.Snapshot.empty();
        assertTrue(snapshot.isCompleteFor(List.of()));
        assertFalse(snapshot.isCompleteFor(List.of(new Object())));
    }
    @Test void rejectsSnapshotWhoseCountDoesNotMatchIdentityEntries() {
        assertThrows(IllegalArgumentException.class, () ->
                new PrismTerrainVanillaDrawIndex.Snapshot(Map.of(new Object(),
                        new PrismTerrainVanillaDrawIndex.DrawDescriptor(0, 0, 0, null, 0)), 0));
    }
}
