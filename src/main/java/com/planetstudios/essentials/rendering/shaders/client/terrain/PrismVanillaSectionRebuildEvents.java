package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.client.mixin.RenderSectionRegionAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CPU-side hooks around Minecraft's real section compiler. The rebuild input
 * is callback-scoped and all listeners run on the vanilla compile worker.
 */
public final class PrismVanillaSectionRebuildEvents {
    private static final int MAX_LISTENERS = 64;
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<RebuildContext> ACTIVE_REBUILD = new ThreadLocal<>();

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

    /** Called at SectionCompiler.compile HEAD. */
    public static void onVanillaCompile(SectionPos sectionPos, RenderSectionRegion region) {
        Objects.requireNonNull(sectionPos, "sectionPos");
        Objects.requireNonNull(region, "region");
        if (!(((RenderSectionRegionAccessor) (Object) region).planetEssentials$getLevel() instanceof ClientLevel level)) {
            ACTIVE_REBUILD.remove();
            return;
        }
        PrismTerrainSectionLifecycle.BuildToken token = PrismTerrainChunkLifecycleEvents.beginRebuild(
                level, sectionPos.x(), sectionPos.y(), sectionPos.z());
        RebuildContext context = new RebuildContext(level, sectionPos, region, token);
        ACTIVE_REBUILD.set(context);
        for (Listener listener : LISTENERS) {
            try {
                listener.onRebuild(context);
            } catch (RuntimeException failure) {
                PlanetEssentials.LOGGER.error("A Prism vanilla section rebuild listener failed at {}",
                        sectionPos, failure);
            }
        }
    }

    /**
     * Called at SectionCompiler.compile RETURN. Copies the completed vanilla
     * layer buffers before Minecraft transfers/closes their MeshData objects.
     */
    public static void onVanillaCompileComplete(SectionCompiler.Results results) {
        RebuildContext context = ACTIVE_REBUILD.get();
        ACTIVE_REBUILD.remove();
        if (context == null || results == null) return;
        PrismVanillaTerrainCapture.capture(context, results);
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
