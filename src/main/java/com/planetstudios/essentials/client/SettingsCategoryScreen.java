package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.SettingDefinition;
import com.planetstudios.essentials.config.SettingsRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Native settings page generated from the typed settings registry. */
public final class SettingsCategoryScreen extends Screen {
    private final Screen parent;
    private final String category;

    public SettingsCategoryScreen(Screen parent, String category) {
        super(Component.literal("Performance & Diagnostics"));
        this.parent = parent;
        this.category = category;
    }

    @Override
    protected void init() {
        List<SettingDefinition<?>> settings = SettingsRegistry.inCategory(category);
        int center = width / 2;
        int y = height / 4 + 24;
        for (SettingDefinition<?> setting : settings) {
            addRenderableWidget(createButton(setting, center - 155, y, 310, 20));
            y += 27;
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> minecraft.setScreen(parent))
                .bounds(center - 100, height - 40, 200, 20).build());
    }

    private Button createButton(SettingDefinition<?> setting, int x, int y, int buttonWidth, int buttonHeight) {
        return Button.builder(label(setting), button -> {
            Object current = setting.get(EssentialsRuntime.config());
            if (setting.kind() == SettingDefinition.Kind.BOOLEAN) {
                setting.set(EssentialsRuntime.config(), !((Boolean) current));
            } else if (setting.kind() == SettingDefinition.Kind.INTEGER) {
                int value = (Integer) current + 20;
                if (value > 1200) value = 20;
                setting.set(EssentialsRuntime.config(), value);
            }
            EssentialsRuntime.performance().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
            EssentialsRuntime.save();
            button.setMessage(label(setting));
        }).bounds(x, y, buttonWidth, buttonHeight).build();
    }

    private Component label(SettingDefinition<?> setting) {
        return Component.literal(setting.displayName() + ": " + setting.get(EssentialsRuntime.config()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 4, 0xFFFFFF);
        graphics.drawCenteredString(font, "Select a setting to cycle its available value.",
                width / 2, height / 4 + 48 + SettingsRegistry.inCategory(category).size() * 27, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
