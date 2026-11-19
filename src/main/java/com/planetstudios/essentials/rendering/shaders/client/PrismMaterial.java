package com.planetstudios.essentials.rendering.shaders.client;

import java.util.Objects;

/**
 * A shader material declaration for cooperating renderer integrations.
 *
 * This describes which Prism program a modded geometry renderer should use.
 * It does not silently replace vanilla terrain or entity materials.
 */
public record PrismMaterial(String id, String programId, PrismFrameStage stage) {
    public PrismMaterial {
        requireResourceId(id, "material");
        requireResourceId(programId, "program");
        Objects.requireNonNull(stage, "stage");
    }

    private static void requireResourceId(String value, String kind) {
        if (value == null || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || value.contains("..") || value.contains("//")) {
            throw new IllegalArgumentException("Invalid Prism " + kind + " identifier: " + value);
        }
    }
}
