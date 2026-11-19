package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.PlanetEssentials;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Renderer-compatible drawing-stage dispatcher.
 *
 * Fabric drawing callbacks are integration points for custom geometry, not hooks
 * that replace Minecraft's built-in terrain or entity material pipelines.
 */
public final class PrismFrameStageRegistry {
    private static final Map<PrismFrameStage, CopyOnWriteArrayList<Entry>> CALLBACKS =
            new EnumMap<>(PrismFrameStage.class);
    private static final AtomicBoolean EVENTS_REGISTERED = new AtomicBoolean();

    static {
        for (PrismFrameStage stage : PrismFrameStage.values()) {
            CALLBACKS.put(stage, new CopyOnWriteArrayList<>());
        }
    }

    private PrismFrameStageRegistry() {}

    public static void registerFabricEvents() {
        if (!EVENTS_REGISTERED.compareAndSet(false, true)) return;
        WorldRenderEvents.BEFORE_ENTITIES.register(
                context -> dispatch(PrismFrameStage.BEFORE_ENTITIES, context));
        WorldRenderEvents.AFTER_ENTITIES.register(
                context -> dispatch(PrismFrameStage.AFTER_ENTITIES, context));
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(
                context -> dispatch(PrismFrameStage.BEFORE_TRANSLUCENT, context));
        WorldRenderEvents.END_MAIN.register(
                context -> dispatch(PrismFrameStage.END_MAIN, context));
    }

    public static Registration register(
            PrismFrameStage stage, Consumer<WorldRenderContext> callback) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(callback, "callback");
        Entry entry = new Entry(callback);
        CALLBACKS.get(stage).add(entry);
        return () -> {
            entry.enabled.set(false);
            CALLBACKS.get(stage).remove(entry);
        };
    }

    private static void dispatch(PrismFrameStage stage, WorldRenderContext context) {
        for (Entry entry : CALLBACKS.get(stage)) {
            if (!entry.enabled.get()) continue;
            try {
                entry.callback.accept(context);
            } catch (RuntimeException failure) {
                entry.enabled.set(false);
                CALLBACKS.get(stage).remove(entry);
                PlanetEssentials.LOGGER.error(
                        "Disabled a failing Prism {} render-stage callback", stage, failure);
            }
        }
    }

    private static final class Entry {
        private final Consumer<WorldRenderContext> callback;
        private final AtomicBoolean enabled = new AtomicBoolean(true);

        private Entry(Consumer<WorldRenderContext> callback) {
            this.callback = callback;
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}
