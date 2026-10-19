package de.pf2craft.core.combat;

import java.util.Locale;
import java.util.Optional;

/** Conditions tracked on combatants. Valued conditions carry a number, the others are simply present. */
public enum Condition {
    FRIGHTENED(true, false),
    OFF_GUARD(false, false),
    PRONE(false, true),
    GRABBED(false, false),
    SLOWED(true, false),
    STUNNED(true, false),
    UNCONSCIOUS(false, true),
    DYING(true, true),
    WOUNDED(true, false);

    private final boolean valued;
    private final boolean publicToAll;

    Condition(boolean valued, boolean publicToAll) {
        this.valued = valued;
        this.publicToAll = publicToAll;
    }

    public boolean valued() {
        return valued;
    }

    /** Whether everyone sees this condition (otherwise only the owner and GMs). */
    public boolean publicToAll() {
        return publicToAll;
    }

    /** Lowercase snake_case id, e.g. {@code off_guard}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<Condition> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Condition c : values()) {
            if (c.id().equals(id)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
