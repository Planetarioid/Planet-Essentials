package com.planetstudios.essentials.textures;

import java.util.concurrent.CompletableFuture;

/** Small, testable failure boundary for opt-in texture processing. */
public final class TextureFallback {
    private TextureFallback() {}

    /**
     * Returns the processed value on success, or the supplied vanilla value when
     * the asynchronous opt-in path fails or produces no value.
     */
    public static <T> CompletableFuture<T> orElse(
            CompletableFuture<T> attempt, T vanillaValue) {
        if (attempt == null) {
            return CompletableFuture.completedFuture(vanillaValue);
        }
        return attempt.handle((value, failure) ->
                failure == null && value != null ? value : vanillaValue);
    }
}
