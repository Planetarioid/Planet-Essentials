package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.client.mixin.MinecraftMixin;
import com.planetstudios.essentials.config.EssentialsConfig;
import com.planetstudios.essentials.diagnostics.PerformanceMonitor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Initial native Minecraft settings hub and diagnostics screen. */
public final class EssentialsScreen extends Screen {
    private final Screen parent;

    public EssentialsScreen(Screen parent) {
        super(Component.literal("Planet Essentials"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int top = this.height / 4;

        addRenderableWidget(Button.builder(diagnosticsLabel(), button -> {
            EssentialsConfig config = EssentialsRuntime.config();
            config.diagnosticsEnabled = !config.diagnosticsEnabled;
            EssentialsRuntime.save();
            button.setMessage(diagnosticsLabel());
        }).bounds(center - 100, top + 34, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Vanilla Minecraft Settings"), button -> {
            if (minecraft != null) MinecraftMixin.openVanillaOptions(minecraft, this);
        }).bounds(center - 100, top + 60, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Clear performance history"), button -> {
            EssentialsRuntime.performance().clear();
        }).bounds(center - 100, top + 86, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 100, top + 120, 200, 20).build());
    }

    private static Component diagnosticsLabel() {
        return Component.literal("Performance diagnostics: "
                + (EssentialsRuntime.config().diagnosticsEnabled ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int center = this.width / 2;
        int top = this.height / 4;
        PerformanceMonitor monitor = EssentialsRuntime.performance();

        graphics.drawCenteredString(font, title, center, top, 0xFFFFFF);
        graphics.drawCenteredString(font,
                "Latest FPS: " + monitor.latestFps(), center, top + 4, 0xD8E8FF);
        graphics.drawCenteredString(font,
                String.format(java.util.Locale.ROOT, "Average FPS: %.1f", monitor.averageFps()),
                center, top + 16, 0xD8E8FF);
        graphics.drawCenteredString(font,
                "Samples retained: " + monitor.sampleCount(),
                center, top + 148, 0xAAAAAA);
        graphics.drawCenteredString(font,
                "More optimization systems are added after profiling and validation.",
                center, top + 162, 0x888888);
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
