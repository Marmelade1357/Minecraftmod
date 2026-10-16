package de.pf2craft.client;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.character.CharacterImportException;
import de.pf2craft.core.character.CharacterJson;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.network.SheetSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Client cache of the character sheets the server sent us (the own one and, for GMs, others). */
public final class ClientSheets {
    public record Synced(CharacterSheet sheet, boolean gmView, boolean classSupported) {
    }

    private static final Map<UUID, Synced> SHEETS = new HashMap<>();

    private ClientSheets() {
    }

    /** Stores a synced sheet; a sheet that cannot be read is logged and ignored. */
    public static void put(SheetSyncPayload payload) {
        try {
            CharacterSheet sheet = CharacterJson.read(payload.sheetJson());
            SHEETS.put(payload.owner(), new Synced(sheet, payload.gmView(), payload.classSupported()));
        } catch (CharacterImportException | RuntimeException e) {
            Pf2Craft.LOGGER.warn("Could not read synced character sheet of {}", payload.owner(), e);
        }
    }

    public static Optional<Synced> get(UUID owner) {
        return Optional.ofNullable(SHEETS.get(owner));
    }

    public static void remove(UUID owner) {
        SHEETS.remove(owner);
    }

    public static void clear() {
        SHEETS.clear();
    }
}
