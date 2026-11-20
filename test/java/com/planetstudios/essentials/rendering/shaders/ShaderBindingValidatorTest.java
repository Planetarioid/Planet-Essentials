package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShaderBindingValidatorTest {
    private static final ShaderBindingLayout LAYOUT = new ShaderBindingLayout(
            java.util.List.of("Globals"),
            java.util.List.of("LightData"),
            java.util.List.of("Albedo", "ShadowMap"));

    @Test
    void acceptsAnExactBindingSet() {
        assertDoesNotThrow(() -> ShaderBindingValidator.validate(
                LAYOUT, Set.of("Globals"), Set.of("LightData"), Set.of("Albedo", "ShadowMap")));
    }

    @Test
    void rejectsMissingBindingsBeforeRendering() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ShaderBindingValidator.validate(
                        LAYOUT, Set.of(), Set.of("LightData"), Set.of("Albedo", "ShadowMap")));
        assertTrue(failure.getMessage().contains("Globals"));
        assertTrue(failure.getMessage().contains("missing"));
    }

    @Test
    void rejectsUndeclaredBindings() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ShaderBindingValidator.validate(
                        LAYOUT, Set.of("Globals", "Extra"), Set.of("LightData"), Set.of("Albedo", "ShadowMap")));
        assertTrue(failure.getMessage().contains("Extra"));
        assertTrue(failure.getMessage().contains("undeclared"));
    }

    @Test
    void validatesEachResourceKindIndependently() {
        assertThrows(IllegalArgumentException.class,
                () -> ShaderBindingValidator.validate(
                        LAYOUT, Set.of("Globals"), Set.of(), Set.of("Albedo", "ShadowMap")));
        assertThrows(IllegalArgumentException.class,
                () -> ShaderBindingValidator.validate(
                        LAYOUT, Set.of("Globals"), Set.of("LightData"), Set.of("Albedo")));
    }
}
