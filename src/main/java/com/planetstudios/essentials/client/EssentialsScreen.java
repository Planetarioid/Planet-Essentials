package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.client.mixin.MinecraftMixin;
import com.planetstudios.essentials.config.SettingsRegistry;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Main Planet Essentials settings hub. */
public final class EssentialsScreen extends Screen {
    private final Screen parent;

    public EssentialsScreen(Screen parent) {
        super(Component.literal("Planet Essentials"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int top = height / 4;

        addRenderableWidget(Button.builder(Component.literal("Performance & Diagnostics"), button ->
                minecraft.setScreen(new SettingsCategoryScreen(this, "diagnostics")))
                .bounds(center - 100, top + 30, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Configuration Profiles"), button ->
                minecraft.setScreen(new ProfilesScreen(this)))
                .bounds(center - 100, top + 56, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Vanilla Minecraft Settings"), button ->
                MinecraftMixin.openVanillaOptions(minecraft, this))
                .bounds(center - 100, top + 82, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 100, top + 112, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int center = width / 2;
        int top = height / 4;
        graphics.drawCenteredString(font, title, center, top, 0xFFFFFF);

        if (EssentialsRuntime.config().showPerformanceScreen) {
            PerformanceMonitor monitor = EssentialsRuntime.performance();
            graphics.drawCenteredString(font, "Latest FPS: " + monitor.latestFps(), center, top + 145, 0xD8E8FF);
            graphics.drawCenteredString(font,
                    String.format(java.util.Locale.ROOT, "Average FPS: %.1f", monitor.averageFps()),
                    center, top + 157, 0xD8E8FF);
            graphics.drawCenteredString(font, "Samples retained: " + monitor.sampleCount(),
                    center, top + 169, 0xAAAAAA);
        }
        graphics.drawCenteredString(font,
                "Registered settings: " + SettingsRegistry.all().size(),
                center, height - 18, 0x888888);
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
