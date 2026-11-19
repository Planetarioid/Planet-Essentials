package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.rendering.shaders.ShaderPackCatalog;
import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** In-game Prism shader enable/disable and discovered pack selection. */
public final class ShaderSettingsScreen extends Screen {
    private final Screen parent;
    private String status = "Shaders are opt-in. Vanilla rendering is used when disabled.";
    private List<ShaderPackLoader.LoadedPack> packs = List.of();

    public ShaderSettingsScreen(Screen parent) {
        super(Component.literal("Prism Shader Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        packs = ShaderPackRuntime.catalogSnapshot().packs();
        int center = width / 2;
        var config = EssentialsRuntime.config();

        addRenderableWidget(Button.builder(shaderToggleLabel(), button -> {
            config.shadersEnabled = !config.shadersEnabled;
            EssentialsRuntime.save();
            button.setMessage(shaderToggleLabel());
            status = config.shadersEnabled
                    ? "Prism enabled. A selected and successfully compiled pack is required."
                    : "Prism disabled. Vanilla rendering remains active.";
        }).bounds(center - 150, height / 4 + 30, 300, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Select next shader pack"), button -> {
            refreshPacks();
            if (packs.isEmpty()) {
                status = "No valid Prism shader packs were discovered in active resource packs.";
                return;
            }
            List<String> ids = packs.stream().map(pack -> pack.manifest().id()).toList();
            String current = config.selectedShaderPack;
            int index = ids.indexOf(current);
            config.selectedShaderPack = ids.get((index + 1) % ids.size());
            EssentialsRuntime.save();
            ShaderPackRuntime.selectPack(config.selectedShaderPack);
            status = "Selected: " + config.selectedShaderPack + ". Compilation queued.";
        }).bounds(center - 150, height / 4 + 58, 300, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Rescan shader packs"), button -> {
            ShaderPackRuntime.reload(Minecraft.getInstance().getResourceManager());
            refreshPacks();
            status = "Shader-pack rescan and compilation queued.";
        }).bounds(center - 150, height / 4 + 86, 300, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> minecraft.setScreen(parent))
                .bounds(center - 100, height - 42, 200, 20).build());
    }

    private void refreshPacks() {
        packs = ShaderPackRuntime.catalogSnapshot().packs();
    }

    private Component shaderToggleLabel() {
        return Component.literal("Prism shaders: "
                + (EssentialsRuntime.config().shadersEnabled ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        int center = width / 2;
        graphics.drawCenteredString(font, title, center, height / 4, 0xFFFFFF);
        graphics.drawCenteredString(font, "Selected pack: "
                + (EssentialsRuntime.config().selectedShaderPack.isBlank()
                ? "None" : EssentialsRuntime.config().selectedShaderPack),
                center, height / 4 + 118, 0xD8E8FF);
        graphics.drawCenteredString(font, "Discovered packs: " + packs.size(),
                center, height / 4 + 134, 0xAAAAAA);
        graphics.drawCenteredString(font, status, center, height / 4 + 158, 0xAAAAAA);
        graphics.drawCenteredString(font, "Disabling shaders bypasses Prism effects without restarting.",
                center, height / 4 + 178, 0x888888);
    }

    @Override
    public void onClose() {
        EssentialsRuntime.save();
        minecraft.setScreen(parent);
    }
}
