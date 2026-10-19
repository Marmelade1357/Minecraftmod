package de.pf2craft.combat;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.check.DegreeOfSuccess;
import de.pf2craft.core.combat.Condition;
import de.pf2craft.core.combat.ConditionSet;
import de.pf2craft.core.combat.DyingRules;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.roll.RollService;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Knocked-out players: they stay in the initiative order (unconscious and dying), roll a recovery check at the
 * start of each of their turns (the turn then ends by itself) and wake up when healed. Death is never executed,
 * only reported to the GMs. Static, server thread only.
 */
public final class DyingService {
    private static final Condition[] TRACKED = {Condition.UNCONSCIOUS, Condition.DYING, Condition.WOUNDED};
    /** Combatants at dying 4 that the GMs were already told about; automatic recovery stops for them. */
    private static final Set<String> REPORTED_DEAD = new HashSet<>();

    private DyingService() {
    }

    /** A player dropped to 0 HP in a running combat. Does nothing outside a combat. */
    public static void knockOut(ServerPlayerEntity player, boolean critical) {
        String id = player.getUuid().toString();
        if (!CombatService.contains(player.getUuid())) {
            return;
        }
        ConditionSet set = ConditionService.of(id);
        int[] before = snapshot(set);
        DyingRules.knockOut(set, critical);
        announceChanges(id, set, before, "knocked out");
        reportIfDead(id, set);
        CombatService.sync(null);
    }

    /** A downed player (already at 0 HP) takes damage: dying rises instead of a second knock-out. */
    public static void damageWhileDown(ServerPlayerEntity player, boolean critical) {
        String id = player.getUuid().toString();
        if (!CombatService.contains(player.getUuid())) {
            return;
        }
        ConditionSet set = ConditionService.of(id);
        int[] before = snapshot(set);
        DyingRules.damageWhileDying(set, critical);
        announceChanges(id, set, before, "damaged while down");
        reportIfDead(id, set);
        CombatService.sync(null);
    }

    /** The dying combatant's turn begins: roll the recovery check against the DC (called by ConditionService). */
    static void recover(String id) {
        ConditionSet set = ConditionService.of(id);
        if (!set.has(Condition.DYING)) {
            return; // unconscious but stable: the turn is simply skipped by the auto end
        }
        if (set.value(Condition.DYING) < ConditionSet.DYING_CAP) {
            REPORTED_DEAD.remove(id);
        } else if (REPORTED_DEAD.contains(id)) {
            return; // already reported; the GM decides what happens
        }
        int dc = DyingRules.recoveryDc(set);
        int natural;
        try {
            natural = RollService.rollQuiet("1d20").naturalD20().orElseThrow();
        } catch (DiceParseException e) {
            throw new IllegalStateException(e);
        }
        DegreeOfSuccess degree = DegreeOfSuccess.determine(natural, dc, OptionalInt.of(natural));
        int[] before = snapshot(set);
        DyingRules.recover(set, degree);
        String name = ConditionService.nameOf(id);
        var line = CombatMessages.recoveryCheck(name, natural, dc, degree, set.value(Condition.DYING));
        for (ServerPlayerEntity r : CombatService.recipients(null)) {
            r.sendMessage(line, false);
        }
        Pf2Craft.LOGGER.info("Recovery check of {}: d20 {} vs DC {} -> {}, dying {} (was {})", id, natural, dc, degree,
                set.value(Condition.DYING), before[1]);
        reportIfDead(id, set);
        CombatService.sync(null);
    }

    /** HP rose above 0: dying and unconsciousness end, wounded goes up if the player was dying. */
    public static void onHealed(String id) {
        if (!CombatService.active()) {
            return;
        }
        ConditionSet set = ConditionService.of(id);
        if (!set.has(Condition.DYING) && !set.has(Condition.UNCONSCIOUS)) {
            return;
        }
        int[] before = snapshot(set);
        DyingRules.heal(set);
        REPORTED_DEAD.remove(id);
        announceChanges(id, set, before, "healed");
        CombatService.sync(null);
    }

    /** A GM set conditions by hand: report a dying-4 combatant to the GMs, or allow recovery again if it dropped. */
    static void afterEdit(String id) {
        ConditionSet set = ConditionService.of(id);
        if (set.value(Condition.DYING) < ConditionSet.DYING_CAP) {
            REPORTED_DEAD.remove(id);
        }
        reportIfDead(id, set);
    }

    static void forget(String id) {
        REPORTED_DEAD.remove(id);
    }

    static void forgetAll() {
        REPORTED_DEAD.clear();
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private static void reportIfDead(String id, ConditionSet set) {
        if (set.value(Condition.DYING) >= ConditionSet.DYING_CAP && REPORTED_DEAD.add(id)) {
            var line = CombatMessages.wouldBeDead(ConditionService.nameOf(id));
            for (ServerPlayerEntity r : CombatService.recipients(null)) {
                if (GmRoles.isGm(r)) {
                    r.sendMessage(line, false);
                }
            }
        }
    }

    private static int[] snapshot(ConditionSet set) {
        int[] values = new int[TRACKED.length];
        for (int i = 0; i < TRACKED.length; i++) {
            values[i] = set.value(TRACKED[i]);
        }
        return values;
    }

    private static void announceChanges(String id, ConditionSet set, int[] before, String source) {
        for (int i = 0; i < TRACKED.length; i++) {
            int now = set.value(TRACKED[i]);
            if (now != before[i]) {
                ConditionService.announceValue(id, TRACKED[i], now, source);
            }
        }
    }
}
