package de.pf2craft.core.combat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The conditions of one combatant. A value of 0 means absent; an unvalued condition that is present counts as 1. */
public final class ConditionSet {
    /** Dying is capped at 4 (death). */
    public static final int DYING_CAP = 4;

    private final Map<Condition, Integer> values = new LinkedHashMap<>();

    public int value(Condition c) {
        return values.getOrDefault(c, 0);
    }

    public boolean has(Condition c) {
        return values.containsKey(c);
    }

    /** Overwrites the value; 0 or less removes the condition. */
    public void set(Condition c, int value) {
        if (value <= 0) {
            values.remove(c);
            return;
        }
        int v = c.valued() ? value : 1;
        if (c == Condition.DYING) {
            v = Math.min(v, DYING_CAP);
        }
        values.put(c, v);
    }

    /** Applies the condition, keeping the higher value if it is already present. */
    public void apply(Condition c, int value) {
        if (value <= 0) {
            return;
        }
        set(c, Math.max(value(c), value));
    }

    public void remove(Condition c) {
        values.remove(c);
    }

    /** Lowers a valued condition by one; it disappears at 0. Unvalued ones are removed. */
    public void decrement(Condition c) {
        if (!has(c)) {
            return;
        }
        set(c, c.valued() ? value(c) - 1 : 0);
    }

    /** Unmodifiable view in insertion order. */
    public Map<Condition, Integer> all() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public ConditionSet copy() {
        ConditionSet copy = new ConditionSet();
        copy.values.putAll(values);
        return copy;
    }
}
