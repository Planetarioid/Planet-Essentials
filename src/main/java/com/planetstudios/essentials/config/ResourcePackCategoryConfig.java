package com.planetstudios.essentials.config;

/** Per-pack category filters used by Space Client's advanced resource-pack mode. */
public final class ResourcePackCategoryConfig {
    public boolean textures = true;
    public boolean sounds = true;
    public boolean animations = true;

    public ResourcePackCategoryConfig() {}

    public boolean allEnabled() {
        return textures && sounds && animations;
    }

    public void validate() {
        // Boolean fields need no normalization; this method provides a stable validation boundary.
    }
}
