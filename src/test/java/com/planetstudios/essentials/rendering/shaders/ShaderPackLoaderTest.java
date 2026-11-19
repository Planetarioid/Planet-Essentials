package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShaderPackLoaderTest {
    private static final String MANIFEST = """
            {
              "schema": 1,
              "id": "planet:sample",
              "name": "Sample Pack",
              "version": "1.0.0",
              "description": "A test pack",
              "programs": [
                {
                  "id": "planet:world",
                  "vertex": "planet:shaders/world.vsh",
                  "fragment": "planet:shaders/world.fsh"
                }
              ]
            }
            """;

    @Test
    void parsesManifestAndLoadsImmutablePrograms() {
        var loaded = ShaderPackLoader.load(MANIFEST, (id, limit) -> switch (id) {
            case "planet:shaders/world.vsh" -> "#version 150\nvoid main() {}";
            case "planet:shaders/world.fsh" -> "#version 150\nout vec4 color; void main() { color = vec4(1.0); }";
            default -> throw new IllegalArgumentException("Missing: " + id);
        });
        assertEquals("planet:sample", loaded.manifest().id());
        assertEquals(1, loaded.programs().size());
        assertEquals("planet:world", loaded.programs().getFirst().id());
        assertTrue(loaded.sourceBytes() > 0);
        assertThrows(UnsupportedOperationException.class, () -> loaded.programs().clear());
    }

    @Test
    void rejectsUnsupportedSchemaAndTraversal() {
        assertThrows(IllegalArgumentException.class,
                () -> ShaderPackManifest.parse(MANIFEST.replace("\"schema\": 1", "\"schema\": 2")));
        assertThrows(IllegalArgumentException.class,
                () -> ShaderPackManifest.parse(MANIFEST.replace("shaders/world.vsh", "shaders/../secret.vsh")));
    }

    @Test
    void rejectsMalformedAndOversizedManifests() {
        assertThrows(IllegalArgumentException.class, () -> ShaderPackManifest.parse("[]"));
        assertThrows(IllegalArgumentException.class,
                () -> ShaderPackManifest.parse(" ".repeat(ShaderPackManifest.MAX_MANIFEST_BYTES + 1)));
    }

    @Test
    void rejectsMissingAndOversizedSources() {
        assertThrows(NullPointerException.class,
                () -> ShaderPackLoader.load(MANIFEST, (id, limit) -> null));
        assertThrows(IllegalArgumentException.class,
                () -> ShaderPackLoader.load(MANIFEST,
                        (id, limit) -> "x".repeat(ShaderProgramSpec.MAX_SOURCE_BYTES + 1)));
    }

    @Test
    void validatesDuplicateProgramIds() {
        String duplicate = MANIFEST.replace(
                "\"id\": \"planet:world\",",
                "\"id\": \"planet:world\",");
        String doubled = duplicate.replace(
                "              ]",
                """
                          , {
                            "id": "planet:world",
                            "vertex": "planet:shaders/world.vsh",
                            "fragment": "planet:shaders/world.fsh"
                          }
                      ]""");
        assertThrows(IllegalArgumentException.class, () -> ShaderPackManifest.parse(doubled));
    }
}
