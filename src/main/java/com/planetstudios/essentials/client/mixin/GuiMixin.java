package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.client.CrosshairTargetIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a compact red overlay when the vanilla crosshair targets an attackable player. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void planetEssentials$drawTargetIndicator(GuiGraphics graphics, DeltaTracker deltaTracker,
                                                       CallbackInfo callback) {
        if (!CrosshairTargetIndicator.shouldHighlight()) return;

        Minecraft client = Minecraft.getInstance();
        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;
        int red = 0xFFFF3030;
        int shadow = 0xFF401010;

        // Dark one-pixel outline keeps the indicator readable over bright and dark scenes.
        graphics.fill(centerX - 1, centerY - 6, centerX + 2, centerY + 6, shadow);
        graphics.fill(centerX - 6, centerY - 1, centerX + 6, centerY + 2, shadow);
        graphics.fill(centerX, centerY - 5, centerX + 1, centerY + 5, red);
        graphics.fill(centerX - 5, centerY, centerX + 5, centerY + 1, red);
    }
}
