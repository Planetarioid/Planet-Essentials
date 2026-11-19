package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.client.mixin.RenderSectionRegionAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.SectionPos;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CPU-side extension point fired from Minecraft's vanilla section compiler.
 * Callbacks run synchronously on the chunk-build worker and must not retain
 * the region beyond the callback or perform GPU/render-thread operations.
 */
public final class PrismVanillaSectionRebuildEvents {
    private static final int MAX_LISTENERS = 64;
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private PrismVanillaSectionRebuildEvents() {}

    /** Registers a CPU-only listener. Registration is safe from any thread. */
    public static Registration register(Listener listener) {
        Listener value = Objects.requireNonNull(listener, "listener");
        if (LISTENERS.size() >= MAX_LISTENERS) {
            throw new IllegalStateException("Prism vanilla rebuild listener limit reached");
        }
        LISTENERS.add(value);
        return () -> LISTENERS.remove(value);
    }

    public static void onVanillaCompile(SectionPos sectionPos, RenderSectionRegion region) {
        Objects.requireNonNull(sectionPos, "sectionPos");
        Objects.requireNonNull(region, "region");
        if (!(((RenderSectionRegionAccessor) (Object) region).planetEssentials$getLevel() instanceof ClientLevel level)) {
            return;
        }
        PrismTerrainSectionLifecycle.BuildToken token = PrismTerrainChunkLifecycleEvents.beginRebuild(
                level, sectionPos.x(), sectionPos.y(), sectionPos.z());
        RebuildContext context = new RebuildContext(level, sectionPos, region, token);
        for (Listener listener : LISTENERS) {
            try {
                listener.onRebuild(context);
            } catch (RuntimeException failure) {
                PlanetEssentials.LOGGER.error("A Prism vanilla section rebuild listener failed at {}",
                        sectionPos, failure);
            }
        }
    }

    /** Immutable metadata plus a callback-scoped view of Minecraft's section snapshot. */
    public record RebuildContext(ClientLevel level, SectionPos sectionPos, RenderSectionRegion region,
                                 PrismTerrainSectionLifecycle.BuildToken token) {
        public RebuildContext {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(sectionPos, "sectionPos");
            Objects.requireNonNull(region, "region");
            Objects.requireNonNull(token, "token");
        }

        /** Publishes only if this vanilla rebuild is still the newest revision for the section. */
        public boolean publish(PrismTerrainVertexData data) {
            return PrismTerrainChunkLifecycleEvents.lifecycle(level).publish(token, data);
        }
    }

    @FunctionalInterface
    public interface Listener {
        void onRebuild(RebuildContext context);
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override void close();
    }
}