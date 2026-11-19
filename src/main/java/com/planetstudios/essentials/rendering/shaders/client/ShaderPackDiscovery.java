package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import com.planetstudios.essentials.rendering.shaders.ShaderPackManifest;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Discovers Prism packs from assets/<namespace>/prism/shaderpacks/<pack>/prism.json.
 * ResourceManager's effective resource map respects active pack priority for each path.
 */
public final class ShaderPackDiscovery {
    public static final String ROOT = "prism/shaderpacks";

    private ShaderPackDiscovery() {}

    public static DiscoveryResult discover(ResourceManager resourceManager) {
        Objects.requireNonNull(resourceManager, "resourceManager");
        MinecraftShaderResourceResolver resolver = new MinecraftShaderResourceResolver(resourceManager);
        Map<Identifier, Resource> manifests = resourceManager.findResources(
                ROOT, id -> id.getPath().startsWith(ROOT + "/")
                        && id.getPath().endsWith("/prism.json"));

        List<ShaderPackLoader.LoadedPack> packs = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        manifests.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> {
                    String manifestLocation = entry.getKey().toString();
                    try {
                        String json = readManifest(entry.getValue(), manifestLocation);
                        ShaderPackLoader.LoadedPack pack = ShaderPackLoader.load(json, resolver);
                        if (!ids.add(pack.manifest().id())) {
                            throw new IllegalArgumentException(
                                    "Duplicate shader pack id: " + pack.manifest().id());
                        }
                        packs.add(pack);
                    } catch (RuntimeException failure) {
                        errors.add(manifestLocation + ": " + failure.getMessage());
                    }
                });
        return new DiscoveryResult(packs, errors);
    }

    private static String readManifest(Resource resource, String location) {
        try (resource; InputStream input = resource.open()) {
            byte[] bytes = input.readNBytes(ShaderPackManifest.MAX_MANIFEST_BYTES + 1);
            if (bytes.length > ShaderPackManifest.MAX_MANIFEST_BYTES) {
                throw new IllegalArgumentException("Manifest exceeds 64 KiB: " + location);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read manifest: " + location, exception);
        }
    }

    public record DiscoveryResult(List<ShaderPackLoader.LoadedPack> packs, List<String> errors) {
        public DiscoveryResult {
            packs = List.copyOf(packs);
            errors = List.copyOf(errors);
        }
    }
}
