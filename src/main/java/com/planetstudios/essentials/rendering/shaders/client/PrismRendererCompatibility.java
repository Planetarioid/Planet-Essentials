package com.planetstudios.essentials.rendering.shaders.client;

import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Conservative renderer detection for Prism.
 *
 * Detection is informational until a renderer-specific adapter explicitly
 * advertises a supported hook. A detected mod is never treated as proof that
 * Prism can replace that renderer's terrain or entity pipelines.
 */
public final class PrismRendererCompatibility {
    public enum Renderer {
        VANILLA,
        SODIUM,
        CANVAS,
        VULKANMOD,
        UNKNOWN
    }

    public record Snapshot(
            Renderer renderer,
            Set<String> detectedRendererMods,
            boolean irisPresent,
            String compatibilityNote) {
        public Snapshot {
            Objects.requireNonNull(renderer, "renderer");
            detectedRendererMods = Set.copyOf(detectedRendererMods);
            Objects.requireNonNull(compatibilityNote, "compatibilityNote");
        }

        public Optional<PrismRendererAdapter> adapter() {
            return PrismRendererAdapterRegistry.find(adapterId());
        }

        public boolean supportsAutomaticTerrainReplacement() {
            return adapter().map(a -> a.supportedFeatures().contains(
                    PrismRendererAdapter.Feature.CHUNK_TERRAIN_PIPELINE)).orElse(false);
        }

        public boolean supportsAutomaticEntityReplacement() {
            return adapter().map(a -> a.supportedFeatures().contains(
                    PrismRendererAdapter.Feature.ENTITY_PIPELINE)).orElse(false);
        }

        private String adapterId() {
            return switch (renderer) {
                case VANILLA -> "vanilla";
                case SODIUM -> "sodium";
                case CANVAS -> "canvas";
                case VULKANMOD -> "vulkanmod";
                case UNKNOWN -> "unknown";
            };
        }
    }

    private PrismRendererCompatibility() {}

    /** Detects installed renderer mods without linking against their classes. */
    public static Snapshot detectInstalled() {
        FabricLoader loader = FabricLoader.getInstance();
        Set<String> mods = new LinkedHashSet<>();
        for (String id : Set.of("sodium", "canvas", "vulkanmod", "optifabric", "indium")) {
            if (loader.isModLoaded(id)) mods.add(id);
        }
        return detect(mods, loader.isModLoaded("iris"));
    }

    /** Pure resolver kept separate so renderer precedence can be unit-tested. */
    public static Snapshot detect(Set<String> installedMods, boolean irisPresent) {
        Objects.requireNonNull(installedMods, "installedMods");
        Set<String> normalized = new LinkedHashSet<>();
        for (String id : installedMods) {
            if (id != null && !id.isBlank()) normalized.add(id.toLowerCase(java.util.Locale.ROOT));
        }

        Renderer renderer;
        if (normalized.contains("vulkanmod")) {
            renderer = Renderer.VULKANMOD;
        } else if (normalized.contains("canvas")) {
            renderer = Renderer.CANVAS;
        } else if (normalized.contains("sodium")) {
            renderer = Renderer.SODIUM;
        } else if (normalized.contains("optifabric")) {
            renderer = Renderer.UNKNOWN;
        } else {
            renderer = Renderer.VANILLA;
        }

        String note;
        if (renderer == Renderer.VANILLA && !irisPresent) {
            note = "Vanilla renderer detected. Prism world post-processing is available; automatic terrain/entity replacement requires an explicit adapter.";
        } else if (irisPresent) {
            note = "Iris detected. Prism does not inject into Iris or Sodium internals; renderer-specific shader coexistence remains unverified.";
        } else {
            note = "A non-vanilla renderer was detected. Prism uses conservative integration and does not replace its terrain/entity pipelines without a registered adapter.";
        }

        return new Snapshot(renderer, normalized, irisPresent, note);
    }
}
