package com.planetstudios.essentials.rendering.shaders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable, validated vertex/fragment source bundle and binding layout. */
public final class ShaderProgramSpec {
    public static final int MAX_SOURCE_BYTES = 1_048_576;
    private static final String ID_PATTERN = "[a-z0-9_.-]+:[a-z0-9_./-]+";
    private final String id;
    private final Map<ShaderStage, String> sources;
    private final ShaderBindingLayout bindings;
    private final String contentHash;

    public ShaderProgramSpec(String id, Map<ShaderStage, String> sources) {
        this(id, sources, ShaderBindingLayout.EMPTY);
    }

    public ShaderProgramSpec(
            String id, Map<ShaderStage, String> sources, ShaderBindingLayout bindings) {
        if (id == null || !id.matches(ID_PATTERN) || id.contains("..") || id.contains("//")) {
            throw new IllegalArgumentException("Invalid shader program identifier");
        }
        Objects.requireNonNull(sources, "sources");
        if (sources.size() != 2 || !sources.containsKey(ShaderStage.VERTEX)
                || !sources.containsKey(ShaderStage.FRAGMENT)) {
            throw new IllegalArgumentException("A program requires exactly one vertex and one fragment source");
        }
        Map<ShaderStage, String> copy = new TreeMap<>();
        for (ShaderStage stage : ShaderStage.values()) {
            String source = Objects.requireNonNull(sources.get(stage), stage + " source");
            int bytes = source.getBytes(StandardCharsets.UTF_8).length;
            if (source.isBlank() || bytes > MAX_SOURCE_BYTES) {
                throw new IllegalArgumentException("Shader source is empty or exceeds 1 MiB: " + stage);
            }
            copy.put(stage, source);
        }
        this.id = id;
        this.sources = Map.copyOf(copy);
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.contentHash = hash(copy, bindings);
    }

    public String id() { return id; }
    public Map<ShaderStage, String> sources() { return sources; }
    public String source(ShaderStage stage) { return sources.get(stage); }
    public ShaderBindingLayout bindings() { return bindings; }
    public String contentHash() { return contentHash; }

    private static String hash(Map<ShaderStage, String> values, ShaderBindingLayout bindings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (ShaderStage stage : ShaderStage.values()) {
                digest.update(stage.name().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(values.get(stage).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 255);
            }
            digest.update(bindings.canonicalForm().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShaderProgramSpec spec
                && id.equals(spec.id) && contentHash.equals(spec.contentHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contentHash);
    }
}
