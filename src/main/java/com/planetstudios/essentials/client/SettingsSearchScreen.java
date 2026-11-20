package com.planetstudios.essentials.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.config.SettingDefinition;
import com.planetstudios.essentials.config.SettingsRegistry;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Unified Planet settings index. Minecraft options use their real OptionInstance controls inside this screen. */
public final class SettingsSearchScreen extends Screen {
    private sealed interface Result permits PlanetResult, MinecraftResult {
        String category();
        String name();
        String searchText();
    }
    private record PlanetResult(SettingDefinition<?> setting) implements Result {
        public String category() { return setting.category(); }
        public String name() { return setting.displayName(); }
        public String searchText() { return (setting.id()+" "+setting.category()+" "+setting.displayName()+" "+setting.description()+" "+aliases(setting.id())).toLowerCase(Locale.ROOT); }
    }
    private record MinecraftResult(VanillaSettingsRegistry.Entry entry) implements Result {
        public String category() { return entry.category(); }
        public String name() { return entry.name(); }
        public String searchText() { return entry.searchText(); }
    }

    private final Screen parent;
    private EditBox search;
    private int page;
    private static final int PAGE_SIZE = 7;
    private List<Result> matches = List.of();
    private final List<AbstractWidget> resultWidgets = new ArrayList<>();

    public SettingsSearchScreen(Screen parent) {
        super(Component.literal("PLANET SETTINGS"));
        this.parent = parent;
    }

    @Override protected void init() {
        search = new EditBox(font, width / 2 - 150, 42, 300, 20, Component.literal("Search settings"));
        search.setHint(Component.literal("Search names, categories, tags, or keywords..."));
        search.setMaxLength(100);
        search.setResponder(value -> { page = 0; rebuildResults(); });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("CLEAR"), b -> search.setValue(""))
                .bounds(width / 2 + 156, 42, 54, 20).build());
        rebuildResults();
        addRenderableWidget(Button.builder(Component.literal("PREVIOUS"), b -> { page=Math.max(0,page-1); rebuildResults(); })
                .bounds(width/2-110,height-32,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("NEXT"), b -> { if((page+1)*PAGE_SIZE<matches.size())page++; rebuildResults(); })
                .bounds(width/2+10,height-32,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("BACK"), b -> minecraft.setScreen(parent))
                .bounds(width/2-50,height-58,100,20).build());
    }

    private void rebuildResults() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Result> all = new ArrayList<>();
        for (SettingDefinition<?> setting : SettingsRegistry.all()) all.add(new PlanetResult(setting));
        for (VanillaSettingsRegistry.Entry entry : VanillaSettingsRegistry.all(minecraft)) all.add(new MinecraftResult(entry));
        matches = all.stream().filter(result -> matchesQuery(result.searchText(), query))
                .sorted(Comparator.comparing(Result::category).thenComparing(Result::name)).toList();
        for (AbstractWidget widget : resultWidgets) removeWidget(widget);
        resultWidgets.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(matches.size(), start + PAGE_SIZE);
        int y = 75;
        for (int i=start;i<end;i++) {
            Result result = matches.get(i);
            if (result instanceof MinecraftResult vanilla) {
                AbstractWidget control = vanilla.entry().option().createButton(minecraft.options, width/2-165, y, 330);
                resultWidgets.add(control);
                addRenderableWidget(control);
            } else if (result instanceof PlanetResult planet) {
                SettingDefinition<?> setting = planet.setting();
                Button control = Button.builder(Component.literal(result.category().toUpperCase(Locale.ROOT)+"  /  "+result.name()+"  ["+setting.get(EssentialsRuntime.config())+"]"),
                        ignored -> change(setting)).bounds(width/2-165,y,330,20).build();
                resultWidgets.add(control);
                addRenderableWidget(control);
            }
            y += 27;
        }
    }

    private static boolean matchesQuery(String haystack, String query) {
        if (query.isEmpty()) return true;
        for (String token : query.split("\\s+")) if (!haystack.contains(token)) return false;
        return true;
    }

    private static String aliases(String id) {
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
        graphics.drawCenteredString(font,title,width/2,20,0xFFF4FAFF);
        graphics.drawCenteredString(font,matches.size()+" unified settings  •  Planet + Minecraft",
                width/2,66,0xFF8FAFC8);
        graphics.drawCenteredString(font,matches.isEmpty()?"No settings found. Try another keyword.":
                "Page "+(page+1)+" / "+Math.max(1,(matches.size()+PAGE_SIZE-1)/PAGE_SIZE),
                width/2,height-43,0xFF8FAFC8);
    }

    @Override public void onClose(){minecraft.setScreen(parent);}

    @Override public void removed() {
        if (minecraft != null && minecraft.options != null) minecraft.options.save();
        super.removed();
    }
}
