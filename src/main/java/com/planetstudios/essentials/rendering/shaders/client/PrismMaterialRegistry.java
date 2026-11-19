package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for shader materials exposed to cooperating terrain/entity renderers.
 * Registrations are metadata only; the owning renderer remains responsible for
 * extracting geometry and issuing draws at the material's declared frame stage.
 */
public final class PrismMaterialRegistry {
    private static final Map<String, PrismMaterial> MATERIALS = new ConcurrentHashMap<>();

    private PrismMaterialRegistry() {}

    public static Registration register(PrismMaterial material) {
        Objects.requireNonNull(material, "material");
        PrismMaterial previous = MATERIALS.putIfAbsent(material.id(), material);
        if (previous != null) {
            throw new IllegalArgumentException("Prism material is already registered: " + material.id());
        }
        return () -> MATERIALS.remove(material.id(), material);
    }

    public static Optional<PrismMaterial> find(String materialId) {
        return Optional.ofNullable(MATERIALS.get(Objects.requireNonNull(materialId, "materialId")));
    }

    public static Map<String, PrismMaterial> snapshot() {
        return Map.copyOf(MATERIALS);
    }

    /**
     * Resolves a registered material to the active pack's compiled GPU pipeline.
     * Empty means the caller must use its normal/vanilla material fallback.
     */
    public static Optional<ResolvedMaterial> resolve(String materialId) {
        PrismMaterial material = MATERIALS.get(Objects.requireNonNull(materialId, "materialId"));
        if (material == null || !ShaderPackRuntime.isEnabled()) return Optional.empty();
        RenderPipeline pipeline = ShaderPackRuntime.renderPipeline(material.programId());
        ShaderBindingLayout bindings = ShaderPackRuntime.programBindings(material.programId());
        if (pipeline == null || bindings == null) return Optional.empty();
        return Optional.of(new ResolvedMaterial(material, pipeline, bindings));
    }

    public record ResolvedMaterial(
            PrismMaterial material, RenderPipeline pipeline, ShaderBindingLayout bindings) {
        public ResolvedMaterial {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(bindings, "bindings");
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}
