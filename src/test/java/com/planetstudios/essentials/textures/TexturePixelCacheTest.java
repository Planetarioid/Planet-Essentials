package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TexturePixelCacheTest {
    private static DecodedTexture texture(int width, int height) {
        return new DecodedTexture(width, height, new int[width * height]);
    }

    @Test
    void evictsLeastRecentlyUsedEntriesToRespectByteBudget() {
        TexturePixelCache cache = new TexturePixelCache(32);
        cache.put("a", texture(2, 2));
        cache.put("b", texture(2, 2));
        cache.get("a");
        cache.put("c", texture(2, 2));
        assertTrue(cache.get("a").isPresent());
        assertFalse(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());
        assertEquals(32, cache.retainedBytes());
    }

    @Test
    void oversizedTextureIsNotCached() {
        TexturePixelCache cache = new TexturePixelCache(16);
        cache.put("large", texture(3, 3));
        assertTrue(cache.get("large").isEmpty());
        assertEquals(0, cache.retainedBytes());
    }

    @Test
    void reloadInvalidatesEntriesAndAdvancesGeneration() {
        TexturePixelCache cache = new TexturePixelCache(64);
        cache.put("stone", texture(2, 2));
        assertEquals(1, cache.invalidateForReload());
        assertEquals(0, cache.size());
        assertEquals(0, cache.retainedBytes());
    }
}
