package com.planetstudios.essentials.rendering.shaders;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    void parsesAndValidatesMultiPassGraph() {
        String manifest = """
                {
                  "schema": 1,
                  "id": "planet:cinematic",
                  "name": "Cinematic",
                  "version": "1.0",
                  "worldPostProcessGraph": {
                    "externalInputs": ["minecraft:scene_color"],
                    "passes": [
                      {"id":"planet:combine","program":"planet:combine","inputs":["planet:blurred","minecraft:scene_color"],"output":"planet:final"},
                      {"id":"planet:blur","program":"planet:blur","inputs":["minecraft:scene_color"],"output":"planet:blurred"}
                    ],
                    "output":"planet:final"
                  },
                  "programs": [
                    {"id":"planet:blur","vertex":"planet:shaders/blur.vsh","fragment":"planet:shaders/blur.fsh","samplers":["SceneColor"]},
                    {"id":"planet:combine","vertex":"planet:shaders/combine.vsh","fragment":"planet:shaders/combine.fsh","samplers":["Blurred","Original"]}
                  ]
                }
                """;
        var loaded = ShaderPackLoader.load(manifest, (id, limit) -> "#version 150\nvoid main() {}");
        var graph = loaded.manifest().resolvedWorldPostProcessGraph();
        assertNotNull(graph);
        assertEquals(List.of("planet:blur", "planet:combine"),
                graph.passes().stream().map(PrismPostProcessGraph.Pass::programId).toList());
        assertEquals("planet:final", graph.output());
        assertEquals(4, loaded.programs().size());
    }

    @Test
    void adaptsLegacySinglePassManifestToGraph() {
        String legacy = MANIFEST.replace("\"programs\":",
                "\"worldPostProcessProgram\":\"planet:world\",\"programs\":")
                .replace("\"fragment\":\"planet:shaders/world.fsh\"",
                        "\"fragment\":\"planet:shaders/world.fsh\",\"samplers\":[\"SceneColor\"]");
        var parsed = ShaderPackManifest.parse(legacy);
        assertEquals("planet:world", parsed.worldPostProcessProgram());
        assertEquals(1, parsed.resolvedWorldPostProcessGraph().passes().size());
    }

    @Test
    void rejectsInvalidMultiPassReferencesAndSamplerContracts() {
        String base = """
                {
                  "schema": 1,
                  "id": "planet:cinematic",
                  "name": "Cinematic",
                  "version": "1.0",
                  "worldPostProcessGraph": {
                    "externalInputs": ["minecraft:scene_color"],
                    "passes": [{"id":"planet:blur","program":"planet:missing","inputs":["minecraft:scene_color"],"output":"planet:blurred"}],
                    "output":"planet:blurred"
                  },
                  "programs": [
                    {"id":"planet:blur","vertex":"planet:shaders/blur.vsh","fragment":"planet:shaders/blur.fsh","samplers":["SceneColor"]}
                  ]
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> ShaderPackManifest.parse(base));
        String wrongSamplerCount = base.replace("\"program\":\"planet:missing\"", "\"program\":\"planet:blur\"")
                .replace("\"samplers\":[\"SceneColor\"]", "\"samplers\":[]");
        assertThrows(IllegalArgumentException.class, () -> ShaderPackManifest.parse(wrongSamplerCount));
    }

    @Test
    void validatesOptionalWorldPostProcessProgramReference() {
        String valid = MANIFEST.replace("\"programs\":",
                "\"worldPostProcessProgram\": \"planet:world\", \"programs\":")
                .replace("\"fragment\":\"planet:shaders/world.fsh\"",
                        "\"fragment\":\"planet:shaders/world.fsh\",\"samplers\":[\"SceneColor\"]");
        assertEquals("planet:world", ShaderPackManifest.parse(valid).worldPostProcessProgram());
        String invalid = valid.replace("\"worldPostProcessProgram\": \"planet:world\"",
                "\"worldPostProcessProgram\": \"planet:missing\"");
        assertThrows(IllegalArgumentException.class, () -> ShaderPackManifest.parse(invalid));
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
        assertThrows(IllegalArgumentException.class,
                () -> ShaderPackLoader.load(MANIFEST, (id, limit) -> null));
        assertThrows(IllegalArgumentException.class,
                () -> ShaderPackLoader.load(MANIFEST,
                        (id, limit) -> "x".repeat(ShaderProgramSpec.MAX_SOURCE_BYTES + 1)));
    }

    @Test
    void validatesDuplicateProgramIds() {
        String duplicate = """
                {
                  "schema": 1,
                  "id": "planet:sample",
                  "name": "Sample Pack",
                  "version": "1.0.0",
                  "programs": [
                    {"id":"planet:world","vertex":"planet:shaders/a.vsh","fragment":"planet:shaders/a.fsh"},
                    {"id":"planet:world","vertex":"planet:shaders/b.vsh","fragment":"planet:shaders/b.fsh"}
                  ]
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> ShaderPackManifest.parse(duplicate));
    }

    @Test
    void resolvesSharedShaderSourcesOnlyOnceAndCountsUniqueBytes() {
        AtomicInteger reads = new AtomicInteger();
        String shared = "#version 150\nvoid main() {}";
        String manifest = """
                {
                  "schema": 1,
                  "id": "planet:sample",
                  "name": "Sample Pack",
                  "version": "1.0.0",
                  "programs": [
                    {"id":"planet:first","vertex":"planet:shaders/shared.vsh","fragment":"planet:shaders/shared.fsh"},
                    {"id":"planet:second","vertex":"planet:shaders/shared.vsh","fragment":"planet:shaders/shared.fsh"}
                  ]
                }
                """;
        var loaded = ShaderPackLoader.load(manifest, (id, limit) -> {
            reads.incrementAndGet();
            return shared;
        });
        assertEquals(2, reads.get());
        assertEquals(shared.getBytes(java.nio.charset.StandardCharsets.UTF_8).length * 2L,
                loaded.sourceBytes());
        assertEquals(2, loaded.programs().size());
    }
}
