package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceRendererShaderSourceTest {
    private static final String VERTEX_PATH =
            "/assets/planet_essentials/shaders/core/space_terrain.vsh";
    private static final String FRAGMENT_PATH =
            "/assets/planet_essentials/shaders/core/space_terrain.fsh";

    @Test
    void builtInTerrainShaderSourcesArePackagedAndUseMinecraft12111Bindings() throws IOException {
        String vertex = read(VERTEX_PATH);
        String fragment = read(FRAGMENT_PATH);

        assertTrue(vertex.startsWith("#version 330"));
        assertTrue(fragment.startsWith("#version 330"));
        assertTrue(vertex.contains("#moj_import <minecraft:dynamictransforms.glsl>"));
        assertTrue(vertex.contains("#moj_import <minecraft:projection.glsl>"));
        assertTrue(vertex.contains("uniform SpaceRendererSection"));
        assertTrue(vertex.contains("uniform sampler2D Sampler2;"));
        assertTrue(fragment.contains("uniform sampler2D Sampler0;"));
        assertTrue(fragment.contains("#ifdef ALPHA_CUTOUT"));
        assertTrue(vertex.contains("in vec3 Position;"));
        assertTrue(vertex.contains("in vec4 Color;"));
        assertTrue(vertex.contains("in vec2 UV0;"));
        assertTrue(vertex.contains("in ivec2 UV2;"));
    }

    private static String read(String path) throws IOException {
        try (InputStream stream = SpaceRendererShaderSourceTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing packaged shader resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
