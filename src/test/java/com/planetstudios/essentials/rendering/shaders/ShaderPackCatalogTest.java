package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ShaderPackCatalogTest {
    @Test
    void replacesSnapshotsImmutablyAndAdvancesGeneration() {
        ShaderPackCatalog catalog = new ShaderPackCatalog();
        var first = catalog.replace(List.of(), List.of("warning"));
        var second = catalog.replace(List.of(), List.of());
        assertEquals(1, first.generation());
        assertEquals(2, second.generation());
        assertEquals(1, first.errors().size());
        assertThrows(UnsupportedOperationException.class, () -> second.packs().add(null));
        catalog.clear();
        assertEquals(3, catalog.snapshot().generation());
        assertTrue(catalog.snapshot().packs().isEmpty());
    }
}
