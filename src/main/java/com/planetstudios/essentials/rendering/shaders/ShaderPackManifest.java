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
        List<Program> programs) {

    public static final int CURRENT_SCHEMA = 1;
    public static final int MAX_MANIFEST_BYTES = 64 * 1024;
    public static final int MAX_PROGRAMS = 128;
    private static final String NAMESPACE = "[a-z0-9_.-]+";
    private static final String RESOURCE_PATH = "[a-z0-9/._-]+";

    public ShaderPackManifest {
        if (schemaVersion != CURRENT_SCHEMA) {
            throw new IllegalArgumentException("Unsupported Prism shader pack schema: " + schemaVersion);
        }
        requireText(id, "id", 128);
        requireText(name, "name", 96);
        requireText(version, "version", 48);
        description = description == null ? "" : bounded(description, "description", 512);
        author = author == null ? "" : bounded(author, "author", 96);
        if (!id.matches(NAMESPACE + ":" + RESOURCE_PATH) || id.endsWith(":")
                || id.contains("..") || id.contains("//")) {
            throw new IllegalArgumentException("Invalid shader pack identifier: " + id);
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
        programs = List.copyOf(programs);
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
                    requiredString(object, "fragment")));
        }
        return new ShaderPackManifest(schema, id, name, version, description, author, programs);
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

    private static void requireText(String value, String field, int maxLength) {
        bounded(value, field, maxLength);
        if (value.isBlank()) throw new IllegalArgumentException("Field must not be blank: " + field);
    }

    private static String bounded(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field);
        if (value.length() > maxLength) throw new IllegalArgumentException("Field too long: " + field);
        return value;
    }

    public record Program(String id, String vertex, String fragment) {
        public Program {
            requireText(id, "program id", 128);
            validateResourceId(vertex, "vertex");
            validateResourceId(fragment, "fragment");
            if (!id.matches(NAMESPACE + ":" + RESOURCE_PATH) || id.endsWith(":")
                    || id.contains("..") || id.contains("//")) {
                throw new IllegalArgumentException("Invalid shader program identifier: " + id);
            }
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
