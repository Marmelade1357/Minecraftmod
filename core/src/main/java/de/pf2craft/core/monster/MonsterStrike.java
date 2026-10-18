package de.pf2craft.core.monster;

import java.util.List;

/** A monster's attack with its attack bonus and damage components. */
public record MonsterStrike(String name, int attackBonus, List<DamagePart> damage, List<String> traits,
        boolean ranged) {
}
