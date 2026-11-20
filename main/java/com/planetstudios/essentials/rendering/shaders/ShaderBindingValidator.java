package com.planetstudios.essentials.rendering.shaders;

import java.util.Set;

/** Validates that a draw supplies exactly the resources declared by its shader. */
public final class ShaderBindingValidator {
    private ShaderBindingValidator() {}

    public static void validate(
            ShaderBindingLayout layout,
            Set<String> uniformBuffers,
            Set<String> texelBuffers,
            Set<String> samplers) {
        requireExact("uniform buffer", layout.uniformBuffers(), uniformBuffers);
        requireExact("texel buffer", layout.texelBuffers(), texelBuffers);
        requireExact("sampler", layout.samplers(), samplers);
    }

    private static void requireExact(String kind, java.util.List<String> declared, Set<String> supplied) {
        Set<String> expected = Set.copyOf(declared);
        Set<String> actual = Set.copyOf(supplied);
        if (expected.equals(actual)) return;

        java.util.Set<String> missing = new java.util.TreeSet<>(expected);
        missing.removeAll(actual);
        java.util.Set<String> undeclared = new java.util.TreeSet<>(actual);
        undeclared.removeAll(expected);
        throw new IllegalArgumentException("Invalid " + kind + " bindings; missing="
                + missing + ", undeclared=" + undeclared);
    }
}
