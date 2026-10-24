package de.pf2craft.client;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.spell.Spellbook;
import de.pf2craft.core.spell.SpellbookJson;
import de.pf2craft.network.SpellbookSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Client cache of the spellbooks the server sent us (the own one and, for GMs, others). */
public final class ClientSpellbooks {
    private static final Map<UUID, Spellbook> BOOKS = new HashMap<>();

    private ClientSpellbooks() {
    }

    public static void put(SpellbookSyncPayload payload) {
        try {
            BOOKS.put(payload.owner(), SpellbookJson.read(payload.json()));
        } catch (RuntimeException e) {
            Pf2Craft.LOGGER.warn("Could not read synced spellbook of {}", payload.owner(), e);
        }
    }

    public static Optional<Spellbook> get(UUID owner) {
        return Optional.ofNullable(BOOKS.get(owner));
    }

    public static void clear() {
        BOOKS.clear();
    }
}
