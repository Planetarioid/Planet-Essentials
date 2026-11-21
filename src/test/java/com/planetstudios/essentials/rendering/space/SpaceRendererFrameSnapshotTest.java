package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererFrameSnapshotTest {
    @Test void emptyFrameIsNotEligibleForSubmission() {
        var snapshot = SpaceRendererFrameSnapshot.empty(1);
        assertFalse(snapshot.isComplete());
        assertFalse(snapshot.maySubmit());
        assertEquals(0, snapshot.requiredDraws());
    }

    @Test void rejectsDuplicateVanillaDrawIdentity() {
        Object draw = new Object();
        var section = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var input = new SpaceRendererFrameSnapshot.DrawInput(
                draw, section, "planet_essentials:terrain/vanilla/solid",
                com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage.BEFORE_ENTITIES,
                "solid", 0);
        assertThrows(IllegalArgumentException.class, () ->
                SpaceRendererFrameSnapshot.capture(1, List.of(input, input), ignored -> null));
    }

    @Test void missingGpuMeshesMakeTheWholeFrameIneligible() {
        Object draw = new Object();
        var section = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var input = new SpaceRendererFrameSnapshot.DrawInput(
                draw, section, "planet_essentials:terrain/vanilla/solid",
                com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage.BEFORE_ENTITIES,
                "solid", 0);
        var snapshot = SpaceRendererFrameSnapshot.capture(2, List.of(input), ignored -> null);
        assertFalse(snapshot.isComplete());
        assertEquals(1, snapshot.requiredDraws());
        assertEquals(1, snapshot.missingMeshes());
        assertTrue(snapshot.draws().isEmpty());
    }

    @Test void requiresPositiveFrameSequence() {
        assertThrows(IllegalArgumentException.class, () -> SpaceRendererFrameSnapshot.empty(0));
    }
}
