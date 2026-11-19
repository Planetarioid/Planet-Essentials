package com.planetstudios.essentials.rendering.shaders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolves a validated pack manifest into immutable shader programs with strict byte budgets. */
public final class ShaderPackLoader {
    public static final int MAX_TOTAL_SOURCE_BYTES = 16 * 1024 * 1024;

    private ShaderPackLoader() {}

    public static LoadedPack load(String manifestJson, ShaderResourceResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");
        ShaderPackManifest manifest = ShaderPackManifest.parse(manifestJson);
        List<ShaderProgramSpec> programs = new ArrayList<>(manifest.programs().size());
        Map<String, String> sourceCache = new HashMap<>();

        for (ShaderPackManifest.Program program : manifest.programs()) {
            String vertex = readBounded(resolver, program.vertex(), sourceCache);
            String fragment = readBounded(resolver, program.fragment(), sourceCache);
            long totalBytes = uniqueSourceBytes(sourceCache);
            if (totalBytes > MAX_TOTAL_SOURCE_BYTES) {
                throw new IllegalArgumentException("Shader pack source budget exceeds 16 MiB");
            }
            programs.add(new ShaderProgramSpec(
                    program.id(),
                    Map.of(ShaderStage.VERTEX, vertex, ShaderStage.FRAGMENT, fragment)));
        }
        return new LoadedPack(manifest, programs, uniqueSourceBytes(sourceCache));
    }

    private static long uniqueSourceBytes(Map<String, String> sourceCache) {
        return sourceCache.values().stream()
                .mapToLong(source -> source.getBytes(StandardCharsets.UTF_8).length)
                .sum();
    }

    private static String readBounded(
            ShaderResourceResolver resolver, String resourceId, Map<String, String> sourceCache) {
        String cached = sourceCache.get(resourceId);
        if (cached != null) return cached;
        String source = resolver.readUtf8(resourceId, ShaderProgramSpec.MAX_SOURCE_BYTES);
        if (source == null) {
            throw new IllegalArgumentException("Missing shader source: " + resourceId);
        }
        int bytes = source.getBytes(StandardCharsets.UTF_8).length;
        if (bytes == 0 || bytes > ShaderProgramSpec.MAX_SOURCE_BYTES || source.isBlank()) {
            throw new IllegalArgumentException("Invalid or oversized shader source: " + resourceId);
        }
        sourceCache.put(resourceId, source);
        return source;
    }

    @FunctionalInterface
    public interface ShaderResourceResolver {
        /** Implementations must enforce the supplied byte limit while reading, not after an unbounded read. */
        String readUtf8(String resourceId, int maxBytes);
    }

    public record LoadedPack(ShaderPackManifest manifest, List<ShaderProgramSpec> programs, long sourceBytes) {
        public LoadedPack {
            Objects.requireNonNull(manifest, "manifest");
            programs = List.copyOf(programs);
            if (sourceBytes < 1 || sourceBytes > MAX_TOTAL_SOURCE_BYTES) {
                throw new IllegalArgumentException("Invalid loaded shader pack byte count");
            }
        }
    }
}
