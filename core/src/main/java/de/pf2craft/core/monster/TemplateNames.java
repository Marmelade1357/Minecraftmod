package de.pf2craft.core.monster;

import java.util.Locale;

/** Safe file names for NPC templates saved under the config folder. */
public final class TemplateNames {
    private static final int MAX_LENGTH = 64;

    private TemplateNames() {
    }

    public static String fileName(String id) {
        String name = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
        name = trimDashes(name);
        if (name.length() > MAX_LENGTH) {
            name = trimDashes(name.substring(0, MAX_LENGTH));
        }
        return (name.isEmpty() ? "template" : name) + ".json";
    }

    private static String trimDashes(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '-') {
            start++;
        }
        while (end > start && s.charAt(end - 1) == '-') {
            end--;
        }
        return s.substring(start, end);
    }
}
