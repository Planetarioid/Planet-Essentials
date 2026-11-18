package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Client-only registrations. No client classes are loaded by dedicated servers. */
public final class PlanetEssentialsClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "planet_essentials", "main"));
    private static KeyMapping openScreenKey;

    @Override
    public void onInitializeClient() {
        EssentialsRuntime.initialize();
        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.planet_essentials.open",
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreenKey.consumeClick()) {
                client.setScreen(new EssentialsScreen(client.screen));
            }
            if (EssentialsRuntime.config().diagnosticsEnabled && client.level != null) {
                EssentialsRuntime.performance().recordFps(Minecraft.getInstance().getFps());
            }
        });
    }
}
