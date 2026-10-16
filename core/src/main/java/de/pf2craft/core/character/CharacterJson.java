package de.pf2craft.core.character;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;

/** Stores character sheets as JSON (explicit field mapping, format version 1). */
public final class CharacterJson {
    private static final int VERSION = 1;
    private static final Gson GSON = new Gson();

    private CharacterJson() {
    }

    public static String write(CharacterSheet s) {
        JsonObject o = new JsonObject();
        o.addProperty("version", VERSION);
        o.addProperty("name", s.name());
        o.addProperty("className", s.className());
        o.addProperty("ancestry", s.ancestry());
        o.addProperty("heritage", s.heritage());
        o.addProperty("background", s.background());
        o.addProperty("level", s.level());
        o.addProperty("keyAbility", s.keyAbility().name());
        JsonObject scores = new JsonObject();
        for (Map.Entry<Ability, Integer> e : s.abilityScores().entrySet()) {
            scores.addProperty(e.getKey().name(), e.getValue());
        }
        o.add("abilityScores", scores);
        JsonObject prof = new JsonObject();
        new TreeMap<>(s.proficiencies()).forEach((k, v) -> prof.addProperty(k, v.name()));
        o.add("proficiencies", prof);
        JsonArray lores = new JsonArray();
        for (Lore l : s.lores()) {
            JsonObject lo = new JsonObject();
            lo.addProperty("name", l.name());
            lo.addProperty("rank", l.rank().name());
            lores.add(lo);
        }
        o.add("lores", lores);
        HitPoints hp = s.hitPoints();
        JsonObject hpo = new JsonObject();
        hpo.addProperty("ancestryHp", hp.ancestryHp());
        hpo.addProperty("classHpPerLevel", hp.classHpPerLevel());
        hpo.addProperty("bonusHp", hp.bonusHp());
        hpo.addProperty("bonusHpPerLevel", hp.bonusHpPerLevel());
        o.add("hitPoints", hpo);
        o.addProperty("speed", s.speed());
        JsonObject ac = new JsonObject();
        ac.addProperty("armorCategory", s.armorClass().armorCategory());
        if (s.armorClass().dexCap().isPresent()) {
            ac.addProperty("dexCap", s.armorClass().dexCap().getAsInt());
        }
        ac.addProperty("itemBonus", s.armorClass().itemBonus());
        o.add("armorClass", ac);
        JsonArray strikes = new JsonArray();
        for (Strike st : s.strikes()) {
            JsonObject so = new JsonObject();
            so.addProperty("name", st.name());
            so.addProperty("category", st.category());
            so.addProperty("damageDie", st.damageDie());
            so.addProperty("damageType", st.damageType());
            so.addProperty("attackAbility", st.attackAbility().name());
            so.addProperty("otherAttackBonus", st.otherAttackBonus());
            so.addProperty("otherDamageBonus", st.otherDamageBonus());
            strikes.add(so);
        }
        o.add("strikes", strikes);
        JsonArray feats = new JsonArray();
        for (Feat f : s.feats()) {
            JsonObject fo = new JsonObject();
            fo.addProperty("name", f.name());
            fo.addProperty("type", f.type());
            fo.addProperty("level", f.level());
            feats.add(fo);
        }
        o.add("feats", feats);
        JsonArray specials = new JsonArray();
        s.specials().forEach(specials::add);
        o.add("specials", specials);
        o.addProperty("currentHp", s.currentHp());
        o.addProperty("levelUpGranted", s.levelUpGranted());
        return GSON.toJson(o);
    }

    public static CharacterSheet read(String json) throws CharacterImportException {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                throw new CharacterImportException("Character data is not a JSON object");
            }
            JsonObject o = root.getAsJsonObject();
            if (!o.has("version") || o.get("version").getAsInt() != VERSION) {
                throw new CharacterImportException("Unsupported or missing character data version");
            }
            Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
            JsonObject so = obj(o, "abilityScores");
            for (Ability a : Ability.values()) {
                scores.put(a, num(so, a.name()));
            }
            Map<String, Rank> prof = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : obj(o, "proficiencies").entrySet()) {
                prof.put(e.getKey(), Rank.valueOf(e.getValue().getAsString()));
            }
            List<Lore> lores = new ArrayList<>();
            for (JsonElement el : arr(o, "lores")) {
                JsonObject lo = el.getAsJsonObject();
                lores.add(new Lore(str(lo, "name"), Rank.valueOf(str(lo, "rank"))));
            }
            JsonObject h = obj(o, "hitPoints");
            HitPoints hp = new HitPoints(num(h, "ancestryHp"), num(h, "classHpPerLevel"),
                    num(h, "bonusHp"), num(h, "bonusHpPerLevel"));
            JsonObject a = obj(o, "armorClass");
            ArmorClass ac = new ArmorClass(str(a, "armorCategory"),
                    a.has("dexCap") ? OptionalInt.of(a.get("dexCap").getAsInt()) : OptionalInt.empty(),
                    num(a, "itemBonus"));
            List<Strike> strikes = new ArrayList<>();
            for (JsonElement el : arr(o, "strikes")) {
                JsonObject st = el.getAsJsonObject();
                strikes.add(new Strike(str(st, "name"), str(st, "category"), str(st, "damageDie"),
                        str(st, "damageType"), Ability.valueOf(str(st, "attackAbility")),
                        num(st, "otherAttackBonus"), num(st, "otherDamageBonus")));
            }
            List<Feat> feats = new ArrayList<>();
            for (JsonElement el : arr(o, "feats")) {
                JsonObject f = el.getAsJsonObject();
                feats.add(new Feat(str(f, "name"), str(f, "type"), num(f, "level")));
            }
            List<String> specials = new ArrayList<>();
            for (JsonElement el : arr(o, "specials")) {
                specials.add(el.getAsString());
            }
            return new CharacterSheet(str(o, "name"), str(o, "className"), str(o, "ancestry"),
                    str(o, "heritage"), str(o, "background"), num(o, "level"),
                    Ability.valueOf(str(o, "keyAbility")), scores, prof, lores, hp, num(o, "speed"), ac,
                    strikes, feats, specials, num(o, "currentHp"),
                    o.get("levelUpGranted").getAsBoolean());
        } catch (CharacterImportException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CharacterImportException("Invalid character data: " + e, e);
        }
    }

    private static JsonObject obj(JsonObject o, String key) {
        return o.get(key).getAsJsonObject();
    }

    private static JsonArray arr(JsonObject o, String key) {
        return o.get(key).getAsJsonArray();
    }

    private static String str(JsonObject o, String key) {
        return o.get(key).getAsString();
    }

    private static int num(JsonObject o, String key) {
        return o.get(key).getAsInt();
    }
}
