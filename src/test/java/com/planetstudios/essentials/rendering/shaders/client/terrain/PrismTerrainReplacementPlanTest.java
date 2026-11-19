package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainReplacementPlanTest {
    private static PrismTerrainVanillaDrawIndex.DrawDescriptor descriptor(int x, ChunkSectionLayer layer, int ordinal) {
        return new PrismTerrainVanillaDrawIndex.DrawDescriptor(x, 0, 0, layer, ordinal);
    }
    private static PrismTerrainVanillaDrawIndex.Snapshot index(List<?> draws,
            List<PrismTerrainVanillaDrawIndex.DrawDescriptor> descriptors) {
        IdentityHashMap<Object, PrismTerrainVanillaDrawIndex.DrawDescriptor> map = new IdentityHashMap<>();
        for (int i = 0; i < draws.size(); i++) map.put(draws.get(i), descriptors.get(i));
        return new PrismTerrainVanillaDrawIndex.Snapshot(map, draws.size());
    }

    @Test void createsCompleteOrderedPlanOnlyWhenEveryDrawIsReady() {
        Object a = new Object(), b = new Object();
        List<Object> draws = List.of(a, b);
        var snapshot = index(draws, List.of(descriptor(1, ChunkSectionLayer.SOLID, 0),
                descriptor(2, ChunkSectionLayer.SOLID, 1)));
        var plan = PrismTerrainReplacementPlan.build(draws, snapshot, ChunkSectionLayer.SOLID,
                ignored -> true, true, true, true, false);
        assertTrue(plan.mayReplace());
        assertEquals(List.of(a, b), plan.entries().stream().map(PrismTerrainReplacementPlan.Entry::vanillaDraw).toList());
        assertEquals(2, plan.readyDraws());
    }

    @Test void preservesReverseLayerOrderWhenRequested() {
        Object a = new Object(), b = new Object();
        List<Object> draws = List.of(a, b);
        var snapshot = index(draws, List.of(descriptor(1, ChunkSectionLayer.TRANSLUCENT, 0),
                descriptor(2, ChunkSectionLayer.TRANSLUCENT, 1)));
        var plan = PrismTerrainReplacementPlan.build(draws, snapshot, ChunkSectionLayer.TRANSLUCENT,
                ignored -> true, true, true, true, true);
        assertEquals(List.of(b, a), plan.entries().stream().map(PrismTerrainReplacementPlan.Entry::vanillaDraw).toList());
    }

    @Test void oneMissingPayloadOrGpuMeshForcesWholeGroupFallback() {
        Object a = new Object(), b = new Object();
        List<Object> draws = List.of(a, b);
        var snapshot = index(draws, List.of(descriptor(1, ChunkSectionLayer.SOLID, 0),
                descriptor(2, ChunkSectionLayer.SOLID, 1)));
        var plan = PrismTerrainReplacementPlan.build(draws, snapshot, ChunkSectionLayer.SOLID,
                d -> d.sectionX() == 1, true, true, true, false);
        assertFalse(plan.mayReplace());
        assertEquals(PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE, plan.fallbackReason());
        assertTrue(plan.entries().isEmpty());
    }

    @Test void rejectsWrongLayerAndMissingDrawIndexEntries() {
        Object draw = new Object();
        List<Object> draws = List.of(draw);
        var wrong = PrismTerrainReplacementPlan.build(draws,
                index(draws, List.of(descriptor(1, ChunkSectionLayer.CUTOUT, 0))),
                ChunkSectionLayer.SOLID, ignored -> true, true, true, true, false);
        assertFalse(wrong.mayReplace());
        var missing = PrismTerrainReplacementPlan.build(draws,
                new PrismTerrainVanillaDrawIndex.Snapshot(Map.of(), 0),
                ChunkSectionLayer.SOLID, ignored -> true, true, true, true, false);
        assertEquals(PrismTerrainReplacementGate.FallbackReason.INCOMPLETE_COVERAGE, missing.fallbackReason());
    }

    @Test void unavailableProgramDeviceOrFrameNeverProducesPartialPlan() {
        Object draw = new Object();
        List<Object> draws = List.of(draw);
        var snapshot = index(draws, List.of(descriptor(1, ChunkSectionLayer.SOLID, 0)));
        assertEquals(PrismTerrainReplacementGate.FallbackReason.PROGRAM_UNAVAILABLE,
                PrismTerrainReplacementPlan.build(draws, snapshot, ChunkSectionLayer.SOLID,
                        ignored -> true, false, true, true, false).fallbackReason());
        assertEquals(PrismTerrainReplacementGate.FallbackReason.INCOMPATIBLE_DEVICE,
                PrismTerrainReplacementPlan.build(draws, snapshot, ChunkSectionLayer.SOLID,
                        ignored -> true, true, false, true, false).fallbackReason());
        assertEquals(PrismTerrainReplacementGate.FallbackReason.FRAME_NOT_READY,
                PrismTerrainReplacementPlan.build(draws, snapshot, ChunkSectionLayer.SOLID,
                        ignored -> true, true, true, false, false).fallbackReason());
    }

    @Test void emptyGroupIsNeverClaimedAsReplaced() {
        var plan = PrismTerrainReplacementPlan.build(List.of(), PrismTerrainVanillaDrawIndex.Snapshot.empty(),
                ChunkSectionLayer.SOLID, ignored -> true, true, true, true, false);
        assertEquals(PrismTerrainReplacementGate.FallbackReason.EMPTY_GROUP, plan.fallbackReason());
        assertFalse(plan.mayReplace());
    }
}
