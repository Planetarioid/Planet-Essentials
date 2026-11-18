package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.client.EssentialsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes the vanilla Options entry point into Planet Essentials.
 * The original Minecraft settings remain reachable from the Essentials hub.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void planetEssentials$replaceOptionsScreen(Screen incoming, CallbackInfo callback) {
        if (incoming instanceof OptionsScreen) {
            ((Minecraft) (Object) this).setScreen(new EssentialsScreen(screen));
            callback.cancel();
        }
    }
}
