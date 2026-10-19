package de.pf2craft.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.pf2craft.Pf2Craft;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client state of the action menu: the action waiting for a target (mirrors the server) and the favorite
 * actions, which are stored in {@code config/pf2craft-client.json}.
 */
public final class ClientActions {
    private static final Gson GSON = new Gson();
    private static final String FAVORITES = "favorites";

    private static final String COMBAT_CHAT_SCALE = "combatChatScale";
    private static final String COMBAT_CHAT_HEIGHT = "combatChatHeight";
    private static final String HUD_ANCHOR = "hudAnchor";
    private static final double MIN_FACTOR = 0.3;
    private static final double MAX_FACTOR = 1.0;

    /** Where the combat HUD text sits: right (default), center or left. */
    public enum HudAnchor { RIGHT, CENTER, LEFT }

    private static Set<String> favorites;
    private static double combatChatScale = 0.7;
    private static double combatChatHeight = 0.6;
    private static HudAnchor hudAnchor = HudAnchor.RIGHT;
    private static boolean settingsLoaded;
    private static String pending = "";

    private ClientActions() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pf2craft-client.json");
    }

    /** Chat text scale factor while a combat runs (config key {@code combatChatScale}, 0.3..1.0). */
    public static double combatChatScale() {
        loadSettings();
        return combatChatScale;
    }

    /** Open-chat height factor while a combat runs (config key {@code combatChatHeight}, 0.3..1.0). */
    public static double combatChatHeight() {
        loadSettings();
        return combatChatHeight;
    }

    /** Position of the combat HUD text (config key {@code hudAnchor}). */
    public static HudAnchor hudAnchor() {
        loadSettings();
        return hudAnchor;
    }

    private static void loadSettings() {
        if (settingsLoaded) {
            return;
        }
        settingsLoaded = true;
        try {
            Path file = file();
            if (Files.isRegularFile(file)) {
                JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
                if (root != null) {
                    combatChatScale = factor(root.get(COMBAT_CHAT_SCALE), combatChatScale);
                    combatChatHeight = factor(root.get(COMBAT_CHAT_HEIGHT), combatChatHeight);
                    JsonElement anchor = root.get(HUD_ANCHOR);
                    if (anchor != null && anchor.isJsonPrimitive()) {
                        for (HudAnchor candidate : HudAnchor.values()) {
                            if (candidate.name().equalsIgnoreCase(anchor.getAsString())) {
                                hudAnchor = candidate;
                            }
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.warn("Could not read the client config", e);
        }
    }

    private static double factor(JsonElement e, double fallback) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        double v = e.getAsDouble();
        return Double.isNaN(v) ? fallback : Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, v));
    }

    /** The favorite action ids, in the order they were added (read from disk on first use). */
    public static Set<String> favorites() {
        if (favorites == null) {
            favorites = load();
        }
        return Collections.unmodifiableSet(favorites);
    }

    public static void toggleFavorite(String id) {
        favorites();
        if (!favorites.remove(id)) {
            favorites.add(id);
        }
        save();
    }

    private static Set<String> load() {
        Set<String> result = new LinkedHashSet<>();
        try {
            Path file = file();
            if (Files.isRegularFile(file)) {
                JsonObject root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
                JsonElement list = root == null ? null : root.get(FAVORITES);
                if (list != null && list.isJsonArray()) {
                    for (JsonElement e : list.getAsJsonArray()) {
                        if (e.isJsonPrimitive()) {
                            result.add(e.getAsString());
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.warn("Could not read the client config", e);
        }
        return result;
    }

    private static void save() {
        try {
            JsonArray list = new JsonArray();
            favorites.forEach(list::add);
            Path file = file();
            JsonObject root = null;
            try {
                if (Files.isRegularFile(file)) {
                    root = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
                }
            } catch (IOException | RuntimeException e) {
                root = null; // unreadable or not an object: start fresh
            }
            if (root == null) {
                root = new JsonObject();
            }
            root.add(FAVORITES, list);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.warn("Could not write the client config", e);
        }
    }

    /** The action waiting for a target click, or "" for none. */
    public static String pending() {
        return pending;
    }

    public static void setPending(String id) {
        pending = id == null ? "" : id;
    }
}
