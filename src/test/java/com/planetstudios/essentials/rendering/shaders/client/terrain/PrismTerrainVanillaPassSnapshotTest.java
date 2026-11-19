package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

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
    void sequentialIndexRequirementsAreCapturedWithoutAnAllocatedBuffer() {
        var empty = new PrismTerrainVanillaPassSnapshot.IndexBufferInput(0, Optional.empty());
        assertEquals(0, empty.maximumIndexCount());
        assertTrue(empty.type().isEmpty());

        var required = new PrismTerrainVanillaPassSnapshot.IndexBufferInput(
                384, Optional.of(com.mojang.blaze3d.vertex.VertexFormat.IndexType.INT));
        assertEquals(384, required.maximumIndexCount());
        assertEquals(com.mojang.blaze3d.vertex.VertexFormat.IndexType.INT, required.type().orElseThrow());

        assertThrows(IllegalArgumentException.class, () ->
                new PrismTerrainVanillaPassSnapshot.IndexBufferInput(0,
                        Optional.of(com.mojang.blaze3d.vertex.VertexFormat.IndexType.SHORT)));
        assertThrows(IllegalArgumentException.class, () ->
                new PrismTerrainVanillaPassSnapshot.IndexBufferInput(12, Optional.empty()));
    }

    @Test
    void vanillaPassOptionsRepresentNoClearOrExplicitScissor() {
        var options = new PrismTerrainVanillaPassSnapshot.PassOptions(
                OptionalInt.empty(), OptionalDouble.empty(), false, Optional.empty(), Optional.empty());
        assertTrue(options.colorClear().isEmpty());
        assertTrue(options.depthClear().isEmpty());
        assertFalse(options.stencilAttachment());
        assertTrue(options.viewport().isEmpty());
        assertTrue(options.scissor().isEmpty());
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
