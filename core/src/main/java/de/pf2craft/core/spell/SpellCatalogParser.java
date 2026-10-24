package de.pf2craft.core.spell;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pf2craft.core.combat.DamageResolver;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Lenient parser for the spell catalog JSON: invalid entries are skipped and reported. */
public final class SpellCatalogParser {
    private static final Pattern DICE = Pattern.compile("[1-9]\\d{0,2}d(4|6|8|10|12|20)");
    private static final Set<String> SAVES = Set.of("", "fortitude", "reflex", "will");

    public record ParseResult(SpellCatalog catalog, List<String> errors) {
    }

    private SpellCatalogParser() {
    }

    public static ParseResult parse(String json) {
        List<String> errors = new ArrayList<>();
        List<SpellDefinition> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            errors.add("Invalid JSON: " + e.getMessage());
            return new ParseResult(SpellCatalog.empty(), errors);
        }
        if (!root.isJsonArray()) {
            errors.add("Spell catalog must be a JSON array");
            return new ParseResult(SpellCatalog.empty(), errors);
        }
        int index = 0;
        for (JsonElement el : root.getAsJsonArray()) {
            index++;
            try {
                if (!el.isJsonObject()) {
                    throw new IllegalArgumentException("not an object");
                }
                SpellDefinition d = entry(el.getAsJsonObject());
                if (!seen.add(d.id())) {
                    throw new IllegalArgumentException("duplicate id '" + d.id() + "'");
                }
                out.add(d);
            } catch (RuntimeException e) {
                errors.add("Entry " + index + ": " + e.getMessage());
            }
        }
        return new ParseResult(new SpellCatalog(out), errors);
    }

    private static SpellDefinition entry(JsonObject o) {
        String id = str(o, "id", null);
        String name = str(o, "name", null);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("missing id");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("missing name in '" + id + "'");
        }
        int rank = num(o, "rank", 0);
        int actions = num(o, "actions", 2);
        if (rank < 0 || rank > 10) {
            throw new IllegalArgumentException("rank out of range in '" + id + "'");
        }
        if (actions < 1 || actions > 3) {
            throw new IllegalArgumentException("actions must be 1..3 in '" + id + "'");
        }
        int range = num(o, "rangeFeet", 0);
        int targets = num(o, "maxTargets", 1);
        if (range < 0 || targets < 0) {
            throw new IllegalArgumentException("negative range or targets in '" + id + "'");
        }
        SpellKind kind;
        try {
            kind = SpellKind.valueOf(str(o, "kind", "UTILITY").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("bad kind in '" + id + "'");
        }
        String save = str(o, "save", "").toLowerCase(Locale.ROOT);
        if (!SAVES.contains(save)) {
            throw new IllegalArgumentException("bad save in '" + id + "'");
        }
        String dice = str(o, "damageDice", "");
        String heighten = str(o, "heightenDice", "");
        if ((!dice.isEmpty() && !DICE.matcher(dice).matches())
                || (!heighten.isEmpty() && !DICE.matcher(heighten).matches())) {
            throw new IllegalArgumentException("bad dice in '" + id + "'");
        }
        int step = num(o, "heightenStep", 0);
        if (step < 0 || step > 2) {
            throw new IllegalArgumentException("heightenStep must be 0..2 in '" + id + "'");
        }
        String type = str(o, "damageType", "").toLowerCase(Locale.ROOT);
        if (!dice.isEmpty() && kind != SpellKind.HEAL && !DamageResolver.isDamageType(type)) {
            throw new IllegalArgumentException("bad damage type in '" + id + "'");
        }
        String extra = str(o, "extraDice", "");
        String extraHeighten = str(o, "extraHeightenDice", "");
        String extraType = str(o, "extraType", "").toLowerCase(Locale.ROOT);
        if ((!extra.isEmpty() && !DICE.matcher(extra).matches())
                || (!extraHeighten.isEmpty() && !DICE.matcher(extraHeighten).matches())
                || (!extra.isEmpty() && !DamageResolver.isDamageType(extraType))) {
            throw new IllegalArgumentException("bad extra damage in '" + id + "'");
        }
        return new SpellDefinition(id, name, rank, actions, range, targets, kind, save,
                bool(o, "basicSave"), dice, num(o, "damageBonus", 0), type, heighten, step,
                bool(o, "addAbilityMod"), num(o, "heightenBonus", 0), extra, extraType, extraHeighten);
    }

    private static String str(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? def : e.getAsString();
    }

    private static int num(JsonObject o, String key, int def) {
        JsonElement e = o.get(key);
        try {
            return e == null || e.isJsonNull() ? def : e.getAsInt();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("bad number '" + key + "'");
        }
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }
}
