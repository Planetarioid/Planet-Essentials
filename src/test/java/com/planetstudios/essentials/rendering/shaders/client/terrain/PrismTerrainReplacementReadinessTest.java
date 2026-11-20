package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainReplacementReadinessTest {
    @Test void solidAndCutoutUseOpaqueTerrainStage() {
        assertEquals(PrismFrameStage.BEFORE_ENTITIES,
                PrismTerrainReplacementReadiness.stageFor(ChunkSectionLayer.SOLID));
        assertEquals(PrismFrameStage.BEFORE_ENTITIES,
                PrismTerrainReplacementReadiness.stageFor(ChunkSectionLayer.CUTOUT));
    }

    @Test void translucentAndTripwireUseLateTerrainStage() {
        assertEquals(PrismFrameStage.BEFORE_TRANSLUCENT,
                PrismTerrainReplacementReadiness.stageFor(ChunkSectionLayer.TRANSLUCENT));
        assertEquals(PrismFrameStage.BEFORE_TRANSLUCENT,
                PrismTerrainReplacementReadiness.stageFor(ChunkSectionLayer.TRIPWIRE));
    }

    @Test void materialIdsMatchVanillaCaptureNaming() {
        assertEquals("planet_essentials:terrain/vanilla/solid",
                PrismTerrainReplacementReadiness.materialFor(ChunkSectionLayer.SOLID));
        assertEquals("planet_essentials:terrain/vanilla/translucent",
                PrismTerrainReplacementReadiness.materialFor(ChunkSectionLayer.TRANSLUCENT));
    }
}
