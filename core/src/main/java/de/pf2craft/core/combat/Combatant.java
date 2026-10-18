package de.pf2craft.core.combat;

/** A participant of an encounter; {@code perception} only breaks initiative ties. */
public record Combatant(String id, String name, boolean npc, int perception) {
}
