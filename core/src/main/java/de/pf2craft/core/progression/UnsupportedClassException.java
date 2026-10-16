package de.pf2craft.core.progression;

/** Thrown when a level-up is impossible: unknown class or maximum level reached. */
public class UnsupportedClassException extends Exception {
    public UnsupportedClassException(String message) {
        super(message);
    }
}
