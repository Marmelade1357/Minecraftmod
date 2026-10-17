package de.pf2craft.core.item;

/** Display words for weapon runes. */
public final class RuneNames {
    private RuneNames() {
    }

    /** Striking rune word for a number of weapon dice: "" for 1, up to "Major Striking" for 4. */
    public static String strikingWord(int dice) {
        return switch (dice) {
            case 2 -> "Striking";
            case 3 -> "Greater Striking";
            case 4 -> "Major Striking";
            default -> "";
        };
    }
}
