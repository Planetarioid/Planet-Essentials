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
    private static final int SPACE_CROSSHAIR_HALF_EXTENT = 7;

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
        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;

        // Vanilla's crosshair occupies a centered 15x15 GUI-pixel sprite. Keep
        // the same center and outer bounds, including its 3-pixel center gap.
        int left = centerX - SPACE_CROSSHAIR_HALF_EXTENT;
        int top = centerY - SPACE_CROSSHAIR_HALF_EXTENT;
        int right = centerX + SPACE_CROSSHAIR_HALF_EXTENT + 1;
        int bottom = centerY + SPACE_CROSSHAIR_HALF_EXTENT + 1;

        // Dark outline follows the vanilla crosshair's shadow, then the red
        // one-pixel strokes sit inside it. All four arms share the same bounds.
        int shadow = 0xFF401010;
        int red = 0xFFFF3030;
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
