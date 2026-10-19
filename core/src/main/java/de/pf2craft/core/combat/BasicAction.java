package de.pf2craft.core.combat;

/**
 * Definition of a basic action. {@code skill} is "" for none, a {@code Skill.id()} or "perception";
 * {@code defense} is "" or one of fortitude, reflex, will, perception_dc. {@code rangeBlocks} only matters
 * for {@link ActionKind#TARGET_RANGE}.
 */
public record BasicAction(String id, int cost, ActionKind kind, String skill, String defense,
        boolean countsAsAttack, boolean needsShield, int rangeBlocks) {
}
