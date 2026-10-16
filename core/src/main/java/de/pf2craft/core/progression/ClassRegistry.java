package de.pf2craft.core.progression;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Lookup of class progressions by name, case-insensitive. */
public final class ClassRegistry {
    private final Map<String, ClassProgression> byName = new HashMap<>();

    public ClassRegistry(Collection<ClassProgression> classes) {
        for (ClassProgression c : classes) {
            byName.put(c.name().toLowerCase(Locale.ROOT), c);
        }
    }

    public Optional<ClassProgression> find(String className) {
        return className == null ? Optional.empty()
                : Optional.ofNullable(byName.get(className.toLowerCase(Locale.ROOT)));
    }

    public int size() {
        return byName.size();
    }
}
