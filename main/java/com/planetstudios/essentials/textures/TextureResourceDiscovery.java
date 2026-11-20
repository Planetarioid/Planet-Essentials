package com.planetstudios.essentials.textures;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Discovers effective PNG texture resources through Minecraft's active
 * ResourceManager. It never scans directories, opens streams, or decodes data.
 */
public final class TextureResourceDiscovery {
    private static final String TEXTURE_DIRECTORY = "textures";

    private TextureResourceDiscovery() {
    }

    public static DiscoveryResult discover(ResourceManager resourceManager, int maxAssets) {
        if (resourceManager == null) {
            throw new IllegalArgumentException("resourceManager must not be null");
        }
        if (maxAssets < 1) {
            throw new IllegalArgumentException("maxAssets must be positive");
        }

        Map<Identifier, ?> resources = resourceManager.listResources(
                TEXTURE_DIRECTORY,
                id -> id.getPath().startsWith(TEXTURE_DIRECTORY + "/")
                        && id.getPath().endsWith(".png")
        );

        List<Identifier> identifiers = resources.keySet().stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();

        boolean truncated = identifiers.size() > maxAssets;
        int retainedCount = Math.min(identifiers.size(), maxAssets);
        List<TextureAssetDescriptor> assets = new ArrayList<>(retainedCount);

        for (int index = 0; index < retainedCount; index++) {
            Identifier id = identifiers.get(index);
            assets.add(new TextureAssetDescriptor(
                    id.toString(),
                    id.getNamespace(),
                    id.getPath(),
                    TextureAssetDescriptor.TextureFormat.PNG
            ));
        }

        return new DiscoveryResult(assets, truncated, identifiers.size());
    }

    public record DiscoveryResult(
            List<TextureAssetDescriptor> assets,
            boolean truncated,
            int discoveredCount
    ) {
        public DiscoveryResult {
            assets = List.copyOf(assets);
            if (discoveredCount < assets.size()) {
                throw new IllegalArgumentException("discoveredCount cannot be smaller than retained assets");
            }
        }
    }
}
