package com.planetstudios.essentials.rendering.shaders.client;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        Objects.requireNonNull(adapter.testedVersion(), "testedVersion");
        Objects.requireNonNull(adapter.supportedFeatures(), "supportedFeatures");
        PrismRendererAdapter previous = ADAPTERS.putIfAbsent(id, adapter);
        if (previous != null) {
            throw new IllegalArgumentException("A Prism renderer adapter is already registered: " + id);
        }
        return () -> ADAPTERS.remove(id, adapter);
    }

    public static Optional<PrismRendererAdapter> find(String rendererId) {
        return Optional.ofNullable(ADAPTERS.get(Objects.requireNonNull(rendererId, "rendererId")));
    }

    public static Map<String, PrismRendererAdapter> snapshot() {
        return Map.copyOf(ADAPTERS);
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}
