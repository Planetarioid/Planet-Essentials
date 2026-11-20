package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackResourceReloadListener;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStageRegistry;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainFrameCoordinator;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainChunkLifecycleEvents;
import com.planetstudios.essentials.rendering.shaders.client.PrismWorldPostProcessor;
import com.planetstudios.essentials.rendering.shaders.client.PrismRendererCompatibility;
import com.planetstudios.essentials.diagnostics.DiagnosticEventType;
import com.planetstudios.essentials.textures.TextureAssetCatalog;
import com.planetstudios.essentials.textures.TextureDecodeLimits;
import com.planetstudios.essentials.textures.TextureDecodeService;
import com.planetstudios.essentials.textures.TexturePixelCache;
import com.planetstudios.essentials.textures.TextureResourceDecoder;
import com.planetstudios.essentials.textures.TextureGpuRegistrar;
import com.planetstudios.essentials.textures.TextureFallback;
import com.planetstudios.essentials.textures.TextureResourceReloadListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

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
        var renderer = PrismRendererCompatibility.detectInstalled();
        PlanetEssentials.LOGGER.info(
                "Prism renderer environment: {} (Iris: {}, adapter: {}). {}",
                renderer.renderer(), renderer.irisPresent(),
                renderer.adapter().map(PrismRendererAdapter -> PrismRendererAdapter.testedVersion()).orElse("none"),
                renderer.compatibilityNote());

        TextureResourceReloadListener.register(TEXTURE_ASSETS, TEXTURE_DECODER);
        ShaderPackResourceReloadListener.register();
        PrismFrameStageRegistry.registerFabricEvents();
        PrismTerrainFrameCoordinator.registerFabricEvents();
        PrismTerrainChunkLifecycleEvents.registerFabricEvents();
        PrismWorldPostProcessor.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> record(DiagnosticEventType.CLIENT_STARTED));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            record(DiagnosticEventType.CLIENT_STOPPING);
            TextureGpuRegistrar.shutdown(client);
            TEXTURE_ASSETS.clear();
            TEXTURE_DECODER.close();
            PrismWorldPostProcessor.shutdown();
            ShaderPackRuntime.shutdown();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> record(DiagnosticEventType.WORLD_JOINED));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
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

    public static TextureAssetCatalog.Snapshot textureAssetsSnapshot() { return TEXTURE_ASSETS.snapshot(); }

    public static CompletableFuture<com.planetstudios.essentials.textures.DecodedTexture> decodeTexture(
            Identifier resourceId) {
        return TextureResourceDecoder.decode(
                Minecraft.getInstance().getResourceManager(), resourceId, TEXTURE_DECODER);
    }

    public static CompletableFuture<Identifier> decodeAndRegisterTexture(Identifier resourceId) {
        return decodeTexture(resourceId).thenCompose(decoded ->
                TextureGpuRegistrar.register(Minecraft.getInstance(), resourceId, decoded));
    }

    public static CompletableFuture<Identifier> decodeAndRegisterTextureWithVanillaFallback(
            Identifier resourceId) {
        return TextureFallback.orElse(decodeAndRegisterTexture(resourceId), resourceId);
    }

    private static void record(DiagnosticEventType type) {
        var config = EssentialsRuntime.config();
        if (config.diagnosticsEnabled && config.eventCollectionEnabled) {
            EssentialsRuntime.diagnosticEvents().record(type);
        }
    }
}
