package de.pf2craft.core.combat;

import java.util.List;

/** Result of applying damage to defences. {@code unmatched} lists defence types that had no effect here. */
public record DamageOutcome(int total, List<AppliedPart> parts, List<String> unmatched) {
    public DamageOutcome {
        parts = List.copyOf(parts);
        unmatched = List.copyOf(unmatched);
    }
}
