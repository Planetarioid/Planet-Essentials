package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import com.planetstudios.essentials.rendering.shaders.ShaderStage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

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
                List.of("PrismSection", "ChunkSection", "Projection", "Fog", "Globals")));
        assertEquals(Set.of("Sampler0", "Sampler2"), Set.copyOf(terrain.bindings().samplers()));

        String vertex = terrain.source(ShaderStage.VERTEX);
        String fragment = terrain.source(ShaderStage.FRAGMENT);
        assertTrue(vertex.contains("Position + SectionOrigin.xyz"));
        assertTrue(vertex.contains("minecraft_sample_lightmap"));
        assertTrue(fragment.contains("UseRgss == 1"));
        assertTrue(fragment.contains("sampleRGSS"));
        assertTrue(fragment.contains("sampleNearest"));
        assertTrue(fragment.contains("TextureSize"));
        assertTrue(fragment.contains("ChunkVisibility"));
        assertTrue(fragment.contains("#ifdef ALPHA_CUTOUT"));
        assertTrue(fragment.contains("FogEnvironmentalStart"));
        assertTrue(fragment.contains("FogRenderDistanceStart"));
    }

    private static String read(String path) throws IOException {
        try (InputStream input = PrismTerrainShaderPackTest.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Resource not found: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
