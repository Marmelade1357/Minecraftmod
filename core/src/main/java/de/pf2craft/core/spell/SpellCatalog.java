package de.pf2craft.core.spell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** All known spells, looked up by name or id. */
public final class SpellCatalog {
    private final Map<String, SpellDefinition> byId = new LinkedHashMap<>();

    public SpellCatalog(List<SpellDefinition> spells) {
        for (SpellDefinition s : spells) {
            byId.put(s.id(), s);
        }
    }

    public static SpellCatalog empty() {
        return new SpellCatalog(List.of());
    }

    public Optional<SpellDefinition> byName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        for (SpellDefinition s : byId.values()) {
            if (s.id().equalsIgnoreCase(key) || s.name().toLowerCase(Locale.ROOT).equals(key)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public List<SpellDefinition> all() {
        return new ArrayList<>(byId.values());
    }

    /** Entries of the override replace entries with the same id; new ones are appended. */
    public SpellCatalog merged(SpellCatalog override) {
        Map<String, SpellDefinition> map = new LinkedHashMap<>(byId);
        map.putAll(override.byId);
        return new SpellCatalog(new ArrayList<>(map.values()));
    }
}
