package de.pf2craft.core.progression;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * What a character gets when advancing to {@code newLevel}. {@code featSlots} lists the slot
 * types in the order the chosen feat names must be supplied. {@code hpGain} is a preview
 * computed before ability boosts.
 */
public record LevelUpOptions(int newLevel, boolean skillIncrease, int abilityBoosts,
                             List<String> featSlots, List<String> features, int hpGain) {
    private static final Gson GSON = new Gson();

    public LevelUpOptions {
        featSlots = List.copyOf(featSlots);
        features = List.copyOf(features);
    }

    /** Serializes the options so the server can send them to the client. */
    public String toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("newLevel", newLevel);
        o.addProperty("skillIncrease", skillIncrease);
        o.addProperty("abilityBoosts", abilityBoosts);
        o.add("featSlots", array(featSlots));
        o.add("features", array(features));
        o.addProperty("hpGain", hpGain);
        return GSON.toJson(o);
    }

    public static LevelUpOptions fromJson(String json) throws IllegalArgumentException {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            return new LevelUpOptions(o.get("newLevel").getAsInt(),
                    o.get("skillIncrease").getAsBoolean(), o.get("abilityBoosts").getAsInt(),
                    strings(o.getAsJsonArray("featSlots")), strings(o.getAsJsonArray("features")),
                    o.get("hpGain").getAsInt());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid level-up options: " + e, e);
        }
    }

    private static JsonArray array(List<String> values) {
        JsonArray a = new JsonArray();
        values.forEach(a::add);
        return a;
    }

    private static List<String> strings(JsonArray a) {
        List<String> out = new ArrayList<>();
        for (JsonElement el : a) {
            out.add(el.getAsString());
        }
        return out;
    }
}
