package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrismTerrainVanillaPassSnapshotTest {
    @Test
    void translucentDrawsAreReversedExactlyOnce() {
        List<String> source = List.of("near", "middle", "far");
        assertEquals(List.of("far", "middle", "near"),
                PrismTerrainVanillaPassSnapshot.effectiveDrawOrder(ChunkSectionLayer.TRANSLUCENT, source));
    }

    @Test
    void opaqueAndCutoutDrawsKeepVanillaOrder() {
        List<String> source = List.of("first", "second", "third");
        assertEquals(source, PrismTerrainVanillaPassSnapshot.effectiveDrawOrder(ChunkSectionLayer.SOLID, source));
        assertEquals(source, PrismTerrainVanillaPassSnapshot.effectiveDrawOrder(ChunkSectionLayer.CUTOUT, source));
        assertEquals(source, PrismTerrainVanillaPassSnapshot.effectiveDrawOrder(ChunkSectionLayer.TRIPWIRE, source));
    }

    @Test
    void capturedDrawOrderIsAnImmutableCopy() {
        ArrayList<String> source = new ArrayList<>(List.of("a", "b"));
        List<?> captured = PrismTerrainVanillaPassSnapshot.effectiveDrawOrder(ChunkSectionLayer.SOLID, source);
        source.clear();
        assertEquals(List.of("a", "b"), captured);
        assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) captured).add("c"));
    }
}
