package de.pf2craft.core.character;

import de.pf2craft.core.item.ItemCatalog;
import java.util.List;

/** Turns an HTTP answer from Pathbuilder into a character sheet. */
public final class PathbuilderResponse {
    static final int MAX_BODY_CHARS = 2 * 1024 * 1024;

    private PathbuilderResponse() {
    }

    private static final ItemCatalog EMPTY_CATALOG = new ItemCatalog(List.of(), List.of(), List.of());

    public static CharacterSheet toSheet(int httpStatus, String body) throws CharacterImportException {
        return toSheet(httpStatus, body, EMPTY_CATALOG);
    }

    public static CharacterSheet toSheet(int httpStatus, String body, ItemCatalog catalog)
            throws CharacterImportException {
        if (httpStatus != 200) {
            throw new CharacterImportException("Pathbuilder answered with status " + httpStatus);
        }
        if (body == null || body.length() > MAX_BODY_CHARS) {
            throw new CharacterImportException("Pathbuilder answer is missing or too large");
        }
        return PathbuilderImporter.parse(body, catalog);
    }
}
