package de.pf2craft.client;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.character.CharacterImportException;
import de.pf2craft.core.character.CharacterJson;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.progression.LevelUpOptions;
import de.pf2craft.network.SheetSyncPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Client cache of the character sheets the server sent us (the own one and, for GMs, others). */
public final class ClientSheets {
    /** {@code levelUpOptions} is present while a level-up is granted and possible. */
    public record Synced(CharacterSheet sheet, boolean gmView, boolean classSupported,
                         Optional<LevelUpOptions> levelUpOptions) {
    }

    private static final Map<UUID, Synced> SHEETS = new HashMap<>();

    private ClientSheets() {
    }

    /** Stores a synced sheet; a sheet that cannot be read is logged and ignored. */
    public static void put(SheetSyncPayload payload) {
        try {
            CharacterSheet sheet = CharacterJson.read(payload.sheetJson());
            SHEETS.put(payload.owner(), new Synced(sheet, payload.gmView(), payload.classSupported(),
                    readOptions(payload)));
        } catch (CharacterImportException | RuntimeException e) {
            Pf2Craft.LOGGER.warn("Could not read synced character sheet of {}", payload.owner(), e);
        }
    }

    private static Optional<LevelUpOptions> readOptions(SheetSyncPayload payload) {
        if (payload.levelUpOptionsJson().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LevelUpOptions.fromJson(payload.levelUpOptionsJson()));
        } catch (IllegalArgumentException e) {
            Pf2Craft.LOGGER.warn("Could not read level-up options of {}", payload.owner(), e);
            return Optional.empty();
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
