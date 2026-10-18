package de.pf2craft.core.monster;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Lookup and search over monsters. Later entries replace earlier ones with the same id. */
public final class MonsterCatalog {
    private final Map<String, MonsterData> byId = new LinkedHashMap<>();

    public MonsterCatalog(List<MonsterData> monsters) {
        for (MonsterData m : monsters) {
            byId.put(m.id(), m);
        }
    }

    public Optional<MonsterData> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public int size() {
        return byId.size();
    }

    /** Every word of {@code text} must occur in the name (case-insensitive); sorted by level, then name. */
    public List<MonsterData> search(String text, int limit) {
        String[] words = text.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return byId.values().stream()
                .filter(m -> {
                    String name = m.name().toLowerCase(Locale.ROOT);
                    for (String w : words) {
                        if (!w.isEmpty() && !name.contains(w)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparingInt(MonsterData::level).thenComparing(MonsterData::name))
                .limit(Math.max(0, limit))
                .toList();
    }
}
