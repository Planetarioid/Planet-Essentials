package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.ExperienceConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Customization controls for the Planet client presentation and HUD preferences. */
public final class ExperienceSettingsScreen extends Screen {
    private final Screen parent;

    public ExperienceSettingsScreen(Screen parent) {
        super(Component.literal("Planet Experience"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ExperienceConfig config = EssentialsRuntime.config().experience;
        int center = width / 2;
        int left = center - 155;
        int y = Math.max(54, height / 5);

        addRenderableWidget(cycle("Theme", left, y, 310, () -> config.theme,
                new String[]{"midnight", "obsidian", "light"}, value -> config.theme = value));
        y += 27;
        addRenderableWidget(cycle("Accent", left, y, 310, () -> config.accent,
                new String[]{"cyan", "violet", "lime", "amber", "rose"}, value -> config.accent = value));
        y += 27;
        addRenderableWidget(cycle("Background", left, y, 310, () -> config.background,
                new String[]{"gradient", "solid", "dimmed"}, value -> config.background = value));
        y += 27;
        addRenderableWidget(cycle("UI scale", left, y, 310,
                () -> String.format(java.util.Locale.ROOT, "%.2fx", config.uiScale),
                new String[]{"0.75x", "0.85x", "1.0x", "1.15x", "1.3x", "1.5x"},
                value -> config.uiScale = Float.parseFloat(value.substring(0, value.length() - 1))));
        y += 27;
        addRenderableWidget(cycle("Panel opacity", left, y, 310,
                () -> Math.round(config.panelOpacity * 100) + "%",
                new String[]{"45%", "60%", "75%", "85%", "92%", "100%"},
                value -> config.panelOpacity = Integer.parseInt(value.replace("%", "")) / 100.0f));
        y += 27;
        addRenderableWidget(toggle("Interface animations", left, y, 310,
                () -> config.animations, value -> config.animations = value));
        y += 27;
        addRenderableWidget(toggle("Compact layout", left, y, 310,
                () -> config.compactLayout, value -> config.compactLayout = value));
        y += 36;

        addRenderableWidget(toggle("HUD: FPS", left, y, 310,
                () -> config.showFpsWidget, value -> config.showFpsWidget = value));
        y += 25;
        addRenderableWidget(toggle("HUD: Ping", left, y, 310,
                () -> config.showPingWidget, value -> config.showPingWidget = value));
        y += 25;
        addRenderableWidget(toggle("HUD: Frame time", left, y, 310,
                () -> config.showFrameTimeWidget, value -> config.showFrameTimeWidget = value));
        y += 25;
        addRenderableWidget(toggle("HUD: Memory", left, y, 310,
                () -> config.showMemoryWidget, value -> config.showMemoryWidget = value));
        y += 25;
        addRenderableWidget(toggle("HUD: CPU", left, y, 310,
                () -> config.showCpuWidget, value -> config.showCpuWidget = value));
        y += 25;
        addRenderableWidget(toggle("HUD: GPU", left, y, 310,
                () -> config.showGpuWidget, value -> config.showGpuWidget = value));

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            EssentialsRuntime.save();
            minecraft.setScreen(parent);
        }).bounds(center - 100, height - 30, 200, 20).build());
    }

    private Button cycle(String name, int x, int y, int width,
                         java.util.function.Supplier<String> current, String[] values,
                         java.util.function.Consumer<String> setter) {
        return Button.builder(label(name, current.get()), button -> {
            int index = java.util.Arrays.asList(values).indexOf(current.get());
            setter.accept(values[(index + 1 + values.length) % values.length]);
            EssentialsRuntime.save();
            button.setMessage(label(name, current.get()));
        }).bounds(x, y, width, 20).build();
    }

    private Button toggle(String name, int x, int y, int width,
                          java.util.function.Supplier<Boolean> current,
                          java.util.function.Consumer<Boolean> setter) {
        return Button.builder(label(name, current.get() ? "ON" : "OFF"), button -> {
            setter.accept(!current.get());
            EssentialsRuntime.save();
            button.setMessage(label(name, current.get() ? "ON" : "OFF"));
        }).bounds(x, y, width, 20).build();
    }

    private Component label(String name, String value) {
        return Component.literal(name + ": " + value);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFF4FAFF);
        graphics.drawCenteredString(font,
                "Presentation settings are live; hardware HUD sources activate as providers become available.",
                width / 2, height - 48, 0xFF91A8BE);
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
