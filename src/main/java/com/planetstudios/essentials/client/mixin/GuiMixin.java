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
    private static final int CROSSHAIR_ARM_LENGTH = 6;
    private static final int CROSSHAIR_STROKE = 1;
    private static final int TARGET_CROSSHAIR_COLOR = 0xFFFF3030;

    @Inject(method = "render", at = @At("RETURN"))
    private void spaceClient$renderHudWidgets(GuiGraphics graphics, DeltaTracker deltaTracker,
                                               CallbackInfo callback) {
        HudWidgetRenderer.render(graphics, Minecraft.getInstance());
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void spaceClient$drawTargetIndicator(GuiGraphics graphics, DeltaTracker deltaTracker,
                                                  CallbackInfo callback) {
        if (!CrosshairTargetIndicator.shouldHighlight()) return;

        // Use GuiGraphics dimensions, exactly as vanilla does for its 15x15 sprite.
        // Window dimensions can differ from the active GUI coordinate space.
        int left = (graphics.guiWidth() - VANILLA_CROSSHAIR_SIZE) / 2;
        int top = (graphics.guiHeight() - VANILLA_CROSSHAIR_SIZE) / 2;
        int centerX = left + VANILLA_CROSSHAIR_SIZE / 2;
        int centerY = top + VANILLA_CROSSHAIR_SIZE / 2;
        int armEnd = CROSSHAIR_ARM_LENGTH;

        // Match the vanilla sprite's 15x15 footprint: four one-pixel arms,
        // each six pixels long, with a three-pixel opening at the center.
        graphics.fill(left, centerY, left + armEnd, centerY + CROSSHAIR_STROKE,
                TARGET_CROSSHAIR_COLOR);
        graphics.fill(centerX + 2, centerY, centerX + 2 + armEnd, centerY + CROSSHAIR_STROKE,
                TARGET_CROSSHAIR_COLOR);
        graphics.fill(centerX, top, centerX + CROSSHAIR_STROKE, top + armEnd,
                TARGET_CROSSHAIR_COLOR);
        graphics.fill(centerX, centerY + 2, centerX + CROSSHAIR_STROKE, centerY + 2 + armEnd,
                TARGET_CROSSHAIR_COLOR);
    }
}
