package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainSectionLifecycleTest {
    private static PrismTerrainVertexData payload(int x, int y, int z, String material) {
        var a = new PrismTerrainVertex(0,0,0,0,0,0xFFFFFFFF,0,0,0);
        var b = new PrismTerrainVertex(1,0,0,1,0,0xFFFFFFFF,0,0,0);
        var c = new PrismTerrainVertex(1,1,0,1,1,0xFFFFFFFF,0,0,0);
        var d = new PrismTerrainVertex(0,1,0,0,1,0xFFFFFFFF,0,0,0);
        return PrismTerrainVertexData.builder(x,y,z,material,PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(a,b,c,d).build();
    }

    @Test void newerRebuildPreventsOlderAsyncResultFromPublishing() {
        var cache = new PrismTerrainSectionCache(4096);
        var lifecycle = new PrismTerrainSectionLifecycle(cache);
        var oldBuild = lifecycle.beginRebuild(2,3,4);
        var currentBuild = lifecycle.beginRebuild(2,3,4);
        assertFalse(lifecycle.publish(oldBuild, payload(2,3,4,"example:old")));
        assertTrue(lifecycle.publish(currentBuild, payload(2,3,4,"example:new")));
        assertNull(cache.get(2,3,4,"example:old",PrismFrameStage.BEFORE_ENTITIES));
        assertNotNull(cache.get(2,3,4,"example:new",PrismFrameStage.BEFORE_ENTITIES));
    }

    @Test void unloadInvalidatesOutstandingBuildsAndAllCachedMaterials() {
        var cache = new PrismTerrainSectionCache(4096);
        var lifecycle = new PrismTerrainSectionLifecycle(cache);
        var token = lifecycle.beginRebuild(5,7,-2);
        assertTrue(lifecycle.publish(token, payload(5,7,-2,"example:stone")));
        assertTrue(lifecycle.publish(token, payload(5,7,-2,"example:dirt")));
        assertEquals(2, lifecycle.unloadChunk(5,7,9,-2));
        assertFalse(lifecycle.publish(token, payload(5,7,-2,"example:late")));
        assertEquals(0, cache.size());
    }

    @Test void rejectsMismatchedPayloadAndInvalidChunkRange() {
        var lifecycle = new PrismTerrainSectionLifecycle(new PrismTerrainSectionCache(4096));
        var token = lifecycle.beginRebuild(0,0,0);
        assertThrows(IllegalArgumentException.class, () -> lifecycle.publish(token, payload(1,0,0,"example:stone")));
        assertThrows(IllegalArgumentException.class, () -> lifecycle.unloadChunk(0,2,1,0));
    }

    @Test void explicitSectionInvalidationRejectsOldToken() {
        var cache = new PrismTerrainSectionCache(4096);
        var lifecycle = new PrismTerrainSectionLifecycle(cache);
        var token = lifecycle.beginRebuild(-1,4,8);
        assertEquals(0, lifecycle.invalidateSection(-1,4,8));
        assertFalse(lifecycle.publish(token, payload(-1,4,8,"example:stone")));
    }
}