package de.pf2craft.core.combat;

import de.pf2craft.core.monster.Resistance;
import java.util.List;

/** Resistances, weaknesses and immunities of a target. */
public record Defenses(List<Resistance> resistances, List<Resistance> weaknesses, List<String> immunities) {
    public static final Defenses NONE = new Defenses(List.of(), List.of(), List.of());

    public Defenses {
        resistances = List.copyOf(resistances);
        weaknesses = List.copyOf(weaknesses);
        immunities = List.copyOf(immunities);
    }
}
