package com.planetstudios.essentials.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.planetstudios.essentials.client.CrosshairTargetIndicator;
import com.planetstudios.essentials.hud.HudWidgetRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws Space Client HUD widgets and tints the vanilla crosshair sprite when
 * the optional player hit indicator is active.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    private static final int TARGET_CROSSHAIR_TINT = 0xFFFF3030;

    @Inject(method = "render", at = @At("RETURN"))
    private void spaceClient$renderHudWidgets(GuiGraphics graphics, DeltaTracker deltaTracker,
                                               CallbackInfo callback) {
        HudWidgetRenderer.render(graphics, Minecraft.getInstance());
    }

    /**
     * Intercepts only Gui.renderCrosshair's vanilla crosshair sprite call.
     * The original sprite, dimensions, GUI-scaled coordinates, and render
     * pipeline are preserved. GuiGraphics applies the tint to the same sprite.
     */
    @Redirect(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void spaceClient$tintVanillaCrosshair(GuiGraphics graphics, RenderPipeline pipeline,
                                                   Identifier sprite, int x, int y, int width, int height) {
        if (CrosshairTargetIndicator.shouldHighlight()) {
            graphics.blitSprite(pipeline, sprite, x, y, width, height, TARGET_CROSSHAIR_TINT);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height);
        }
    }
}
