package de.pf2craft.core.character;

import java.util.Optional;

/** Identifies a rollable check on a sheet, e.g. "skill:athletics" or "strike:0". */
public record CheckId(Kind kind, String key, int index) {
    public enum Kind { SKILL, SAVE, PERCEPTION, LORE, STRIKE }

    public static Optional<CheckId> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        if (text.equals("perception")) {
            return Optional.of(new CheckId(Kind.PERCEPTION, "", 0));
        }
        int colon = text.indexOf(':');
        if (colon < 0) {
            return Optional.empty();
        }
        String prefix = text.substring(0, colon);
        String rest = text.substring(colon + 1);
        switch (prefix) {
            case "skill":
                for (Skill skill : Skill.values()) {
                    if (skill.id().equals(rest)) {
                        return Optional.of(new CheckId(Kind.SKILL, rest, 0));
                    }
                }
                return Optional.empty();
            case "save":
                for (Save save : Save.values()) {
                    if (save.id().equals(rest)) {
                        return Optional.of(new CheckId(Kind.SAVE, rest, 0));
                    }
                }
                return Optional.empty();
            case "lore":
            case "strike":
                if (rest.isEmpty() || rest.length() > 6 || !rest.chars().allMatch(c -> c >= '0' && c <= '9')) {
                    return Optional.empty();
                }
                return Optional.of(new CheckId(prefix.equals("lore") ? Kind.LORE : Kind.STRIKE,
                        "", Integer.parseInt(rest)));
            default:
                return Optional.empty();
        }
    }

    public String format() {
        switch (kind) {
            case SKILL:
                return "skill:" + key;
            case SAVE:
                return "save:" + key;
            case PERCEPTION:
                return "perception";
            case LORE:
                return "lore:" + index;
            default:
                return "strike:" + index;
        }
    }

    /** English label for display, e.g. "Athletics" or "Lore: Farming". */
    public String label(CharacterSheet sheet) {
        switch (kind) {
            case SKILL:
            case SAVE:
                return Character.toUpperCase(key.charAt(0)) + key.substring(1);
            case PERCEPTION:
                return "Perception";
            case LORE:
                return index < sheet.lores().size() ? "Lore: " + sheet.lores().get(index).name() : "Lore";
            default:
                return index < sheet.strikes().size() ? sheet.strikes().get(index).name() : "Strike";
        }
    }
}
