package de.pf2craft.core.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict parser for the item catalog JSON files (weapons, armor, shields). */
public final class ItemCatalogParser {
    private static final Set<String> WEAPON_CATEGORIES = Set.of("simple", "martial", "advanced", "unarmed");
    private static final Set<String> ARMOR_CATEGORIES = Set.of("unarmored", "light", "medium", "heavy");
    private static final Set<String> DAMAGE_TYPES = Set.of("B", "P", "S");
    private static final Set<String> DICE = Set.of("d4", "d6", "d8", "d10", "d12");

    private ItemCatalogParser() {
    }

    public static List<WeaponData> weapons(String json) throws IllegalArgumentException {
        List<WeaponData> out = new ArrayList<>();
        for (JsonObject o : entries(json)) {
            String id = id(o);
            out.add(new WeaponData(id, string(o, id, "name"),
                    oneOf(o, id, "category", WEAPON_CATEGORIES), oneOf(o, id, "die", DICE),
                    oneOf(o, id, "damageType", DAMAGE_TYPES), string(o, id, "group"),
                    number(o, id, "hands"), number(o, id, "range"), strings(o, id, "traits")));
        }
        return out;
    }

    public static List<ArmorData> armor(String json) throws IllegalArgumentException {
        List<ArmorData> out = new ArrayList<>();
        for (JsonObject o : entries(json)) {
            String id = id(o);
            out.add(new ArmorData(id, string(o, id, "name"), oneOf(o, id, "category", ARMOR_CATEGORIES),
                    number(o, id, "acBonus"), number(o, id, "dexCap"), number(o, id, "checkPenalty"),
                    number(o, id, "speedPenalty"), number(o, id, "strength"), strings(o, id, "traits")));
        }
        return out;
    }

    public static List<ShieldData> shields(String json) throws IllegalArgumentException {
        List<ShieldData> out = new ArrayList<>();
        for (JsonObject o : entries(json)) {
            String id = id(o);
            out.add(new ShieldData(id, string(o, id, "name"), number(o, id, "acBonus"),
                    number(o, id, "hardness"), number(o, id, "hitPoints"),
                    number(o, id, "brokenThreshold"), number(o, id, "speedPenalty")));
        }
        return out;
    }

    /** Parses the array and checks that ids are present and unique. */
    private static List<JsonObject> entries(String json) {
        JsonArray array;
        try {
            array = JsonParser.parseString(json).getAsJsonArray();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid item JSON: " + e, e);
        }
        List<JsonObject> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("Item entry is not an object: " + el);
            }
            JsonObject o = el.getAsJsonObject();
            String id = id(o);
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate item id '" + id + "'");
            }
            out.add(o);
        }
        return out;
    }

    private static String id(JsonObject o) {
        JsonElement el = o.get("id");
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()
                || el.getAsString().isBlank()) {
            throw new IllegalArgumentException("Item entry without valid 'id': " + o);
        }
        return el.getAsString();
    }

    private static String string(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()
                || el.getAsString().isBlank()) {
            throw new IllegalArgumentException("Item '" + id + "': field '" + field
                    + "' missing or not a string");
        }
        return el.getAsString();
    }

    private static String oneOf(JsonObject o, String id, String field, Set<String> allowed) {
        String value = string(o, id, field);
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException("Item '" + id + "': field '" + field
                    + "' has unknown value '" + value + "'");
        }
        return value;
    }

    private static int number(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Item '" + id + "': field '" + field
                    + "' missing or not a number");
        }
        int value;
        try {
            value = el.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Item '" + id + "': field '" + field
                    + "' is not an integer");
        }
        if (value < 0) {
            throw new IllegalArgumentException("Item '" + id + "': field '" + field + "' is negative");
        }
        return value;
    }

    private static List<String> strings(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonArray()) {
            throw new IllegalArgumentException("Item '" + id + "': field '" + field
                    + "' missing or not an array");
        }
        List<String> out = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Item '" + id + "': field '" + field
                        + "' contains a non-string");
            }
            out.add(e.getAsString());
        }
        return List.copyOf(out);
    }
}
