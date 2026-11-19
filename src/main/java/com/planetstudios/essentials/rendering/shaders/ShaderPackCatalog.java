package com.planetstudios.essentials.rendering.shaders;

import java.util.List;
import java.util.Objects;

/** Atomic immutable view of the currently discovered shader packs and diagnostics. */
public final class ShaderPackCatalog {
    public record Snapshot(long generation, List<ShaderPackLoader.LoadedPack> packs, List<String> errors) {
        public Snapshot {
            packs = List.copyOf(packs);
            errors = List.copyOf(errors);
        }
    }

    private long generation;
    private Snapshot snapshot = new Snapshot(0, List.of(), List.of());

    public synchronized Snapshot snapshot() {
        return snapshot;
    }

    public synchronized Snapshot replace(
            List<ShaderPackLoader.LoadedPack> packs, List<String> errors) {
        Objects.requireNonNull(packs, "packs");
        Objects.requireNonNull(errors, "errors");
        snapshot = new Snapshot(++generation, packs, errors);
        return snapshot;
    }

    public synchronized void clear() {
        snapshot = new Snapshot(++generation, List.of(), List.of());
    }
}
