package de.pf2craft.core.dialogue;

/** Cleans up text an NPC is made to say. */
public final class SpeechText {
    public static final int MAX_LENGTH = 120;

    private SpeechText() {
    }

    /**
     * Removes formatting codes (the section sign and the character after it) and control characters, collapses
     * whitespace, trims and caps the length at {@link #MAX_LENGTH}. Null gives an empty string.
     */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\u00a7') {
                i++; // the code character that follows
                continue;
            }
            sb.append(Character.isISOControl(c) || Character.isWhitespace(c) ? ' ' : c);
        }
        String text = sb.toString().replaceAll(" {2,}", " ").trim();
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH).trim() : text;
    }
}
