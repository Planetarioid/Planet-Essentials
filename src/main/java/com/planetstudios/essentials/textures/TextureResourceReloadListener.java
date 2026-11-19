package com.planetstudios.essentials.textures;

import com.planetstudios.essentials.PlanetEssentials;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourceType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Client resource reload bridge for the metadata-only texture catalog.
 *
 * <p>The current stage intentionally does not intercept Minecraft's texture
 * upload/render path. A failed discovery leaves the last known-good snapshot
 * available, so the catalog cannot interfere with vanilla resource fallback.</p>
 */
public final class TextureResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            PlanetEssentials.MOD_ID, "texture_asset_discovery"
    );

    private static final Logger LOGGER = PlanetEssentials.LOGGER;
    private final TextureAssetCatalog catalog;

    public TextureResourceReloadListener(TextureAssetCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public static void register(TextureAssetCatalog catalog) {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new TextureResourceReloadListener(catalog));
    }

    @Override
    public Identifier getFabricId() {
        return LISTENER_ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        try {
            TextureResourceDiscovery.DiscoveryResult result =
                    TextureResourceDiscovery.discover(resourceManager, TextureAssetCatalog.DEFAULT_MAX_ASSETS);
            TextureAssetCatalog.Snapshot snapshot = catalog.replace(result.assets(), result.truncated());
            LOGGER.info("Texture discovery indexed {} effective PNG resources (generation {}, truncated: {}).",
                    snapshot.size(), snapshot.generation(), snapshot.truncated());
        } catch (RuntimeException exception) {
            LOGGER.warn("Texture asset discovery failed; retaining the previous catalog snapshot.", exception);
        }
    }
}
