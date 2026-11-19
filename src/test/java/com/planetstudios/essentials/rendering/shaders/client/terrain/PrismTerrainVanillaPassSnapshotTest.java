package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalDouble;

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
    void indexBufferAndTypeMustBeCapturedTogether() {
        assertDoesNotThrow(() -> new PrismTerrainVanillaPassSnapshot.IndexBufferInput(
                Optional.empty(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () ->
                new PrismTerrainVanillaPassSnapshot.IndexBufferInput(Optional.empty(),
                        Optional.of(net.minecraft.client.renderer.chunk.ChunkSectionLayer.values().length == -1
                                ? null : com.mojang.blaze3d.vertex.VertexFormat.IndexType.SHORT))));
    }

    @Test
    void vanillaPassOptionsRepresentNoClearOrExplicitScissor() {
        var options = new PrismTerrainVanillaPassSnapshot.PassOptions(
                OptionalInt.empty(), OptionalDouble.empty(), false, false, false);
        assertTrue(options.colorClear().isEmpty());
        assertTrue(options.depthClear().isEmpty());
        assertFalse(options.stencilAttachment());
        assertFalse(options.explicitViewport());
        assertFalse(options.explicitScissor());
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
