package com.planetstudios.essentials.rendering.shaders.client;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registry for explicit, separately tested renderer bridge implementations. */
public final class PrismRendererAdapterRegistry {
    private static final Map<String, PrismRendererAdapter> ADAPTERS = new ConcurrentHashMap<>();

    private PrismRendererAdapterRegistry() {}

    public static Registration register(PrismRendererAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        String id = Objects.requireNonNull(adapter.rendererId(), "rendererId");
        if (!id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid Prism renderer ID: " + id);
        }
        String testedVersion = Objects.requireNonNull(adapter.testedVersion(), "testedVersion");
        if (testedVersion.isBlank()) {
            throw new IllegalArgumentException("testedVersion must not be blank");
        }
        Set<PrismRendererAdapter.Feature> features =
                Set.copyOf(Objects.requireNonNull(adapter.supportedFeatures(), "supportedFeatures"));
        PrismRendererAdapter frozen = new RegisteredAdapter(id, testedVersion, features);
        PrismRendererAdapter previous = ADAPTERS.putIfAbsent(id, frozen);
        if (previous != null) {
            throw new IllegalArgumentException("A Prism renderer adapter is already registered: " + id);
        }
        return () -> ADAPTERS.remove(id, frozen);
    }

    public static Optional<PrismRendererAdapter> find(String rendererId) {
        return Optional.ofNullable(ADAPTERS.get(Objects.requireNonNull(rendererId, "rendererId")));
    }

    public static Map<String, PrismRendererAdapter> snapshot() {
        return Map.copyOf(ADAPTERS);
    }

    private record RegisteredAdapter(
            String rendererId,
            String testedVersion,
            Set<PrismRendererAdapter.Feature> supportedFeatures)
            implements PrismRendererAdapter {
        private RegisteredAdapter {
            supportedFeatures = Set.copyOf(supportedFeatures);
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}
