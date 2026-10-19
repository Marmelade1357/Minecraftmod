package de.pf2craft.combat;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.combat.Combatant;
import de.pf2craft.core.combat.Condition;
import de.pf2craft.core.combat.ConditionEffects;
import de.pf2craft.core.combat.ConditionSet;
import de.pf2craft.core.combat.Encounter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Conditions of the running combat, per combatant id. In memory only, like {@link CombatService}; cleared when the
 * combatant leaves or the combat ends. Static, server thread only.
 */
public final class ConditionService {
    private static final Map<String, ConditionSet> SETS = new HashMap<>();
    /** Grabbed combatant id -> the id of the creature holding them. */
    private static final Map<String, String> GRABBED_BY = new HashMap<>();

    private ConditionService() {
    }

    /** The conditions of one combatant; an empty set is created on demand. */
    public static ConditionSet of(String id) {
        return SETS.computeIfAbsent(id, k -> new ConditionSet());
    }

    public static void clear(String id) {
        SETS.remove(id);
        GRABBED_BY.entrySet().removeIf(e -> e.getKey().equals(id) || e.getValue().equals(id));
        DyingService.forget(id);
    }

    public static void clearAll() {
        SETS.clear();
        GRABBED_BY.clear();
        DyingService.forgetAll();
    }

    public static boolean canAct(String id) {
        ConditionSet set = SETS.get(id);
        return set == null || ConditionEffects.canAct(set);
    }

    /** Applies the condition (the higher value wins) and tells the audience. */
    public static void add(String id, Condition condition, int value, String source) {
        ConditionSet set = of(id);
        int before = set.value(condition);
        set.apply(condition, value);
        if (set.value(condition) == before) {
            return; // nothing changed, nothing to announce
        }
        announce(id, condition, CombatMessages.conditionGained(nameOf(id), condition, set.value(condition)),
                set.value(condition), source);
    }

    public static void remove(String id, Condition condition) {
        ConditionSet set = SETS.get(id);
        if (set == null || !set.has(condition)) {
            return;
        }
        set.remove(condition);
        if (condition == Condition.GRABBED) {
            GRABBED_BY.remove(id);
        }
        announce(id, condition, CombatMessages.conditionLost(nameOf(id), condition), 0, "removed");
    }

    /** {@code targetId} is now grabbed by {@code grabberId}. */
    public static void grab(String targetId, String grabberId, String source) {
        GRABBED_BY.put(targetId, grabberId);
        add(targetId, Condition.GRABBED, 1, source);
    }

    /** The id of the creature holding {@code targetId}, if known. */
    public static Optional<String> grabberOf(String targetId) {
        return Optional.ofNullable(GRABBED_BY.get(targetId));
    }

    /** The grabber moved: everyone it held is free. */
    public static void onGrabberMoved(String grabberId) {
        if (!GRABBED_BY.containsValue(grabberId)) {
            return;
        }
        for (String held : new ArrayList<>(GRABBED_BY.keySet())) {
            if (GRABBED_BY.get(held).equals(grabberId)) {
                remove(held, Condition.GRABBED);
            }
        }
    }

    /**
     * A combatant's turn begins (the fresh {@link de.pf2craft.core.combat.TurnState} is already in place): slowed
     * and stunned cost actions right away, and stunned shrinks by the actions it took.
     */
    public static void onTurnStart(String id) {
        ConditionSet set = SETS.get(id);
        if (set == null) {
            return;
        }
        int lost = ConditionEffects.actionsLost(set);
        if (lost > 0) {
            int spent = Math.min(lost, CombatService.turn().actionsLeft());
            CombatService.turn().spendActions(spent);
            int stunnedLost = Math.min(set.value(Condition.STUNNED), lost);
            if (stunnedLost > 0) {
                set.set(Condition.STUNNED, set.value(Condition.STUNNED) - stunnedLost);
            }
            sendTo(id, false, CombatMessages.actionsLost(nameOf(id), spent));
        }
        DyingService.recover(id); // a dying combatant rolls its recovery check at the start of its turn
    }

    /** A combatant's turn ends: frightened fades by one. */
    public static void onTurnEnd(String id) {
        ConditionSet set = SETS.get(id);
        if (set == null || !set.has(Condition.FRIGHTENED)) {
            return;
        }
        set.decrement(Condition.FRIGHTENED);
        if (!set.has(Condition.FRIGHTENED)) {
            announce(id, Condition.FRIGHTENED, CombatMessages.conditionLost(nameOf(id), Condition.FRIGHTENED), 0,
                    "turn end");
        }
    }

    /** Tells the audience that the condition now has {@code value} (0 = gone); used by {@link DyingService}. */
    static void announceValue(String id, Condition condition, int value, String source) {
        Text line = value > 0 ? CombatMessages.conditionGained(nameOf(id), condition, value)
                : CombatMessages.conditionLost(nameOf(id), condition);
        announce(id, condition, line, value, source);
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private static void announce(String id, Condition condition, Text line, int value, String source) {
        sendTo(id, condition.publicToAll(), line);
        Pf2Craft.LOGGER.info("Condition {} of {} is now {} ({})", condition.id(), id, value, source);
        CombatService.sync(null);
    }

    /** To everybody in the combat if {@code everyone}, else to the affected player and all GMs. */
    private static void sendTo(String id, boolean everyone, Text line) {
        for (ServerPlayerEntity r : CombatService.recipients(null)) {
            if (everyone || GmRoles.isGm(r) || r.getUuid().toString().equals(id)) {
                r.sendMessage(line, false);
            }
        }
    }

    static String nameOf(String id) {
        if (CombatService.encounter().isPresent()) {
            for (Encounter.Entry e : CombatService.encounter().get().order()) {
                Combatant c = e.combatant();
                if (c.id().equals(id)) {
                    return c.name();
                }
            }
        }
        return id;
    }
}
