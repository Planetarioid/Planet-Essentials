package com.planetstudios.essentials.client.mixin;

import com.planetstudios.essentials.client.EssentialsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a second entry point to the same Planet client hub from the pause menu. */
@Mixin(net.minecraft.client.gui.screens.PauseScreen.class)
public abstract class PauseScreenMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry>
    T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void planetEssentials$addClientHubButton(CallbackInfo callback) {
        int buttonY = Math.min(height - 28, height / 4 + 192);
        addRenderableWidget(Button.builder(Component.literal("PLANET CLIENT"), button ->
                        Minecraft.getInstance().setScreen(new EssentialsScreen((Screen) (Object) this)))
                .bounds(width / 2 - 100, buttonY, 200, 20)
                .build());
    }
}
