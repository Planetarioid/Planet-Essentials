package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Client-style Prism shader control panel. */
public final class ShaderSettingsScreen extends Screen {
    private final Screen parent;
    private String status = "Prism is opt-in. Vanilla rendering remains the fallback.";
    private List<ShaderPackLoader.LoadedPack> packs = List.of();

    public ShaderSettingsScreen(Screen parent) {
        super(Component.literal("PRISM  /  SHADERS"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        packs = ShaderPackRuntime.catalogSnapshot().packs();
        int center = width / 2;
        int top = Math.max(76, height / 4);
        int w = Math.min(320, width - 48);
        int x = center - w / 2;
        var config = EssentialsRuntime.config();

        addRenderableWidget(Button.builder(toggleLabel(), button -> {
            config.shadersEnabled = !config.shadersEnabled;
            EssentialsRuntime.save();
            button.setMessage(toggleLabel());
            status = config.shadersEnabled ? "Prism enabled. A valid selected pack is required."
                    : "Prism disabled. Vanilla rendering remains active.";
        }).bounds(x, top + 28, w, 22).build());

        addRenderableWidget(Button.builder(Component.literal("CYCLE ACTIVE SHADER PACK"), button -> {
            refreshPacks();
            if (packs.isEmpty()) {
                status = "No valid Prism packs found in active resource packs.";
                return;
            }
            List<String> ids = packs.stream().map(pack -> pack.manifest().id()).toList();
            int index = ids.indexOf(config.selectedShaderPack);
            config.selectedShaderPack = ids.get((index + 1) % ids.size());
            EssentialsRuntime.save();
            ShaderPackRuntime.selectPack(config.selectedShaderPack);
            status = "Selected " + config.selectedShaderPack + ". Compilation queued.";
        }).bounds(x, top + 56, w, 22).build());

        addRenderableWidget(Button.builder(Component.literal("RESCAN RESOURCE PACKS"), button -> {
            ShaderPackRuntime.reload(Minecraft.getInstance().getResourceManager());
            refreshPacks();
            status = "Pack discovery and compilation queued.";
        }).bounds(x, top + 84, w, 22).build());

        addRenderableWidget(Button.builder(Component.literal("BACK"), button -> minecraft.setScreen(parent))
                .bounds(center - 65, height - 42, 130, 20).build());
    }

    private void refreshPacks() { packs = ShaderPackRuntime.catalogSnapshot().packs(); }

    private Component toggleLabel() {
        return Component.literal("PRISM SHADERS  /  "
                + (EssentialsRuntime.config().shadersEnabled ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int center = width / 2;
        int top = Math.max(76, height / 4);
        int panelWidth = Math.min(380, width - 24);
        int left = center - panelWidth / 2;
        graphics.fill(0, 0, width, height, 0xB5080D18);
        graphics.fill(left, top - 28, left + panelWidth, top + 210, 0xEE111B2B);
        graphics.fill(left, top - 28, left + panelWidth, top - 25, 0xFF55C7FF);
        graphics.drawCenteredString(font, title, center, top - 12, 0xFFF4FAFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, "ACTIVE PACK", center, top + 116, 0xFF8FAFC8);
        graphics.drawCenteredString(font, EssentialsRuntime.config().selectedShaderPack.isBlank()
                ? "NONE SELECTED" : EssentialsRuntime.config().selectedShaderPack,
                center, top + 130, 0xFFD8E8FF);
        graphics.drawCenteredString(font, "DISCOVERED  " + packs.size(), center, top + 148, 0xFF8FAFC8);
        graphics.drawCenteredString(font, status, center, top + 170, 0xFFB5C5D8);
        String failure = ShaderPackRuntime.pipelineSnapshot().lastFailure();
        if (!failure.isBlank()) {
            graphics.drawCenteredString(font, "COMPILE ERROR  " + failure, center, top + 190, 0xFFFF7777);
        }
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
