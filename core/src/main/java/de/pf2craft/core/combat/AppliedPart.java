package de.pf2craft.core.combat;

/** One damage instance before ({@code rolled}) and after ({@code applied}) resistances and weaknesses. */
public record AppliedPart(String type, int rolled, int applied) {
}
