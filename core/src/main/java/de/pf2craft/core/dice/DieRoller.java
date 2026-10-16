package de.pf2craft.core.dice;

import java.util.random.RandomGenerator;

/** Source of single die results. Abstracted so tests can use fixed values. */
@FunctionalInterface
public interface DieRoller {
    /** @return a value between 1 and {@code sides} (inclusive) */
    int roll(int sides);

    static DieRoller random(RandomGenerator random) {
        return sides -> random.nextInt(sides) + 1;
    }
}
