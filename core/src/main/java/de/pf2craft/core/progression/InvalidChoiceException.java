package de.pf2craft.core.progression;

/** Thrown when level-up choices break the rules; the message is meant for the player. */
public class InvalidChoiceException extends Exception {
    public InvalidChoiceException(String message) {
        super(message);
    }
}
