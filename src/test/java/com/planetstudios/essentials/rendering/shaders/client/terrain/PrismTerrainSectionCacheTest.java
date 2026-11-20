package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainSectionCacheTest {
    private static PrismTerrainVertexData payload(int x, String material) {
        var v0=new PrismTerrainVertex(0,0,0,0,0,0xFFFFFFFF,0,0,0);
        var v1=new PrismTerrainVertex(1,0,0,1,0,0xFFFFFFFF,0,0,0);
        var v2=new PrismTerrainVertex(1,1,0,1,1,0xFFFFFFFF,0,0,0);
        var v3=new PrismTerrainVertex(0,1,0,0,1,0xFFFFFFFF,0,0,0);
        return PrismTerrainVertexData.builder(x,0,0,material,PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(v0,v1,v2,v3).build();
    }

    @Test void storesAndInvalidatesAllSectionMaterials() {
        var cache=new PrismTerrainSectionCache(4096);
        var a=payload(4,"example:stone");
        var b=payload(4,"example:dirt");
        assertTrue(cache.put(a)); assertTrue(cache.put(b));
        assertEquals(2,cache.size());
        assertEquals(2,cache.invalidateSection(4,0,0));
        assertEquals(0,cache.size()); assertEquals(0,cache.currentBytes());
    }

    @Test void evictsLeastRecentlyUsedWithinBudget() {
        var sample=payload(0,"example:stone");
        var cache=new PrismTerrainSectionCache(sample.byteSize()*2L);
        assertTrue(cache.put(sample));
        assertTrue(cache.put(payload(1,"example:stone")));
        assertNotNull(cache.get(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES));
        assertTrue(cache.put(payload(2,"example:stone")));
        assertNull(cache.get(1,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES));
        assertNotNull(cache.get(0,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES));
        assertNotNull(cache.get(2,0,0,"example:stone",PrismFrameStage.BEFORE_ENTITIES));
        assertTrue(cache.currentBytes()<=cache.maxBytes());
    }

    @Test void rejectsPayloadLargerThanWholeBudgetAndSupportsReplacement() {
        var payload=payload(0,"example:stone");
        var cache=new PrismTerrainSectionCache(payload.byteSize()-1L);
        assertFalse(cache.put(payload));
        var roomy=new PrismTerrainSectionCache(payload.byteSize()*2L);
        assertTrue(roomy.put(payload)); assertTrue(roomy.put(payload));
        assertEquals(1,roomy.size()); assertEquals(payload.byteSize(),roomy.currentBytes());
    }

    @Test void clearReleasesAllCpuPayloadAccounting() {
        var cache=new PrismTerrainSectionCache(4096);
        assertTrue(cache.put(payload(0,"example:stone")));
        cache.clear();
        assertEquals(0,cache.size()); assertEquals(0,cache.currentBytes());
    }
}
