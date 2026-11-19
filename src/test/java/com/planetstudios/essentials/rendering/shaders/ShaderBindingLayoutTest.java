package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ShaderBindingLayoutTest {
    @Test
    void acceptsAndCopiesValidDeclarations() {
        var layout = new ShaderBindingLayout(
                java.util.List.of("Globals"),
                java.util.List.of("LightData"),
                java.util.List.of("Sampler0", "ShadowMap"));
        assertEquals(1, layout.uniformBuffers().size());
        assertEquals(1, layout.texelBuffers().size());
        assertEquals(2, layout.samplers().size());
        assertThrows(UnsupportedOperationException.class, () -> layout.samplers().add("Other"));
    }

    @Test
    void rejectsInvalidOrDuplicateNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShaderBindingLayout(java.util.List.of("bad-name"), java.util.List.of(), java.util.List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ShaderBindingLayout(java.util.List.of("Shared"), java.util.List.of("Shared"), java.util.List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ShaderBindingLayout(java.util.List.of(), java.util.List.of(),
                        java.util.Collections.nCopies(17, "Sampler")));
    }

    @Test
    void bindingLayoutContributesToProgramContentHash() {
        var sources = Map.of(ShaderStage.VERTEX, "vertex", ShaderStage.FRAGMENT, "fragment");
        var withoutBindings = new ShaderProgramSpec("planet:test", sources);
        var withBindings = new ShaderProgramSpec("planet:test", sources,
                new ShaderBindingLayout(java.util.List.of("Globals"), java.util.List.of(), java.util.List.of()));
        assertNotEquals(withoutBindings.contentHash(), withBindings.contentHash());
    }
}
