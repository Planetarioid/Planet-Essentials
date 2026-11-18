package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** Client-only registrations. No client classes are loaded by dedicated servers. */
public final class PlanetEssentialsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EssentialsRuntime.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (EssentialsRuntime.config().diagnosticsEnabled && client.level != null) {
                EssentialsRuntime.performance().recordFps(Minecraft.getInstance().getFps());
            }
        });
    }
}
