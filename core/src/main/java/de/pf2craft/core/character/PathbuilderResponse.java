package de.pf2craft.core.character;

/** Turns an HTTP answer from Pathbuilder into a character sheet. */
public final class PathbuilderResponse {
    static final int MAX_BODY_CHARS = 2 * 1024 * 1024;

    private PathbuilderResponse() {
    }

    public static CharacterSheet toSheet(int httpStatus, String body) throws CharacterImportException {
        if (httpStatus != 200) {
            throw new CharacterImportException("Pathbuilder answered with status " + httpStatus);
        }
        if (body == null || body.length() > MAX_BODY_CHARS) {
            throw new CharacterImportException("Pathbuilder answer is missing or too large");
        }
        return PathbuilderImporter.parse(body);
    }
}
