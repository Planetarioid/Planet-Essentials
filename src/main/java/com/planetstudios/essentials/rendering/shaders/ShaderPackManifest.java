package com.planetstudios.essentials.rendering.shaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Strict, bounded metadata for a Prism shader pack. */
public record ShaderPackManifest(
        int schemaVersion,
        String id,
        String name,
        String version,
        String description,
        String author,
        String worldPostProcessProgram,
        PrismPostProcessGraph worldPostProcessGraph,
        List<Program> programs) {

    public static final int CURRENT_SCHEMA = 1;
    public static final int MAX_MANIFEST_BYTES = 64 * 1024;
    public static final int MAX_PROGRAMS = 128;
    private static final String NAMESPACE = "[a-z0-9_.-]+";
    private static final String RESOURCE_PATH = "[a-z0-9/._-]+";

    public ShaderPackManifest(int schemaVersion, String id, String name, String version,
            String description, String author, String worldPostProcessProgram, List<Program> programs) {
        this(schemaVersion, id, name, version, description, author,
                worldPostProcessProgram, null, programs);
    }

    public ShaderPackManifest(int schemaVersion, String id, String name, String version,
            String description, String author, List<Program> programs) {
        this(schemaVersion, id, name, version, description, author, "", null, programs);
    }

    public ShaderPackManifest {
        if (schemaVersion != CURRENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported Prism shader pack schema: " + schemaVersion);
        }
        requireText(id, "id", 128);
        requireText(name, "name", 96);
        requireText(version, "version", 48);
        description = description == null ? "" : bounded(description, "description", 512);
        author = author == null ? "" : bounded(author, "author", 96);
        worldPostProcessProgram = worldPostProcessProgram == null ? ""
                : bounded(worldPostProcessProgram, "worldPostProcessProgram", 128);
        if (!id.matches(NAMESPACE + ":" + RESOURCE_PATH) || id.endsWith(":")
                || id.contains("..") || id.contains("//")) {
            throw new IllegalArgumentException("Invalid shader pack identifier: " + id);
        }
        if (!worldPostProcessProgram.isBlank()
                && (!worldPostProcessProgram.matches(NAMESPACE + ":" + RESOURCE_PATH)
                || worldPostProcessProgram.endsWith(":") || worldPostProcessProgram.contains("..")
                || worldPostProcessProgram.contains("//"))) {
            throw new IllegalArgumentException("Invalid world post-process program identifier");
        }
        Objects.requireNonNull(programs, "programs");
        if (programs.isEmpty() || programs.size() > MAX_PROGRAMS) {
            throw new IllegalArgumentException("A shader pack must contain 1 to " + MAX_PROGRAMS + " programs");
        }
        Set<String> ids = new HashSet<>();
        for (Program program : programs) {
            if (!ids.add(program.id())) {
                throw new IllegalArgumentException("Duplicate shader program: " + program.id());
            }
        }
        if (!worldPostProcessProgram.isBlank() && worldPostProcessGraph != null) {
            throw new IllegalArgumentException("Use either worldPostProcessProgram or worldPostProcessGraph, not both");
        }
        if (!worldPostProcessProgram.isBlank()) {
            Program legacy = findProgram(programs, worldPostProcessProgram);
            if (legacy == null) {
                throw new IllegalArgumentException("worldPostProcessProgram must reference a declared program");
            }
            requireFullscreenSamplerContract(legacy, 1);
            if (!legacy.samplers().equals(List.of("SceneColor"))) {
                throw new IllegalArgumentException("Legacy worldPostProcessProgram must declare the SceneColor sampler");
            }
        }
        if (worldPostProcessGraph != null) {
            validateGraphPrograms(worldPostProcessGraph, programs);
        }
        programs = List.copyOf(programs);
    }

    /** Returns the explicit graph, or adapts the legacy single-program field to a one-pass graph. */
    public PrismPostProcessGraph resolvedWorldPostProcessGraph() {
        if (worldPostProcessGraph != null) return worldPostProcessGraph;
        if (worldPostProcessProgram.isBlank()) return null;
        return PrismPostProcessGraph.builder()
                .externalInput(PrismPostProcessGraph.SCENE_COLOR)
                .pass(new PrismPostProcessGraph.Pass(
                        "planet:legacy_world_post", worldPostProcessProgram,
                        List.of(PrismPostProcessGraph.SCENE_COLOR), "planet:legacy_world_post_output"))
                .output("planet:legacy_world_post_output")
                .build();
    }

    public static ShaderPackManifest parse(String json) {
        Objects.requireNonNull(json, "json");
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_MANIFEST_BYTES) {
            throw new IllegalArgumentException("Shader pack manifest exceeds 64 KiB");
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed shader pack manifest", exception);
        }
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("Shader pack manifest must be a JSON object");
        }
        JsonObject root = parsed.getAsJsonObject();
        int schema = requiredInt(root, "schema");
        String id = requiredString(root, "id");
        String name = requiredString(root, "name");
        String version = requiredString(root, "version");
        String description = optionalString(root, "description");
        String author = optionalString(root, "author");
        String postProcessProgram = optionalString(root, "worldPostProcessProgram");
        JsonElement programsElement = root.get("programs");
        if (programsElement == null || !programsElement.isJsonArray()) {
            throw new IllegalArgumentException("Manifest programs must be an array");
        }
        JsonArray array = programsElement.getAsJsonArray();
        if (array.size() == 0 || array.size() > MAX_PROGRAMS) {
            throw new IllegalArgumentException("Invalid shader program count");
        }
        List<Program> programs = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Each shader program must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            programs.add(new Program(
                    requiredString(object, "id"),
                    requiredString(object, "vertex"),
                    requiredString(object, "fragment"),
                    optionalStringArray(object, "uniformBuffers"),
                    optionalStringArray(object, "texelBuffers"),
                    optionalStringArray(object, "samplers")));
        }
        PrismPostProcessGraph graph = root.has("worldPostProcessGraph")
                && !root.get("worldPostProcessGraph").isJsonNull()
                ? parsePostProcessGraph(root.get("worldPostProcessGraph")) : null;
        return new ShaderPackManifest(schema, id, name, version, description, author,
                postProcessProgram, graph, programs);
    }

    private static PrismPostProcessGraph parsePostProcessGraph(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("worldPostProcessGraph must be an object");
        }
        JsonObject object = element.getAsJsonObject();
        List<String> externalInputs = requiredStringArray(object, "externalInputs", 4);
        if (!externalInputs.equals(List.of(PrismPostProcessGraph.SCENE_COLOR))) {
            throw new IllegalArgumentException("World post-process graphs currently support only minecraft:scene_color as an external input");
        }
        JsonElement passElement = object.get("passes");
        if (passElement == null || !passElement.isJsonArray()) {
            throw new IllegalArgumentException("worldPostProcessGraph.passes must be an array");
        }
        JsonArray passArray = passElement.getAsJsonArray();
        if (passArray.isEmpty() || passArray.size() > PrismPostProcessGraph.MAX_PASSES) {
            throw new IllegalArgumentException("Invalid world post-process pass count");
        }
        PrismPostProcessGraph.Builder builder = PrismPostProcessGraph.builder();
        externalInputs.forEach(builder::externalInput);
        for (JsonElement entry : passArray) {
            if (!entry.isJsonObject()) throw new IllegalArgumentException("Each post-process pass must be an object");
            JsonObject pass = entry.getAsJsonObject();
            builder.pass(new PrismPostProcessGraph.Pass(
                    requiredString(pass, "id"),
                    requiredString(pass, "program"),
                    requiredStringArray(pass, "inputs", ShaderBindingLayout.MAX_BINDINGS_PER_KIND),
                    requiredString(pass, "output")));
        }
        builder.output(requiredString(object, "output"));
        return builder.build();
    }

    private static void validateGraphPrograms(PrismPostProcessGraph graph, List<Program> programs) {
        for (PrismPostProcessGraph.Pass pass : graph.passes()) {
            Program program = findProgram(programs, pass.programId());
            if (program == null) {
                throw new IllegalArgumentException("Post-process pass references an undeclared program: " + pass.programId());
            }
            requireFullscreenSamplerContract(program, pass.inputs().size());
        }
    }

    private static void requireFullscreenSamplerContract(Program program, int inputCount) {
        if (!program.uniformBuffers().isEmpty() || !program.texelBuffers().isEmpty()) {
            throw new IllegalArgumentException("World post-process programs may declare samplers only: " + program.id());
        }
        if (program.samplers().size() != inputCount) {
            throw new IllegalArgumentException("Post-process program " + program.id()
                    + " must declare exactly " + inputCount + " sampler(s)");
        }
    }

    private static Program findProgram(List<Program> programs, String id) {
        return programs.stream().filter(program -> program.id().equals(id)).findFirst().orElse(null);
    }

    private static int requiredInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing or invalid integer field: " + key);
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid integer field: " + key, exception);
        }
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing or invalid string field: " + key);
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid string field: " + key);
        }
        return value.getAsString();
    }

    private static List<String> optionalStringArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return List.of();
        if (!value.isJsonArray()) throw new IllegalArgumentException("Invalid array field: " + key);
        return readStringArray(value.getAsJsonArray(), key, ShaderBindingLayout.MAX_BINDINGS_PER_KIND);
    }

    private static List<String> requiredStringArray(JsonObject object, String key, int maxEntries) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) throw new IllegalArgumentException("Missing or invalid array field: " + key);
        return readStringArray(value.getAsJsonArray(), key, maxEntries);
    }

    private static List<String> readStringArray(JsonArray array, String key, int maxEntries) {
        if (array.size() > maxEntries) throw new IllegalArgumentException("Too many entries in " + key);
        List<String> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Invalid entry in " + key);
            }
            result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    private static void requireText(String value, String field, int maxLength) {
        bounded(value, field, maxLength);
        if (value.isBlank()) throw new IllegalArgumentException("Field must not be blank: " + field);
    }

    private static String bounded(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field);
        if (value.length() > maxLength) throw new IllegalArgumentException("Field too long: " + field);
        return value;
    }

    public record Program(
            String id,
            String vertex,
            String fragment,
            List<String> uniformBuffers,
            List<String> texelBuffers,
            List<String> samplers) {
        public Program(String id, String vertex, String fragment) {
            this(id, vertex, fragment, List.of(), List.of(), List.of());
        }

        public Program {
            requireText(id, "program id", 128);
            validateResourceId(vertex, "vertex");
            validateResourceId(fragment, "fragment");
            if (!id.matches(NAMESPACE + ":" + RESOURCE_PATH) || id.endsWith(":")
                    || id.contains("..") || id.contains("//")) {
                throw new IllegalArgumentException("Invalid shader program identifier: " + id);
            }
            ShaderBindingLayout validated = new ShaderBindingLayout(uniformBuffers, texelBuffers, samplers);
            uniformBuffers = validated.uniformBuffers();
            texelBuffers = validated.texelBuffers();
            samplers = validated.samplers();
        }

        public ShaderBindingLayout bindings() {
            return new ShaderBindingLayout(uniformBuffers, texelBuffers, samplers);
        }

        private static void validateResourceId(String value, String stage) {
            requireText(value, stage, 256);
            int separator = value.indexOf(':');
            if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
                throw new IllegalArgumentException("Invalid " + stage + " resource identifier: " + value);
            }
            String namespace = value.substring(0, separator);
            String path = value.substring(separator + 1);
            if (!namespace.matches(NAMESPACE) || !path.matches(RESOURCE_PATH)
                    || path.startsWith("/") || path.endsWith("/") || path.contains("//")
                    || path.contains("..") || path.contains("\\")) {
                throw new IllegalArgumentException("Unsafe " + stage + " resource path: " + value);
            }
        }
    }
}
