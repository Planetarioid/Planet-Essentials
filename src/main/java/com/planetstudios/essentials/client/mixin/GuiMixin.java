package com.planetstudios.essentials.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.client.CrosshairTargetIndicator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tints only the vanilla crosshair render pass when a valid player target is under aim. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Unique private float[] planetEssentials$previousShaderColor;

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void planetEssentials$beginTargetTint(GuiGraphics graphics, DeltaTracker deltaTracker,
                                                   CallbackInfo callback) {
        planetEssentials$previousShaderColor = null;
        if (!CrosshairTargetIndicator.shouldHighlight()) return;
        float[] previous = RenderSystem.getShaderColor();
        planetEssentials$previousShaderColor = previous.clone();
        RenderSystem.setShaderColor(previous[0], previous[1] * 0.12F,
                previous[2] * 0.12F, previous[3]);
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void planetEssentials$endTargetTint(GuiGraphics graphics, DeltaTracker deltaTracker,
                                                 CallbackInfo callback) {
        if (planetEssentials$previousShaderColor == null) return;
        float[] previous = planetEssentials$previousShaderColor;
        RenderSystem.setShaderColor(previous[0], previous[1], previous[2], previous[3]);
        planetEssentials$previousShaderColor = null;
    }
}
