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

    private static Set<String> favorites;
    private static String pending = "";

    private ClientActions() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pf2craft-client.json");
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
            JsonObject root = new JsonObject();
            root.add(FAVORITES, list);
            Path file = file();
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
