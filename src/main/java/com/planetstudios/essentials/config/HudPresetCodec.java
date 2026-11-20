package com.planetstudios.essentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

/** Validated, portable JSON clipboard format for sharing HUD layouts. */
public final class HudPresetCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type PLACEMENT_MAP = new TypeToken<Map<String, HudWidgetPlacement>>() {}.getType();

    private HudPresetCodec() {}

    public static String encode(Map<String, HudWidgetPlacement> placements) {
        if (placements == null) throw new NullPointerException("placements");
        JsonObject root = new JsonObject();
        root.addProperty("format", "space-client-hud");
        root.addProperty("version", 1);
        root.add("placements", GSON.toJsonTree(placements));
        return GSON.toJson(root);
    }

    public static Map<String, HudWidgetPlacement> decode(String source) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Clipboard does not contain a HUD preset");
        try {
            JsonObject root = JsonParser.parseString(source).getAsJsonObject();
            if (!root.has("format") || !"space-client-hud".equals(root.get("format").getAsString())
                    || !root.has("version") || root.get("version").getAsInt() != 1
                    || !root.has("placements") || !root.get("placements").isJsonObject()) {
                throw new IllegalArgumentException("This is not a supported Space Client HUD preset");
            }
            Map<String, HudWidgetPlacement> decoded = GSON.fromJson(root.get("placements"), PLACEMENT_MAP);
            if (decoded == null) throw new IllegalArgumentException("HUD preset has no placements");
            Map<String, HudWidgetPlacement> safe = new TreeMap<>();
            decoded.forEach((id, placement) -> {
                if (id != null && id.matches("[a-z0-9_.-]+:[a-z0-9_.-]+") && placement != null) {
                    placement.validate();
                    safe.put(id, placement);
                }
            });
            if (safe.isEmpty()) throw new IllegalArgumentException("HUD preset contains no valid widgets");
            return safe;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Could not read HUD preset JSON", exception);
        }
    }
}
