package de.pf2craft.core.combat;

import de.pf2craft.core.check.DegreeOfSuccess;

/** Result of an attack roll: the natural d20, the total and the degree of success against AC. */
public record AttackOutcome(int natural, int total, DegreeOfSuccess degree) {
}
