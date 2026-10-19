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
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Conditions of the running combat, per combatant id. In memory only, like {@link CombatService}; cleared when the
 * combatant leaves or the combat ends. Static, server thread only.
 */
public final class ConditionService {
    /** Longest condition text of one combatant in the sync payload. */
    public static final int MAX_ENCODED = 96;
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
        // a grabber that leaves the combat lets go of everyone it held (one line per held creature)
        for (String held : new ArrayList<>(GRABBED_BY.keySet())) {
            if (GRABBED_BY.get(held).equals(id) && !held.equals(id)) {
                remove(held, Condition.GRABBED);
            }
        }
        SETS.remove(id);
        GRABBED_BY.entrySet().removeIf(e -> e.getKey().equals(id) || e.getValue().equals(id));
        DyingService.forget(id);
    }

    public static void clearAll() {
        SETS.clear();
        GRABBED_BY.clear();
        DyingService.forgetAll();
    }

    /** Whether the combatant is knocked out (unconscious or dying); such conditions survive a disconnect. */
    public static boolean isDowned(String id) {
        ConditionSet set = SETS.get(id);
        return set != null && (set.has(Condition.UNCONSCIOUS) || set.has(Condition.DYING));
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

    /**
     * A GM sets a condition to an exact value (0 removes it). Everything is validated here: the sender must be a GM,
     * the target a combatant and the value within 0..4 (valued) or 0..1 (unvalued). Errors go to the GM's chat.
     */
    public static void edit(ServerPlayerEntity gm, UUID target, Condition condition, int value) {
        if (!GmRoles.isGm(gm)) {
            CombatMessages.error(gm, CombatMessages.notGm());
            return;
        }
        Optional<Text> error = setValue(target, condition, value);
        error.ifPresent(e -> gm.sendMessage(e.copy().formatted(Formatting.RED), false));
    }

    /** Payload entry: the condition id comes from the client and may be anything. */
    public static void editFromClient(ServerPlayerEntity gm, UUID target, String conditionId, int value) {
        Optional<Condition> condition = Condition.byId(conditionId);
        if (condition.isEmpty()) {
            if (GmRoles.isGm(gm)) {
                gm.sendMessage(unknownCondition(), false);
            } else {
                CombatMessages.error(gm, CombatMessages.notGm());
            }
            return;
        }
        edit(gm, target, condition.get(), value);
    }

    /** Command path: permission is checked by the command. Returns the error, if any. */
    public static Optional<Text> editFromCommand(UUID target, Condition condition, int value) {
        return setValue(target, condition, value);
    }

    /** Translated name with value, e.g. "Frightened 2". */
    public static MutableText describe(Condition condition, int value) {
        return CombatMessages.conditionLabel(condition, value);
    }

    public static MutableText unknownCondition() {
        return Text.translatableWithFallback("pf2craft.condition.unknown", "Unknown condition.")
                .formatted(Formatting.RED);
    }

    private static Optional<Text> setValue(UUID target, Condition condition, int value) {
        if (!CombatService.active()) {
            return Optional.of(CombatMessages.notRunning());
        }
        if (!CombatService.contains(target)) {
            return Optional.of(Text.translatableWithFallback("pf2craft.combat.not_participant",
                    "That creature does not take part in the combat."));
        }
        int max = condition.valued() ? 4 : 1;
        if (value < 0 || value > max) {
            return Optional.of(Text.translatableWithFallback("pf2craft.condition.bad_value",
                    "The value must be between 0 and %s.", max));
        }
        String id = target.toString();
        ConditionSet set = of(id);
        ConditionSet before = set.copy();
        if (value == 0) {
            set.remove(condition);
            if (condition == Condition.GRABBED) {
                GRABBED_BY.remove(id);
            }
            if (condition == Condition.UNCONSCIOUS) {
                set.remove(Condition.DYING); // waking up ends dying
            }
        } else {
            set.set(condition, value);
            if (condition == Condition.DYING) {
                set.set(Condition.UNCONSCIOUS, 1); // a dying creature is always unconscious
            }
        }
        for (Condition c : Condition.values()) {
            int now = set.value(c);
            if (now != before.value(c)) {
                announceValue(id, c, now, "gm edit");
            }
        }
        DyingService.afterEdit(id);
        CombatService.sync(null);
        return Optional.empty();
    }

    /**
     * The conditions of {@code id} as {@code id:value} pairs separated by commas, for the sync payload. Only the
     * public ones unless {@code all}; whole pairs are dropped rather than cut when the text would get too long.
     */
    public static String encode(String id, boolean all) {
        ConditionSet set = SETS.get(id);
        if (set == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Condition, Integer> e : set.all().entrySet()) {
            if (!all && !e.getKey().publicToAll()) {
                continue;
            }
            String pair = e.getKey().id() + ":" + e.getValue();
            if (sb.length() + pair.length() + 1 > MAX_ENCODED) {
                break;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(pair);
        }
        return sb.toString();
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
