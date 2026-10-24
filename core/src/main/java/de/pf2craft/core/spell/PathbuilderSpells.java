package de.pf2craft.core.spell;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.Rank;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the spellCasters section of a Pathbuilder 2e build. Never throws on bad data. */
public final class PathbuilderSpells {
    private PathbuilderSpells() {
    }

    /** Reads the spellbook from a whole Pathbuilder response body ({"success":..,"build":{..}}). */
    public static Spellbook parseResponse(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            if (root.isJsonObject() && root.getAsJsonObject().has("build")
                    && root.getAsJsonObject().get("build").isJsonObject()) {
                return parse(root.getAsJsonObject().getAsJsonObject("build"));
            }
        } catch (RuntimeException e) {
            // fall through
        }
        return Spellbook.empty();
    }

    public static Spellbook parse(JsonObject build) {
        try {
            return parseUnsafe(build);
        } catch (RuntimeException e) {
            return Spellbook.empty();
        }
    }

    private static Spellbook parseUnsafe(JsonObject build) {
        if (build == null || !build.has("spellCasters") || !build.get("spellCasters").isJsonArray()) {
            return Spellbook.empty();
        }
        List<SpellcastingEntry> entries = new ArrayList<>();
        int focusFromEntries = 0;
        for (JsonElement el : build.getAsJsonArray("spellCasters")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject c = el.getAsJsonObject();
            String tradition = str(c, "magicTradition");
            CastingType type = CastingType.fromPathbuilder(str(c, "spellcastingType"), tradition);
            entries.add(new SpellcastingEntry(str(c, "name"), tradition, type, ability(c),
                    rank(c), perDay(c), spells(c)));
            if (type == CastingType.FOCUS) {
                focusFromEntries = Math.max(focusFromEntries, integer(c, "focusPoints", 0));
            }
        }
        int focus = build.has("focusPoints") ? integer(build, "focusPoints", 0) : focusFromEntries;
        return new Spellbook(entries, Math.max(0, Math.min(3, focus)));
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    private static int integer(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) {
            return def;
        }
        try {
            return e.getAsInt();
        } catch (RuntimeException ex) {
            return def;
        }
    }

    private static Ability ability(JsonObject c) {
        try {
            return Ability.fromPathbuilder(str(c, "ability"));
        } catch (RuntimeException e) {
            return Ability.CHA;
        }
    }

    private static Rank rank(JsonObject c) {
        try {
            return Rank.fromPathbuilder(integer(c, "proficiency", 0));
        } catch (RuntimeException e) {
            return Rank.UNTRAINED;
        }
    }

    private static int[] perDay(JsonObject c) {
        int[] days = new int[SpellcastingEntry.RANKS];
        JsonElement e = c.get("perDay");
        if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < days.length && i < a.size(); i++) {
                try {
                    days[i] = Math.max(0, a.get(i).getAsInt());
                } catch (RuntimeException ex) {
                    days[i] = 0;
                }
            }
        }
        return days;
    }

    private static Map<Integer, List<String>> spells(JsonObject c) {
        Map<Integer, List<String>> map = new LinkedHashMap<>();
        JsonElement e = c.get("spells");
        if (e == null || !e.isJsonArray()) {
            return map;
        }
        for (JsonElement s : e.getAsJsonArray()) {
            if (!s.isJsonObject()) {
                continue;
            }
            JsonObject o = s.getAsJsonObject();
            int rank = integer(o, "spellLevel", -1);
            if (rank < 0 || rank >= SpellcastingEntry.RANKS || !o.has("list") || !o.get("list").isJsonArray()) {
                continue;
            }
            List<String> names = map.computeIfAbsent(rank, k -> new ArrayList<>());
            for (JsonElement n : o.getAsJsonArray("list")) {
                if (n.isJsonPrimitive()) {
                    names.add(n.getAsString());
                }
            }
        }
        return map;
    }
}
