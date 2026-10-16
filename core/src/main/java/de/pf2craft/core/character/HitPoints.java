package de.pf2craft.core.character;

/** Hit point building blocks; the maximum is computed in {@link CharacterMath#maxHp}. */
public record HitPoints(int ancestryHp, int classHpPerLevel, int bonusHp, int bonusHpPerLevel) {
}
