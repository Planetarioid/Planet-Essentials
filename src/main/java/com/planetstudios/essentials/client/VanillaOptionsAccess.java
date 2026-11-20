package com.planetstudios.essentials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;

/**
 * Bridges the Planet hub to the native options screen without putting public
 * helper methods or state into a Mixin class.
 */
public final class VanillaOptionsAccess {
    private static boolean allowVanillaOptionsOnce;

    private VanillaOptionsAccess() {
    }

    public static void open(Minecraft client, Screen parent) {
        allowVanillaOptionsOnce = true;
        client.setScreen(new OptionsScreen(parent, client.options));
    }

    public static boolean consumeBypass() {
        if (!allowVanillaOptionsOnce) return false;
        allowVanillaOptionsOnce = false;
        return true;
    }
}
