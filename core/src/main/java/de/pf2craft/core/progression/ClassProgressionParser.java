package de.pf2craft.core.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pf2craft.core.character.Rank;
import de.pf2craft.core.character.Skill;
import de.pf2craft.core.progression.ClassProgression.LevelEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Parses the class JSON format (see resources/pf2craft/classes). */
public final class ClassProgressionParser {
    private static final Set<String> KNOWN_KEYS = new HashSet<>(List.of(
            "perception", "fortitude", "reflex", "will", "class_dc", "unarmored", "light", "medium",
            "heavy", "simple", "martial", "advanced", "unarmed"));

    static {
        for (Skill skill : Skill.values()) {
            KNOWN_KEYS.add(skill.id());
        }
    }

    private ClassProgressionParser() {
    }

    public static ClassProgression parse(String json) throws IllegalArgumentException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String name = root.get("name").getAsString();
            int hp = root.get("hpPerLevel").getAsInt();
            Map<Integer, LevelEntry> levels = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("levels").entrySet()) {
                int level = Integer.parseInt(e.getKey());
                if (level < 1 || level > 20) {
                    throw new IllegalArgumentException("Level out of range: " + level);
                }
                levels.put(level, parseLevel(level, e.getValue().getAsJsonObject()));
            }
            return new ClassProgression(name, hp, levels);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid class JSON: " + e, e);
        }
    }

    private static LevelEntry parseLevel(int level, JsonObject o) {
        Map<String, Rank> prof = new HashMap<>();
        if (o.has("proficiencies")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("proficiencies").entrySet()) {
                if (!KNOWN_KEYS.contains(e.getKey())) {
                    throw new IllegalArgumentException(
                            "Unknown proficiency key '" + e.getKey() + "' at level " + level);
                }
                prof.put(e.getKey(), Rank.valueOf(e.getValue().getAsString().toUpperCase(Locale.ROOT)));
            }
        }
        boolean increase = false;
        if (o.has("skillIncrease")) {
            JsonElement el = o.get("skillIncrease");
            if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException("skillIncrease must be a boolean at level " + level);
            }
            increase = el.getAsBoolean();
        }
        return new LevelEntry(prof, strings(o, "features"), increase, strings(o, "featSlots"));
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key)) {
            for (JsonElement el : o.getAsJsonArray(key)) {
                out.add(el.getAsString());
            }
        }
        return out;
    }
}
