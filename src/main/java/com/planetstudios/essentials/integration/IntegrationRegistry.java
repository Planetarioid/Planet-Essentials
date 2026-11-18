package com.planetstudios.essentials.integration;

import com.planetstudios.planetlib.essentials.PlanetIntegration;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Thread-safe registry of immutable metadata supplied by installed Planet mods.
 * Conflicting registrations for the same mod ID are rejected.
 */
public final class IntegrationRegistry {
    private static final IntegrationRegistry INSTANCE = new IntegrationRegistry();

    private final Map<String, PlanetIntegration> integrations = new TreeMap<>();

    public static IntegrationRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers metadata. Repeating an identical registration is idempotent.
     *
     * @return true when the descriptor is registered or already present
     */
    public synchronized boolean register(PlanetIntegration integration) {
        Objects.requireNonNull(integration, "integration");
        PlanetIntegration existing = integrations.get(integration.modId());
        if (existing != null) {
            return existing.equals(integration);
        }
        integrations.put(integration.modId(), integration);
        return true;
    }

    public synchronized Optional<PlanetIntegration> find(String modId) {
        return Optional.ofNullable(integrations.get(Objects.requireNonNull(modId, "modId")));
    }

    /** Returns a stable, immutable snapshot ordered by mod ID. */
    public synchronized Map<String, PlanetIntegration> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(integrations));
    }

    public synchronized int size() {
        return integrations.size();
    }
}
