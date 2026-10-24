package de.pf2craft.core.spell;

import com.google.gson.Gson;
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

/** Serializes a {@link Spellbook} including used slots and focus points. */
public final class SpellbookJson {
    private static final Gson GSON = new Gson();

    private SpellbookJson() {
    }

    public static String write(Spellbook book) {
        JsonObject root = new JsonObject();
        root.addProperty("focusMax", book.focusMax());
        root.addProperty("focusCurrent", book.focusCurrent());
        JsonArray arr = new JsonArray();
        for (int i = 0; i < book.entries().size(); i++) {
            SpellcastingEntry e = book.entries().get(i);
            JsonObject o = new JsonObject();
            o.addProperty("name", e.name());
            o.addProperty("tradition", e.tradition());
            o.addProperty("type", e.type().name());
            o.addProperty("ability", e.ability().name());
            o.addProperty("rank", e.rank().name());
            JsonArray days = new JsonArray();
            JsonArray used = new JsonArray();
            int[] perDay = e.perDay();
            for (int r = 0; r < SpellcastingEntry.RANKS; r++) {
                days.add(perDay[r]);
                used.add(book.slotsUsed(i, r));
            }
            o.add("perDay", days);
            o.add("used", used);
            JsonObject spells = new JsonObject();
            e.spellsByRank().forEach((rank, names) -> spells.add(String.valueOf(rank), GSON.toJsonTree(names)));
            o.add("spells", spells);
            arr.add(o);
        }
        root.add("entries", arr);
        return GSON.toJson(root);
    }

    /** Parses what {@link #write} produced; anything unreadable yields an empty book. */
    public static Spellbook read(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Spellbook.empty();
            }
            JsonObject root = parsed.getAsJsonObject();
            List<SpellcastingEntry> entries = new ArrayList<>();
            List<int[]> usedRows = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("entries")) {
                JsonObject o = el.getAsJsonObject();
                Map<Integer, List<String>> spells = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> s : o.getAsJsonObject("spells").entrySet()) {
                    List<String> names = new ArrayList<>();
                    for (JsonElement n : s.getValue().getAsJsonArray()) {
                        names.add(n.getAsString());
                    }
                    spells.put(Integer.parseInt(s.getKey()), names);
                }
                entries.add(new SpellcastingEntry(o.get("name").getAsString(), o.get("tradition").getAsString(),
                        CastingType.valueOf(o.get("type").getAsString()),
                        Ability.valueOf(o.get("ability").getAsString()),
                        Rank.valueOf(o.get("rank").getAsString()), ints(o.getAsJsonArray("perDay")), spells));
                usedRows.add(ints(o.getAsJsonArray("used")));
            }
            return new Spellbook(entries, root.get("focusMax").getAsInt(), root.get("focusCurrent").getAsInt(),
                    usedRows.toArray(new int[0][]));
        } catch (RuntimeException e) {
            return Spellbook.empty();
        }
    }

    private static int[] ints(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsInt();
        }
        return out;
    }
}
