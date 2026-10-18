package de.pf2craft.core.monster;

import java.util.Locale;
import java.util.function.Predicate;

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

    /**
     * A template id for {@code name} that is not {@code taken}: the sanitised name, else with "-custom",
     * "-custom-2", "-custom-3" ... appended. The id (which is also the file name stem) stays within the cap.
     */
    public static String freeId(String name, Predicate<String> taken) {
        String file = fileName(name);
        String base = file.substring(0, file.length() - ".json".length());
        if (!taken.test(base)) {
            return base;
        }
        for (int n = 1; ; n++) {
            String suffix = n == 1 ? "-custom" : "-custom-" + n;
            String stem = base.length() + suffix.length() > MAX_LENGTH
                    ? trimDashes(base.substring(0, MAX_LENGTH - suffix.length())) : base;
            String candidate = stem + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
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
