package de.pf2craft.core.dice;

/** Thrown when a dice expression such as {@code 2d6+3} cannot be parsed. */
public class DiceParseException extends Exception {
    public DiceParseException(String message) {
        super(message);
    }
}
