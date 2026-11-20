package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.client.CrosshairTargetIndicator;
import com.planetstudios.essentials.hud.HudWidgetRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws Space Client HUD widgets and the optional player hit crosshair indicator. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    private static final int VANILLA_CROSSHAIR_SIZE = 15;

    @Inject(method = "render", at = @At("RETURN"))
    private void spaceClient$renderHudWidgets(GuiGraphics graphics, DeltaTracker deltaTracker,
                                               CallbackInfo callback) {
        HudWidgetRenderer.render(graphics, Minecraft.getInstance());
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void spaceClient$drawTargetIndicator(GuiGraphics graphics, DeltaTracker deltaTracker,
                                                  CallbackInfo callback) {
        if (!CrosshairTargetIndicator.shouldHighlight()) return;

        Minecraft client = Minecraft.getInstance();
        // Match Gui.renderCrosshair exactly: (guiSize - 15) / 2, with a 15x15
        // footprint. Using guiWidth/Height also preserves vanilla GUI scaling.
        int left = (client.getWindow().getGuiScaledWidth() - VANILLA_CROSSHAIR_SIZE) / 2;
        int top = (client.getWindow().getGuiScaledHeight() - VANILLA_CROSSHAIR_SIZE) / 2;
        int centerX = left + 7;
        int centerY = top + 7;
        int right = left + VANILLA_CROSSHAIR_SIZE;
        int bottom = top + VANILLA_CROSSHAIR_SIZE;

        int shadow = 0xFF401010;
        int red = 0xFFFF3030;

        // Recreate the vanilla 15x15 crosshair silhouette: one-pixel red arms,
        // a three-pixel center opening, and a one-pixel dark outline.
        graphics.fill(left, centerY - 1, centerX - 1, centerY + 2, shadow);
        graphics.fill(centerX + 1, centerY - 1, right, centerY + 2, shadow);
        graphics.fill(centerX - 1, top, centerX + 2, centerY - 1, shadow);
        graphics.fill(centerX - 1, centerY + 1, centerX + 2, bottom, shadow);

        graphics.fill(left + 1, centerY, centerX - 1, centerY + 1, red);
        graphics.fill(centerX + 1, centerY, right - 1, centerY + 1, red);
        graphics.fill(centerX, top + 1, centerX + 1, centerY - 1, red);
        graphics.fill(centerX, centerY + 1, centerX + 1, bottom - 1, red);
    }
}
