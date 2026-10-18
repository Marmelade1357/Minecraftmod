package de.pf2craft.core.monster;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;

/** JSON for stored NPC instances and for NPC templates (the {@link MonsterParser} format). */
public final class NpcJson {
    private static final Gson GSON = new Gson();

    private NpcJson() {
    }

    public static String write(NpcStats stats) {
        JsonObject o = new JsonObject();
        o.add("base", monster(stats.base()));
        o.addProperty("displayName", stats.displayName());
        o.addProperty("currentHp", stats.currentHp());
        return GSON.toJson(o);
    }

    public static NpcStats read(String json) throws IllegalArgumentException {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            JsonArray wrapped = new JsonArray();
            wrapped.add(o.get("base").getAsJsonObject());
            List<MonsterData> parsed = MonsterParser.parse(GSON.toJson(wrapped));
            String name = o.get("displayName").getAsString();
            int hp = o.get("currentHp").getAsBigDecimal().intValueExact();
            return new NpcStats(parsed.get(0), name, hp);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid NPC JSON: " + e, e);
        }
    }

    /** A one-element array that {@link MonsterParser#parse} reads back to an equal MonsterData. */
    public static String writeTemplate(MonsterData base) {
        JsonArray array = new JsonArray();
        array.add(monster(base));
        return GSON.toJson(array);
    }

    private static JsonObject monster(MonsterData m) {
        JsonObject o = new JsonObject();
        o.addProperty("id", m.id());
        o.addProperty("name", m.name());
        o.addProperty("level", m.level());
        o.addProperty("size", m.size());
        o.add("traits", strings(m.traits()));
        o.addProperty("perception", m.perception());
        o.addProperty("ac", m.ac());
        o.addProperty("fortitude", m.fortitude());
        o.addProperty("reflex", m.reflex());
        o.addProperty("will", m.will());
        o.addProperty("hp", m.hp());
        o.addProperty("speed", m.speed());
        JsonArray strikes = new JsonArray();
        for (MonsterStrike s : m.strikes()) {
            JsonObject so = new JsonObject();
            so.addProperty("name", s.name());
            so.addProperty("attackBonus", s.attackBonus());
            so.addProperty("ranged", s.ranged());
            so.add("traits", strings(s.traits()));
            JsonArray damage = new JsonArray();
            for (DamagePart d : s.damage()) {
                JsonObject d0 = new JsonObject();
                d0.addProperty("dice", d.dice());
                d0.addProperty("bonus", d.bonus());
                d0.addProperty("type", d.type());
                damage.add(d0);
            }
            so.add("damage", damage);
            strikes.add(so);
        }
        o.add("strikes", strikes);
        o.add("resistances", resistances(m.resistances()));
        o.add("weaknesses", resistances(m.weaknesses()));
        o.add("immunities", strings(m.immunities()));
        JsonObject skills = new JsonObject();
        for (Map.Entry<String, Integer> e : m.skills().entrySet()) {
            skills.addProperty(e.getKey(), e.getValue());
        }
        o.add("skills", skills);
        o.add("abilities", strings(m.abilities()));
        return o;
    }

    private static JsonArray strings(List<String> values) {
        JsonArray a = new JsonArray();
        for (String v : values) {
            a.add(v);
        }
        return a;
    }

    private static JsonArray resistances(List<Resistance> values) {
        JsonArray a = new JsonArray();
        for (Resistance r : values) {
            JsonObject o = new JsonObject();
            o.addProperty("type", r.type());
            o.addProperty("value", r.value());
            a.add(o);
        }
        return a;
    }
}
