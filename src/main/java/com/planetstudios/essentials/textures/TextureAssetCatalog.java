package com.planetstudios.essentials.textures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded, immutable-snapshot catalog of discovered texture identities.
 *
 * <p>Writers publish a complete replacement in one atomic operation. Readers
 * never observe a partially populated catalog and never need to lock. The
 * catalog owns metadata only, not texture data or resource handles.</p>
 */
public final class TextureAssetCatalog {
    public static final int DEFAULT_MAX_ASSETS = 32_768;

    private final int maxAssets;
    private final AtomicReference<Snapshot> current =
            new AtomicReference<>(new Snapshot(0L, List.of(), false));

    public TextureAssetCatalog() {
        this(DEFAULT_MAX_ASSETS);
    }

    public TextureAssetCatalog(int maxAssets) {
        if (maxAssets < 1) {
            throw new IllegalArgumentException("maxAssets must be positive");
        }
        this.maxAssets = maxAssets;
    }

    public Snapshot snapshot() {
        return current.get();
    }

    /**
     * Atomically publishes a deterministic, deduplicated catalog.
     * Duplicate resource IDs retain the last descriptor supplied.
     */
    public Snapshot replace(Collection<TextureAssetDescriptor> discovered, boolean sourceTruncated) {
        Objects.requireNonNull(discovered, "discovered");

        Map<String, TextureAssetDescriptor> unique = new LinkedHashMap<>();
        discovered.stream()
                .map(asset -> Objects.requireNonNull(asset, "asset"))
                .sorted(Comparator.comparing(TextureAssetDescriptor::resourceId))
                .forEach(asset -> unique.put(asset.resourceId(), asset));

        List<TextureAssetDescriptor> bounded = new ArrayList<>(Math.min(unique.size(), maxAssets));
        for (TextureAssetDescriptor asset : unique.values()) {
            if (bounded.size() == maxAssets) {
                break;
            }
            bounded.add(asset);
        }

        boolean truncated = sourceTruncated || unique.size() > maxAssets;
        Snapshot previous = current.get();
        Snapshot replacement = new Snapshot(previous.generation() + 1L, bounded, truncated);
        current.set(replacement);
        return replacement;
    }

    /** Clears all retained resource metadata, for client shutdown. */
    public Snapshot clear() {
        Snapshot previous = current.get();
        Snapshot replacement = new Snapshot(previous.generation() + 1L, List.of(), false);
        current.set(replacement);
        return replacement;
    }

    public record Snapshot(long generation, List<TextureAssetDescriptor> assets, boolean truncated) {
        public Snapshot {
            if (generation < 0L) {
                throw new IllegalArgumentException("generation must not be negative");
            }
            assets = List.copyOf(assets);
        }

        public int size() {
            return assets.size();
        }
    }
}
