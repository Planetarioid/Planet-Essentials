package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureFallbackTest {
    @Test
    void preservesSuccessfulOptimizedResult() {
        assertEquals("planet_essentials:decoded/example",
                TextureFallback.orElse(
                        CompletableFuture.completedFuture("planet_essentials:decoded/example"),
                        "minecraft:block/stone").join());
    }

    @Test
    void returnsVanillaValueWhenOptimizedPathFails() {
        assertEquals("minecraft:block/stone",
                TextureFallback.orElse(
                        CompletableFuture.failedFuture(new IllegalStateException("upload failed")),
                        "minecraft:block/stone").join());
    }

    @Test
    void returnsVanillaValueWhenOptimizedPathProducesNull() {
        assertEquals("minecraft:block/stone",
                TextureFallback.orElse(
                        CompletableFuture.completedFuture(null),
                        "minecraft:block/stone").join());
    }

    @Test
    void handlesMissingAttemptFuture() {
        assertEquals("minecraft:block/stone",
                TextureFallback.orElse(null, "minecraft:block/stone").join());
    }
}
