package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.*;

import de.pf2craft.core.combat.CombatEffects.Expiry;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombatEffectsTest {

    @Test
    void shieldExpiresAtNextTurnStart() {
        CombatEffects fx = new CombatEffects();
        fx.addAcBonus("a", "raise_shield", 2, Expiry.OWN_TURN_START);
        assertEquals(2, fx.acBonus("a"));
        fx.onTurnStart("b");
        assertEquals(2, fx.acBonus("a"));
        fx.onMoved("a");
        assertEquals(2, fx.acBonus("a"));
        fx.onTurnStart("a");
        assertEquals(0, fx.acBonus("a"));
    }

    @Test
    void coverEndsOnMove() {
        CombatEffects fx = new CombatEffects();
        fx.addAcBonus("a", "take_cover", 2, Expiry.MOVE_OR_OWN_TURN_START);
        fx.onMoved("a");
        assertEquals(0, fx.acBonus("a"));
        fx.addAcBonus("a", "take_cover", 2, Expiry.MOVE_OR_OWN_TURN_START);
        fx.onTurnStart("a");
        assertEquals(0, fx.acBonus("a"));
    }

    @Test
    void sameSourceIsReplacedNotStacked() {
        CombatEffects fx = new CombatEffects();
        fx.addAcBonus("a", "raise_shield", 2, Expiry.OWN_TURN_START);
        fx.addAcBonus("a", "raise_shield", 3, Expiry.OWN_TURN_START);
        assertEquals(3, fx.acBonus("a"));
        assertEquals(List.of("raise_shield"), fx.sources("a"));
    }

    @Test
    void highestBonusWinsAcrossSources() {
        CombatEffects fx = new CombatEffects();
        fx.addAcBonus("a", "a", 1, Expiry.OWN_TURN_START);
        fx.addAcBonus("a", "b", 3, Expiry.OWN_TURN_START);
        assertEquals(3, fx.acBonus("a"));
    }

    @Test
    void clearRemovesAll() {
        CombatEffects fx = new CombatEffects();
        fx.addAcBonus("a", "x", 2, Expiry.OWN_TURN_START);
        fx.addAcBonus("b", "x", 2, Expiry.OWN_TURN_START);
        fx.clear("a");
        assertEquals(0, fx.acBonus("a"));
        assertEquals(2, fx.acBonus("b"));
        fx.clearAll();
        assertEquals(0, fx.acBonus("b"));
        assertTrue(fx.sources("b").isEmpty());
    }

    @Test
    void unknownIdIsZero() {
        assertEquals(0, new CombatEffects().acBonus("nobody"));
    }
}
