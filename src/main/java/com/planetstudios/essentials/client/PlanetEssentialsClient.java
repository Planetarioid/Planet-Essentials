package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.hud.HudWidgetRenderer;
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
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

public final class PlanetEssentialsClient implements ClientModInitializer {
    private static final long SLOW_TICK_THRESHOLD_NANOS = 50_000_000L;
    private static final TextureAssetCatalog TEXTURE_ASSETS = new TextureAssetCatalog();
    private static final TexturePixelCache TEXTURE_CACHE = new TexturePixelCache();
    private static final TextureDecodeService TEXTURE_DECODER =
            new TextureDecodeService(TextureDecodeLimits.DEFAULT, TEXTURE_CACHE);
    private static final KeyMapping.Category PLANET_KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("planet_essentials", "client"));
    private static KeyMapping OPEN_PLANET_MENU;

    private static long tickStartedAtNanos;
    private static boolean performanceSamplingEnabledForCurrentTick;
    private static boolean eventCollectionEnabledForCurrentTick;

    @Override
    public void onInitializeClient() {
        EssentialsRuntime.initialize();
        OPEN_PLANET_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.planet_essentials.open_menu", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_0, PLANET_KEY_CATEGORY));
        var renderer = PrismRendererCompatibility.detectInstalled();
        PlanetEssentials.LOGGER.info(
                "Prism renderer environment: {} (Iris: {}, adapter: {}). {}",
                renderer.renderer(), renderer.irisPresent(),
                renderer.adapter().map(adapter -> adapter.testedVersion()).orElse("none"),
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
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            EssentialsRuntime.sessionStatistics().start();
            HudWidgetRenderer.comboCounter().reset();
            record(DiagnosticEventType.WORLD_JOINED);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            EssentialsRuntime.sessionStatistics().stop();
            HudWidgetRenderer.comboCounter().reset();
            record(DiagnosticEventType.WORLD_LEFT);
        });

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
            boolean planetMenuRequested = false;
            while (OPEN_PLANET_MENU.consumeClick()) planetMenuRequested = true;
            if (planetMenuRequested && client.screen == null
                    && client.player != null && client.level != null) {
                client.setScreen(new EssentialsScreen(null));
            }

            if (client.player != null && client.level != null) {
                EssentialsRuntime.sessionStatistics().recordPlayerTick(
                        client.player.getX(), client.player.getY(), client.player.getZ());
            }

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
