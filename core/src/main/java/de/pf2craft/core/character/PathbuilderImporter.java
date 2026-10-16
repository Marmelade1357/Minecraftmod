package de.pf2craft.core.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/** Converts a Pathbuilder 2e JSON export ({@code json.php?id=...}) into a {@link CharacterSheet}. */
public final class PathbuilderImporter {
    private static final int MAX_LIST = 64;
    private static final int MAX_FEATS = 200;
    private static final Set<String> IGNORED_PROFICIENCIES = Set.of("piloting", "computers");

    private PathbuilderImporter() {
    }

    public static CharacterSheet parse(String json) throws CharacterImportException {
        try {
            return parseUnchecked(json);
        } catch (CharacterImportException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CharacterImportException("Invalid Pathbuilder data: " + e.getMessage(), e);
        }
    }

    private static CharacterSheet parseUnchecked(String json) throws CharacterImportException {
        JsonElement rootEl = JsonParser.parseString(json);
        if (!rootEl.isJsonObject()) {
            throw new CharacterImportException("Pathbuilder data must be a JSON object");
        }
        JsonObject root = rootEl.getAsJsonObject();
        if (root.has("success") && !root.get("success").isJsonNull()
                && !root.get("success").getAsBoolean()) {
            throw new CharacterImportException("Pathbuilder reported an error for this character");
        }
        if (!root.has("build") || !root.get("build").isJsonObject()) {
            throw new CharacterImportException("Pathbuilder data has no build");
        }
        JsonObject b = root.getAsJsonObject("build");
        for (String key : new String[] {"name", "class", "level", "abilities", "proficiencies"}) {
            if (!b.has(key) || b.get(key).isJsonNull()) {
                throw new CharacterImportException("Pathbuilder data is missing '" + key + "'");
            }
        }
        if (b.has("dualClass") && !b.get("dualClass").isJsonNull()
                && !b.get("dualClass").getAsString().isEmpty()) {
            throw new CharacterImportException("Dual class characters are not supported yet");
        }
        int level = b.get("level").getAsInt();
        if (level < 1 || level > 20) {
            throw new CharacterImportException("Level must be between 1 and 20");
        }

        Map<Ability, Integer> scores = new EnumMap<>(Ability.class);
        JsonObject abilities = b.getAsJsonObject("abilities");
        for (Ability a : Ability.values()) {
            if (!abilities.has(a.pathbuilderKey())) {
                throw new CharacterImportException("Missing ability score " + a.pathbuilderKey());
            }
            int score = abilities.get(a.pathbuilderKey()).getAsInt();
            if (score < 1 || score > 30) {
                throw new CharacterImportException("Ability score out of range: " + score);
            }
            scores.put(a, score);
        }

        Map<String, Rank> prof = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : b.getAsJsonObject("proficiencies").entrySet()) {
            String key = e.getKey();
            if (key.startsWith("casting") || IGNORED_PROFICIENCIES.contains(key)) {
                continue;
            }
            if (key.equals("classDC")) {
                key = "class_dc";
            }
            prof.put(key, Rank.fromPathbuilder(e.getValue().getAsInt()));
        }

        List<Lore> lores = new ArrayList<>();
        for (JsonElement el : array(b, "lores", MAX_LIST)) {
            JsonArray pair = el.getAsJsonArray();
            lores.add(new Lore(pair.get(0).getAsString(), Rank.fromPathbuilder(pair.get(1).getAsInt())));
        }

        JsonObject attr = b.has("attributes") && b.get("attributes").isJsonObject()
                ? b.getAsJsonObject("attributes") : new JsonObject();
        HitPoints hp = new HitPoints(intOf(attr, "ancestryhp", 0), intOf(attr, "classhp", 0),
                intOf(attr, "bonushp", 0), intOf(attr, "bonushpPerLevel", 0));
        int speed = intOf(attr, "speed", 25) + intOf(attr, "speedBonus", 0);

        List<Feat> feats = new ArrayList<>();
        for (JsonElement el : array(b, "feats", MAX_FEATS)) {
            JsonArray f = el.getAsJsonArray();
            String name = f.get(0).getAsString();
            if (f.size() > 1 && !f.get(1).isJsonNull()) {
                name = name + " (" + f.get(1).getAsString() + ")";
            }
            String type = f.size() > 2 && !f.get(2).isJsonNull() ? f.get(2).getAsString() : "";
            int featLevel = f.size() > 3 && !f.get(3).isJsonNull() ? f.get(3).getAsInt() : 1;
            feats.add(new Feat(name, type, featLevel));
        }

        List<String> specials = new ArrayList<>();
        for (JsonElement el : array(b, "specials", MAX_FEATS)) {
            if (!el.isJsonNull() && !el.getAsString().equals("Not Selected")) {
                specials.add(el.getAsString());
            }
        }

        Ability keyAbility = Ability.fromPathbuilder(str(b, "keyability", "str"));
        int dexMod = Ability.modifier(scores.get(Ability.DEX));
        ArmorClass ac = parseArmor(b, dexMod);

        List<Strike> strikes = new ArrayList<>();
        for (JsonElement el : array(b, "weapons", MAX_LIST)) {
            strikes.add(parseStrike(el.getAsJsonObject(), level, scores, prof));
        }

        CharacterSheet sheet = new CharacterSheet(b.get("name").getAsString(),
                b.get("class").getAsString(), str(b, "ancestry", ""), str(b, "heritage", ""),
                str(b, "background", ""), level, keyAbility, scores, prof, lores, hp, speed, ac,
                strikes, feats, specials, 0, false);
        return sheet.withCurrentHp(CharacterMath.maxHp(sheet));
    }

    private static ArmorClass parseArmor(JsonObject b, int dexMod) {
        String category = "unarmored";
        for (JsonElement el : array(b, "armor", MAX_LIST)) {
            JsonObject armor = el.getAsJsonObject();
            if (armor.has("worn") && !armor.get("worn").isJsonNull() && armor.get("worn").getAsBoolean()) {
                category = str(armor, "prof", "unarmored");
                break;
            }
        }
        int itemBonus = 0;
        OptionalInt dexCap = OptionalInt.empty();
        if (b.has("acTotal") && b.get("acTotal").isJsonObject()) {
            JsonObject total = b.getAsJsonObject("acTotal");
            itemBonus = intOf(total, "acItemBonus", 0);
            int abilityBonus = intOf(total, "acAbilityBonus", dexMod);
            if (abilityBonus < dexMod) {
                dexCap = OptionalInt.of(abilityBonus);
            }
        }
        return new ArmorClass(category, dexCap, itemBonus);
    }

    /** Splits Pathbuilder's single attack bonus into ability modifier, proficiency and the rest. */
    private static Strike parseStrike(JsonObject w, int level, Map<Ability, Integer> scores,
                                      Map<String, Rank> prof) {
        String category = str(w, "prof", "simple");
        int rankBonus = prof.getOrDefault(category, Rank.UNTRAINED).bonus(level);
        int attack = intOf(w, "attack", 0);
        int str = Ability.modifier(scores.get(Ability.STR));
        int dex = Ability.modifier(scores.get(Ability.DEX));
        Ability ability;
        int other;
        if (fits(attack - str - rankBonus)) {
            ability = Ability.STR;
            other = attack - str - rankBonus;
        } else if (fits(attack - dex - rankBonus)) {
            ability = Ability.DEX;
            other = attack - dex - rankBonus;
        } else {
            ability = str >= dex ? Ability.STR : Ability.DEX;
            other = attack - (ability == Ability.STR ? str : dex) - rankBonus;
        }
        int otherDamage = intOf(w, "damageBonus", 0) - (ability == Ability.STR ? str : 0);
        return new Strike(str(w, "name", "Strike"), category, str(w, "die", "d4"),
                str(w, "damageType", ""), ability, other, otherDamage);
    }

    private static boolean fits(int other) {
        return other >= 0 && other <= 4;
    }

    private static JsonArray array(JsonObject o, String key, int max) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return new JsonArray();
        }
        JsonArray a = o.getAsJsonArray(key);
        if (a.size() > max) {
            throw new IllegalArgumentException("Too many entries in " + key);
        }
        return a;
    }

    private static int intOf(JsonObject o, String key, int fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : fallback;
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }
}
