package de.pf2craft.core.character;

import java.util.Optional;

/** Validates the numeric id of a Pathbuilder 2e JSON export. */
public final class PathbuilderId {
    private PathbuilderId() {
    }

    /** @return the trimmed id if it consists of 1 to 10 ASCII digits */
    public static Optional<String> parse(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String id = input.trim();
        if (id.isEmpty() || id.length() > 10) {
            return Optional.empty();
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < '0' || c > '9') {
                return Optional.empty();
            }
        }
        return Optional.of(id);
    }
}
