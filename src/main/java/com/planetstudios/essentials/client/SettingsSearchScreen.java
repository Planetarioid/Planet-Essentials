package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.SettingDefinition;
import com.planetstudios.essentials.config.SettingsRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Searchable, categorized index of all registered Planet settings. */
public final class SettingsSearchScreen extends Screen {
    private final Screen parent;
    private EditBox search;
    private int page;
    private static final int PAGE_SIZE = 9;
    private List<SettingDefinition<?>> matches = List.of();
    private final List<Button> resultButtons = new ArrayList<>();

    public SettingsSearchScreen(Screen parent) {
        super(Component.literal("SETTINGS SEARCH"));
        this.parent = parent;
    }

    @Override protected void init() {
        search = new EditBox(font, width / 2 - 150, 48, 300, 20, Component.literal("Search settings"));
        search.setHint(Component.literal("Name, category, description, or keyword..."));
        search.setMaxLength(80);
        search.setResponder(value -> { page = 0; rebuildResults(); });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> search.setValue(""))
                .bounds(width / 2 + 156, 48, 54, 20).build());
        rebuildResults();
        addRenderableWidget(Button.builder(Component.literal("PREVIOUS"), b -> { page=Math.max(0,page-1); rebuildResults(); })
                .bounds(width/2-110,height-34,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("NEXT"), b -> { if((page+1)*PAGE_SIZE<matches.size())page++; rebuildResults(); })
                .bounds(width/2+10,height-34,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), b -> minecraft.setScreen(parent))
                .bounds(width/2-50,height-60,100,20).build());
    }

    private void rebuildResults() {
        matches = SettingsRegistry.all().stream()
                .filter(setting -> matches(setting, search == null ? "" : search.getValue()))
                .sorted(Comparator.comparing(SettingDefinition<?>::category).thenComparing(SettingDefinition::displayName))
                .toList();
        for (Button button : resultButtons) removeWidget(button);
        resultButtons.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        int y = 82;
        for (int i=start;i<end;i++) {
            SettingDefinition<?> setting=matches.get(i);
            String category=setting.category().toUpperCase(Locale.ROOT);
            String value=String.valueOf(setting.get(EssentialsRuntime.config()));
            Button button=Button.builder(Component.literal(category+"  /  "+setting.displayName()+"  ["+value+"]"),
                    ignored -> change(setting)).bounds(width/2-175,y,350,20).build();
            resultButtons.add(button);
            addRenderableWidget(button);
            y+=25;
        }
    }

    private boolean matches(SettingDefinition<?> setting,String raw) {
        String q=raw.trim().toLowerCase(Locale.ROOT);
        if(q.isEmpty())return true;
        String haystack=(setting.id()+" "+setting.category()+" "+setting.displayName()+" "+setting.description()+" "+aliases(setting.id()))
                .toLowerCase(Locale.ROOT);
        for(String token:q.split("\\s+")) if(!haystack.contains(token)) return false;
        return true;
    }

    private String aliases(String id) {
        return switch(id) {
            case "combat.crosshair_target_indicator" -> "hit indicator crosshair target melee range player";
            case "combat.combo_counter" -> "combo hits streak incoming outgoing negative counter";
            case "hud.fps" -> "frames per second frame rate performance";
            case "diagnostics.history_size" -> "history samples buffer capacity";
            case "diagnostics.performance_screen" -> "fps summary overlay";
            case "systems.performance_monitoring" -> "fps sampling frame rate";
            case "systems.event_collection" -> "events logging";
            case "shaders.enabled" -> "prism graphics rendering effects";
            default -> "";
        };
    }

    private void change(SettingDefinition<?> setting) {
        Object current=setting.get(EssentialsRuntime.config());
        if(setting.kind()==SettingDefinition.Kind.BOOLEAN) setting.set(EssentialsRuntime.config(),!((Boolean)current));
        else if(setting.kind()==SettingDefinition.Kind.INTEGER) {
            int value=(Integer)current+20;
            if(value>1200)value=20;
            setting.set(EssentialsRuntime.config(),value);
        }
        EssentialsRuntime.performance().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
        EssentialsRuntime.diagnosticEvents().setCapacity(EssentialsRuntime.config().diagnosticsHistorySize);
        EssentialsRuntime.save();
        rebuildResults();
    }

    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick);
        graphics.drawCenteredString(font,title,width/2,24,0xFFF4FAFF);
        graphics.drawCenteredString(font,matches.size()+" matching settings  •  Search names, categories, descriptions, and aliases",
                width/2,73,0xFF8FAFC8);
        graphics.drawCenteredString(font,matches.isEmpty()?"No settings found. Try a broader search.":
                "Page "+(page+1)+" / "+Math.max(1,(matches.size()+PAGE_SIZE-1)/PAGE_SIZE),
                width/2,height-45,0xFF8FAFC8);
    }

    @Override public void onClose(){minecraft.setScreen(parent);}
}
