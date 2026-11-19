package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.diagnostics.DiagnosticEventType;
import com.planetstudios.essentials.textures.TextureAssetCatalog;
import com.planetstudios.essentials.textures.TextureDecodeLimits;
import com.planetstudios.essentials.textures.TextureDecodeService;
import com.planetstudios.essentials.textures.TexturePixelCache;
import com.planetstudios.essentials.textures.TextureResourceDecoder;
import com.planetstudios.essentials.textures.TextureResourceReloadListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

/** Client-only registrations. No client classes are loaded by dedicated servers. */
public final class PlanetEssentialsClient implements ClientModInitializer {
    private static final long SLOW_TICK_THRESHOLD_NANOS = 50_000_000L;
    private static final TextureAssetCatalog TEXTURE_ASSETS = new TextureAssetCatalog();
    private static final TexturePixelCache TEXTURE_CACHE = new TexturePixelCache();
    private static final TextureDecodeService TEXTURE_DECODER =
            new TextureDecodeService(TextureDecodeLimits.DEFAULT, TEXTURE_CACHE);

    private static long tickStartedAtNanos;
    private static boolean performanceSamplingEnabledForCurrentTick;
    private static boolean eventCollectionEnabledForCurrentTick;

    @Override
    public void onInitializeClient() {
        EssentialsRuntime.initialize();
        TextureResourceReloadListener.register(TEXTURE_ASSETS, TEXTURE_DECODER);

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> record(DiagnosticEventType.CLIENT_STARTED));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            record(DiagnosticEventType.CLIENT_STOPPING);
            TEXTURE_ASSETS.clear();
            TEXTURE_DECODER.close();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> record(DiagnosticEventType.WORLD_JOINED));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> record(DiagnosticEventType.WORLD_LEFT));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            // One config read per tick. Disabled subsystems do not sample or allocate.
            var config = EssentialsRuntime.config();
            boolean diagnosticsEnabled = config.diagnosticsEnabled;
            performanceSamplingEnabledForCurrentTick =
                    diagnosticsEnabled && config.performanceMonitoringEnabled;
            eventCollectionEnabledForCurrentTick =
                    diagnosticsEnabled && config.eventCollectionEnabled;
            tickStartedAtNanos = eventCollectionEnabledForCurrentTick ? System.nanoTime() : 0L;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (performanceSamplingEnabledForCurrentTick && client.level != null) {
                EssentialsRuntime.performance().recordFps(Minecraft.getInstance().getFps());
            }
            if (eventCollectionEnabledForCurrentTick && tickStartedAtNanos != 0L) {
                long elapsed = System.nanoTime() - tickStartedAtNanos;
                if (elapsed >= SLOW_TICK_THRESHOLD_NANOS) {
                    EssentialsRuntime.diagnosticEvents().record(
                            DiagnosticEventType.SLOW_CLIENT_TICK, elapsed / 1_000_000L);
                }
            }
            tickStartedAtNanos = 0L;
        });
    }

    /** Metadata-only view used by diagnostics and integration code. */
    public static TextureAssetCatalog.Snapshot textureAssetsSnapshot() {
        return TEXTURE_ASSETS.snapshot();
    }

    /**
     * Resolves the effective resource through Minecraft's active ResourceManager
     * and decodes it off-thread. Only catalogued PNG texture assets are accepted.
     */
    public static CompletableFuture<com.planetstudios.essentials.textures.DecodedTexture> decodeTexture(
            Identifier resourceId) {
        return TextureResourceDecoder.decode(
                Minecraft.getInstance().getResourceManager(), resourceId, TEXTURE_DECODER);
    }

    private static void record(DiagnosticEventType type) {
        var config = EssentialsRuntime.config();
        if (config.diagnosticsEnabled && config.eventCollectionEnabled) {
            EssentialsRuntime.diagnosticEvents().record(type);
        }
    }
}
