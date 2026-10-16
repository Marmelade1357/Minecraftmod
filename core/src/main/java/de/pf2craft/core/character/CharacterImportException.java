package de.pf2craft.core.character;

/** Thrown when character data (stored JSON or an import file) is missing or malformed. */
public class CharacterImportException extends Exception {
    public CharacterImportException(String message) {
        super(message);
    }

    public CharacterImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
