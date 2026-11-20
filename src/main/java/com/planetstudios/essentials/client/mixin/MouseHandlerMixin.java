package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.client.InputTelemetry;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts physical mouse presses for the optional CPS HUD; it never synthesizes input. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void spaceClient$recordClick(long window, MouseButtonInfo buttonInfo, int action,
                                         CallbackInfo callback) {
        if (action == GLFW.GLFW_PRESS) InputTelemetry.recordMousePress(buttonInfo.button());
    }
}
