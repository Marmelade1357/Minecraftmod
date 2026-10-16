package de.pf2craft.core.character;

/** A feat; {@code type} is kept as Pathbuilder delivers it, e.g. "Class Feat". */
public record Feat(String name, String type, int level) {
}
