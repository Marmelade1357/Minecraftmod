package de.pf2craft.core.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporary combat effects per combatant (keyed by id string). Only AC bonuses so far; they are circumstance
 * bonuses, so the highest one applies and bonuses never stack. Lives in memory only, like the combat itself.
 */
public final class CombatEffects {
    public enum Expiry { OWN_TURN_START, MOVE_OR_OWN_TURN_START }

    private record AcBonus(int bonus, Expiry expiry) {}

    private final Map<String, Map<String, AcBonus>> effects = new HashMap<>();

    /** Adds an AC bonus; a repeated add from the same source replaces the earlier entry. */
    public void addAcBonus(String combatantId, String source, int bonus, Expiry expiry) {
        effects.computeIfAbsent(combatantId, k -> new LinkedHashMap<>()).put(source, new AcBonus(bonus, expiry));
    }

    /** Highest active bonus (same-type bonuses do not stack), 0 if none. */
    public int acBonus(String combatantId) {
        Map<String, AcBonus> own = effects.get(combatantId);
        int best = 0;
        if (own != null) {
            for (AcBonus b : own.values()) {
                best = Math.max(best, b.bonus());
            }
        }
        return best;
    }

    public void onTurnStart(String combatantId) {
        remove(combatantId, false);
    }

    public void onMoved(String combatantId) {
        remove(combatantId, true);
    }

    private void remove(String combatantId, boolean movedOnly) {
        Map<String, AcBonus> own = effects.get(combatantId);
        if (own == null) {
            return;
        }
        own.values().removeIf(b -> b.expiry() == Expiry.MOVE_OR_OWN_TURN_START
                || (!movedOnly && b.expiry() == Expiry.OWN_TURN_START));
        if (own.isEmpty()) {
            effects.remove(combatantId);
        }
    }

    public void clear(String combatantId) {
        effects.remove(combatantId);
    }

    public void clearAll() {
        effects.clear();
    }

    public List<String> sources(String combatantId) {
        Map<String, AcBonus> own = effects.get(combatantId);
        return own == null ? List.of() : new ArrayList<>(own.keySet());
    }
}
