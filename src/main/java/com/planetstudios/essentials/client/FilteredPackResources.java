package com.planetstudios.essentials.client;

import com.planetstudios.essentials.config.ResourcePackCategoryConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.BiConsumer;

/** Delegating resource-pack view that filters selected asset categories without modifying pack files. */
public final class FilteredPackResources implements PackResources {
    private final PackResources delegate;
    private final ResourcePackCategoryConfig categories;

    public FilteredPackResources(PackResources delegate, ResourcePackCategoryConfig categories) {
        this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
        this.categories = java.util.Objects.requireNonNull(categories, "categories");
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return delegate.getRootResource(path);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        return allowed(type, id) ? delegate.getResource(type, id) : null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        delegate.listResources(type, namespace, path, (id, supplier) -> {
            if (allowed(type, id)) output.accept(id, supplier);
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return delegate.getNamespaces(type);
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> type) throws IOException {
        return delegate.getMetadataSection(type);
    }

    @Override
    public PackLocationInfo location() {
        return delegate.location();
    }

    @Override
    public void close() {
        delegate.close();
    }

    private boolean allowed(PackType type, Identifier id) {
        if (type != PackType.CLIENT_RESOURCES) return true;
        String path = id.getPath();
        if (path.startsWith("textures/")) {
            if (path.endsWith(".mcmeta")) return categories.animations;
            return categories.textures;
        }
        if (path.equals("sounds.json") || path.startsWith("sounds/")) return categories.sounds;
        return true;
    }
}
