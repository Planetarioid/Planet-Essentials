package com.planetstudios.essentials.config;

import com.planetstudios.essentials.EssentialsRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Maps normalized server addresses to existing named profiles. */
public final class ServerProfileManager {
    private ServerProfileManager() {}

    public static String normalizeAddress(String address) {
        if (address == null) throw new IllegalArgumentException("Server address is required");
        String normalized = address.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.length() > 255
                || !normalized.matches("[a-z0-9._:\\[\\]-]+")) {
            throw new IllegalArgumentException("Server address contains unsupported characters");
        }
        return normalized;
    }

    public static Optional<String> currentServerKey(Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isBlank()) return Optional.empty();
        try {
            return Optional.of(normalizeAddress(server.ip));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean linkCurrentServer(Minecraft client, String profileName) {
        String profile = ProfileStore.normalizeName(profileName);
        Optional<String> key = currentServerKey(client);
        if (key.isEmpty()) return false;
        EssentialsRuntime.config().serverProfileMappings.put(key.get(), profile);
        EssentialsRuntime.save();
        return true;
    }

    /** Applies a previously linked profile on world join. Unknown or deleted profiles are ignored. */
    public static Optional<String> applyForCurrentServer(Minecraft client) {
        EssentialsConfig config = EssentialsRuntime.config();
        if (!config.automaticServerProfilesEnabled) return Optional.empty();
        Optional<String> key = currentServerKey(client);
        if (key.isEmpty()) return Optional.empty();
        String profile = config.serverProfileMappings.get(key.get());
        if (profile == null) return Optional.empty();
        try {
            EssentialsConfig loaded = EssentialsRuntime.profiles().load(profile);
            loaded.activeProfile = profile;
            loaded.serverProfileMappings = config.serverProfileMappings;
            loaded.automaticServerProfilesEnabled = config.automaticServerProfilesEnabled;
            EssentialsRuntime.replaceConfig(loaded);
            EssentialsRuntime.save();
            return Optional.of(profile);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Map<String, String> mappings() {
        return Map.copyOf(EssentialsRuntime.config().serverProfileMappings);
    }
}
