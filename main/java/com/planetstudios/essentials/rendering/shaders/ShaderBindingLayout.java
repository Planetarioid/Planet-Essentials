package com.planetstudios.essentials.rendering.shaders;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable, validated resource-binding declarations for a shader program. */
public record ShaderBindingLayout(
        List<String> uniformBuffers,
        List<String> texelBuffers,
        List<String> samplers) {
    public static final int MAX_BINDINGS_PER_KIND = 16;
    private static final String GLSL_IDENTIFIER = "[A-Za-z_][A-Za-z0-9_]{0,63}";
    public static final ShaderBindingLayout EMPTY = new ShaderBindingLayout(List.of(), List.of(), List.of());

    public ShaderBindingLayout {
        uniformBuffers = validatedNames(uniformBuffers, "uniform buffer");
        texelBuffers = validatedNames(texelBuffers, "texel buffer");
        samplers = validatedNames(samplers, "sampler");
        Set<String> all = new HashSet<>();
        for (String name : uniformBuffers) {
            if (!all.add(name)) throw new IllegalArgumentException("Duplicate shader binding: " + name);
        }
        for (String name : texelBuffers) {
            if (!all.add(name)) throw new IllegalArgumentException("Duplicate shader binding: " + name);
        }
        for (String name : samplers) {
            if (!all.add(name)) throw new IllegalArgumentException("Duplicate shader binding: " + name);
        }
    }

    private static List<String> validatedNames(List<String> names, String kind) {
        Objects.requireNonNull(names, kind + "s");
        if (names.size() > MAX_BINDINGS_PER_KIND) {
            throw new IllegalArgumentException("Too many " + kind + " declarations");
        }
        List<String> copy = List.copyOf(names);
        for (String name : copy) {
            if (name == null || !name.matches(GLSL_IDENTIFIER)) {
                throw new IllegalArgumentException("Invalid " + kind + " name: " + name);
            }
        }
        return copy;
    }

    public String canonicalForm() {
        return "ubo=" + String.join(",", uniformBuffers)
                + ";texel=" + String.join(",", texelBuffers)
                + ";sampler=" + String.join(",", samplers);
    }
}
