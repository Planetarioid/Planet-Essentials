package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.EssentialsConfig;
import com.planetstudios.essentials.config.ProfileStore;
import com.planetstudios.essentials.config.ServerProfileManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** UI for named profiles and optional automatic per-server profile switching. */
public final class ProfilesScreen extends Screen {
    private final Screen parent;
    private EditBox nameField;
    private String selected = "default";
    private String status = "Profiles contain Space Client settings only.";
    private int profileCount;

    public ProfilesScreen(Screen parent) {
        super(Component.literal("Configuration Profiles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        List<String> profiles = EssentialsRuntime.profiles().listProfiles();
        profileCount = profiles.size();
        selected = profiles.contains(EssentialsRuntime.config().activeProfile)
                ? EssentialsRuntime.config().activeProfile : "default";

        nameField = new EditBox(font, center - 100, height / 4 + 24, 200, 20, Component.literal("Profile name"));
        nameField.setMaxLength(32);
        nameField.setValue(selected);
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(Component.literal("Create / Save Profile"), button -> {
            try {
                String name = ProfileStore.normalizeName(nameField.getValue());
                EssentialsRuntime.config().activeProfile = name;
                EssentialsRuntime.profiles().save(name, EssentialsRuntime.config());
                EssentialsRuntime.save();
                selected = name;
                profileCount = EssentialsRuntime.profiles().listProfiles().size();
                status = "Saved profile: " + name;
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        }).bounds(center - 100, height / 4 + 54, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Load Selected Profile"), button -> {
            try {
                EssentialsConfig loaded = EssentialsRuntime.profiles().load(selected);
                loaded.activeProfile = selected;
                EssentialsRuntime.replaceConfig(loaded);
                EssentialsRuntime.save();
                status = "Loaded profile: " + selected;
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
        }).bounds(center - 100, height / 4 + 80, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Select Next Profile"), button -> {
            List<String> currentProfiles = EssentialsRuntime.profiles().listProfiles();
            if (!currentProfiles.isEmpty()) {
                int index = currentProfiles.indexOf(selected);
                selected = currentProfiles.get((index + 1 + currentProfiles.size()) % currentProfiles.size());
                nameField.setValue(selected);
                status = "Selected: " + selected;
            }
        }).bounds(center - 100, height / 4 + 106, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Delete Selected Profile"), button -> {
            if ("default".equals(selected)) {
                status = "The default profile cannot be deleted.";
                return;
            }
            if (EssentialsRuntime.profiles().delete(selected)) {
                selected = "default";
                nameField.setValue(selected);
                profileCount = EssentialsRuntime.profiles().listProfiles().size();
                status = "Deleted profile.";
            } else status = "Profile did not exist.";
        }).bounds(center - 100, height / 4 + 132, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("LINK THIS SERVER TO PROFILE"), button -> {
            if (ServerProfileManager.linkCurrentServer(minecraft, selected)) {
                status = "This server will use profile: " + selected;
            } else {
                status = "Join a multiplayer server before linking a profile.";
            }
        }).bounds(center - 100, height / 4 + 158, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal(autoProfileLabel()), button -> {
            var config = EssentialsRuntime.config();
            config.automaticServerProfilesEnabled = !config.automaticServerProfilesEnabled;
            EssentialsRuntime.save();
            button.setMessage(Component.literal(autoProfileLabel()));
            status = "Automatic server profiles " + (config.automaticServerProfilesEnabled ? "enabled." : "disabled.");
        }).bounds(center - 100, height / 4 + 184, 200, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Back"), button -> minecraft.setScreen(parent))
                .bounds(center - 100, height - 40, 200, 20).build());
    }

    private static String autoProfileLabel() {
        return "AUTO SWITCH: " + (EssentialsRuntime.config().automaticServerProfilesEnabled ? "ON" : "OFF");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 4, 0xFFFFFF);
        graphics.drawCenteredString(font, "Selected: " + selected, width / 2, height / 4 + 210, 0xD8E8FF);
        graphics.drawCenteredString(font, status, width / 2, height / 4 + 226, 0xAAAAAA);
        graphics.drawCenteredString(font, "Profiles found: " + profileCount + "  |  Server mappings: "
                + EssentialsRuntime.config().serverProfileMappings.size(),
                width / 2, height / 4 + 242, 0x888888);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
