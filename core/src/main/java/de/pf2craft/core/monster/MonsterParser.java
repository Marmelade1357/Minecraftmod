package de.pf2craft.core.monster;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict parser for monster catalog JSON. All fields are required. */
public final class MonsterParser {
    private static final Set<String> SIZES =
            Set.of("tiny", "small", "medium", "large", "huge", "gargantuan");
    private static final Set<String> DAMAGE_TYPES = Set.of("bludgeoning", "piercing", "slashing", "fire",
            "cold", "electricity", "acid", "sonic", "vitality", "void", "force", "spirit", "mental",
            "poison", "bleed", "precision", "untyped");
    private static final Pattern DICE = Pattern.compile("^[1-9]\\d{0,2}d(4|6|8|10|12)$");

    private MonsterParser() {
    }

    public static List<MonsterData> parse(String json) throws IllegalArgumentException {
        JsonArray array;
        try {
            array = JsonParser.parseString(json).getAsJsonArray();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid monster JSON: " + e, e);
        }
        List<MonsterData> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) {
                throw new IllegalArgumentException("Monster entry is not an object: " + el);
            }
            JsonObject o = el.getAsJsonObject();
            String id = id(o);
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate monster id '" + id + "'");
            }
            out.add(monster(o, id));
        }
        return out;
    }

    private static MonsterData monster(JsonObject o, String id) {
        int level = number(o, id, "level");
        if (level < -1 || level > 25) {
            throw new IllegalArgumentException("Monster '" + id + "': field 'level' out of range -1..25");
        }
        List<MonsterStrike> strikes = new ArrayList<>();
        for (JsonObject s : objects(o, id, "strikes")) {
            strikes.add(strike(s, id));
        }
        return new MonsterData(id, string(o, id, "name"), level, oneOf(o, id, "size", SIZES),
                strings(o, id, "traits"), number(o, id, "perception"), nonNegative(o, id, "ac"),
                number(o, id, "fortitude"), number(o, id, "reflex"), number(o, id, "will"),
                nonNegative(o, id, "hp"), nonNegative(o, id, "speed"), List.copyOf(strikes),
                resistances(o, id, "resistances"), resistances(o, id, "weaknesses"),
                strings(o, id, "immunities"), skills(o, id), strings(o, id, "abilities"));
    }

    private static MonsterStrike strike(JsonObject s, String id) {
        List<DamagePart> damage = new ArrayList<>();
        for (JsonObject d : objects(s, id, "damage")) {
            String dice = rawString(d, id, "dice");
            if (!dice.isEmpty() && !DICE.matcher(dice).matches()) {
                throw new IllegalArgumentException("Monster '" + id + "': field 'dice' is malformed: '"
                        + dice + "'");
            }
            damage.add(new DamagePart(dice, number(d, id, "bonus"), oneOf(d, id, "type", DAMAGE_TYPES)));
        }
        return new MonsterStrike(string(s, id, "name"), number(s, id, "attackBonus"), List.copyOf(damage),
                strings(s, id, "traits"), bool(s, id, "ranged"));
    }

    private static List<Resistance> resistances(JsonObject o, String id, String field) {
        List<Resistance> out = new ArrayList<>();
        for (JsonObject r : objects(o, id, field)) {
            out.add(new Resistance(string(r, id, "type"), number(r, id, "value")));
        }
        return List.copyOf(out);
    }

    private static Map<String, Integer> skills(JsonObject o, String id) {
        JsonElement el = o.get("skills");
        if (el == null || !el.isJsonObject()) {
            throw new IllegalArgumentException("Monster '" + id + "': field 'skills' missing or not an object");
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
            out.put(e.getKey(), intValue(e.getValue(), id, "skills." + e.getKey()));
        }
        return Map.copyOf(out);
    }

    private static String id(JsonObject o) {
        JsonElement el = o.get("id");
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()
                || el.getAsString().isBlank()) {
            throw new IllegalArgumentException("Monster entry without valid 'id': " + o);
        }
        return el.getAsString();
    }

    private static String rawString(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' missing or not a string");
        }
        return el.getAsString();
    }

    private static String string(JsonObject o, String id, String field) {
        String value = rawString(o, id, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field + "' is blank");
        }
        return value;
    }

    private static String oneOf(JsonObject o, String id, String field, Set<String> allowed) {
        String value = string(o, id, field);
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' has unknown value '" + value + "'");
        }
        return value;
    }

    private static int number(JsonObject o, String id, String field) {
        return intValue(o.get(field), id, field);
    }

    private static int nonNegative(JsonObject o, String id, String field) {
        int value = number(o, id, field);
        if (value < 0) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field + "' is negative");
        }
        return value;
    }

    private static int intValue(JsonElement el, String id, String field) {
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' missing or not a number");
        }
        try {
            return el.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' is not an integer");
        }
    }

    private static boolean bool(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' missing or not a boolean");
        }
        return el.getAsBoolean();
    }

    private static List<JsonObject> objects(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonArray()) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' missing or not an array");
        }
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            if (!e.isJsonObject()) {
                throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                        + "' contains a non-object");
            }
            out.add(e.getAsJsonObject());
        }
        return out;
    }

    private static List<String> strings(JsonObject o, String id, String field) {
        JsonElement el = o.get(field);
        if (el == null || !el.isJsonArray()) {
            throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                    + "' missing or not an array");
        }
        List<String> out = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Monster '" + id + "': field '" + field
                        + "' contains a non-string");
            }
            out.add(e.getAsString());
        }
        return List.copyOf(out);
    }
}
