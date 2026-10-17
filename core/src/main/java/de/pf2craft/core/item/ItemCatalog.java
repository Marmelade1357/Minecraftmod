package de.pf2craft.core.item;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Lookup of weapons, armor and shields by loosely normalised name. */
public final class ItemCatalog {
    private final Map<String, WeaponData> weapons = new HashMap<>();
    private final Map<String, ArmorData> armor = new HashMap<>();
    private final Map<String, ShieldData> shields = new HashMap<>();

    /** Later entries override earlier ones with the same normalised name. */
    public ItemCatalog(List<WeaponData> weapons, List<ArmorData> armor, List<ShieldData> shields) {
        for (WeaponData w : weapons) {
            this.weapons.put(normalise(w.name()), w);
        }
        for (ArmorData a : armor) {
            this.armor.put(normalise(a.name()), a);
        }
        for (ShieldData s : shields) {
            this.shields.put(normalise(s.name()), s);
        }
    }

    public Optional<WeaponData> weapon(String name) {
        return Optional.ofNullable(weapons.get(normalise(name)));
    }

    public Optional<ArmorData> armor(String name) {
        return Optional.ofNullable(armor.get(normalise(name)));
    }

    public Optional<ShieldData> shield(String name) {
        return Optional.ofNullable(shields.get(normalise(name)));
    }

    public int size() {
        return weapons.size() + armor.size() + shields.size();
    }

    /** Lowercase, drop "+N " prefix and rune words, keep only letters, digits and single spaces. */
    public static String normalise(String name) {
        String s = name.toLowerCase(Locale.ROOT).trim();
        s = s.replaceFirst("^\\+\\d+\\s+", "");
        s = s.replaceAll("\\b(striking|greater|major)\\b", " ");
        s = s.replaceAll("[^\\p{L}\\p{N} ]", "");
        return s.replaceAll("\\s+", " ").trim();
    }
}
