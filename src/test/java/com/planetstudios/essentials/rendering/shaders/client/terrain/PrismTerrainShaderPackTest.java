package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainShaderPackTest {
    private static final String ROOT = "assets/planet_essentials/prism/shaderpacks/default/";

    @Test
    void bundledPackDeclaresTheCompleteTerrainV1BindingContract() throws IOException {
        String manifest = read(ROOT + "prism.json");
        var loaded = ShaderPackLoader.load(manifest, (resourceId, maxBytes) -> {
            String path = resourceId.substring(resourceId.indexOf(':') + 1);
            try {
                return read("assets/planet_essentials/" + path);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Missing bundled shader source: " + resourceId, exception);
            }
        });

        assertEquals("planet:default", loaded.manifest().id());
        assertEquals(1, loaded.programs().size());
        var terrain = loaded.programs().getFirst();
        assertEquals("planet:terrain", terrain.id());
        assertTrue(terrain.bindings().uniformBuffers().containsAll(
                java.util.List.of("PrismSection", "ChunkSection", "Projection", "Fog")));
        assertEquals(java.util.Set.of("Sampler0", "Sampler2"),
                java.util.Set.copyOf(terrain.bindings().samplers()));
        assertTrue(terrain.source(com.planetstudios.essentials.rendering.shaders.ShaderStage.VERTEX)
                .contains("SectionOrigin.xyz + Position"));
        assertTrue(terrain.source(com.planetstudios.essentials.rendering.shaders.ShaderStage.FRAGMENT)
                .contains("#ifdef ALPHA_CUTOUT"));
    }

    private static String read(String path) throws IOException {
        try (InputStream input = PrismTerrainShaderPackTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Resource not found: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
