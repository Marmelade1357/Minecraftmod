package de.pf2craft.spell;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.character.CharacterStore;
import de.pf2craft.character.SheetService;
import de.pf2craft.combat.ActionService;
import de.pf2craft.combat.AttackService;
import de.pf2craft.combat.CombatMessages;
import de.pf2craft.combat.CombatService;
import de.pf2craft.combat.ConditionService;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Save;
import de.pf2craft.core.check.DegreeOfSuccess;
import de.pf2craft.core.combat.AttackMath;
import de.pf2craft.core.combat.AttackOutcome;
import de.pf2craft.core.combat.ConditionEffects;
import de.pf2craft.core.combat.DamageDice;
import de.pf2craft.core.combat.DamageInstance;
import de.pf2craft.core.combat.DamageOutcome;
import de.pf2craft.core.combat.DamageResolver;
import de.pf2craft.core.combat.DamageRoller;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.spell.CastingType;
import de.pf2craft.core.spell.SpellDefinition;
import de.pf2craft.core.spell.SpellKind;
import de.pf2craft.core.spell.SpellMath;
import de.pf2craft.core.spell.SpellResolver;
import de.pf2craft.core.spell.Spellbook;
import de.pf2craft.core.spell.SpellcastingEntry;
import de.pf2craft.network.PendingActionPayload;
import de.pf2craft.npc.NpcEntity;
import de.pf2craft.npc.NpcService;
import de.pf2craft.roll.RollService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Server side of spell casting. The client only asks ({@code CastSpellPayload}); the entry, spell, rank, slots,
 * turn, actions and every target are checked again here. Spells with targets wait for clicks (the same click
 * path as the basic actions, see {@code ActionService}) and multi-target spells resolve when the last target is
 * chosen or on {@code ActionRequestPayload.CONFIRM}. Slots, focus points and actions are only spent when the
 * spell actually resolves. Spells never increase the attack count, but a spell attack uses the current multiple
 * attack penalty. Outside a combat casting costs no actions (slots are still spent). Static, server thread only.
 */
public final class SpellService {
    private static final long COOLDOWN_MS = 300;
    private static final double EPSILON = 1e-6;
    /** Touch range: the same reach as a melee strike without reach. */
    private static final double TOUCH_BLOCKS = 1.0;
    private static final String PENDING_ID = "cast";

    /** A spell waiting for targets: what was asked for plus the targets chosen so far. */
    private record Pending(int entry, String spell, int rank, List<UUID> targets) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();

    private SpellService() {
    }

    /** Everything a cast needs, derived from the server's own state. */
    private record Plan(CharacterSheet sheet, Spellbook book, SpellcastingEntry entry, int entryIndex,
                        SpellDefinition def, int castRank, int slotRank, boolean usesFocus, int cost,
                        boolean inCombat) {
    }

    /** The outcome for one target, rolled but not yet applied. */
    private record Result(LivingEntity target, AttackService.Vitals before, DegreeOfSuccess degree, int natural,
                          int total, int dc, int map, DamageOutcome damage, int heal) {
    }

    // ---- state -----------------------------------------------------------------------------------------------

    public static boolean hasPending(ServerPlayerEntity player) {
        return PENDING.containsKey(player.getUuid());
    }

    /** Drops a pending spell without telling the client; returns whether there was one. */
    public static boolean dropPending(UUID player) {
        return PENDING.remove(player) != null;
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
        LAST_CAST.remove(player);
    }

    // ---- entry points ----------------------------------------------------------------------------------------

    /** The client's wish to cast: checks everything, then resolves at once or waits for targets. */
    public static void cast(ServerPlayerEntity player, int entryIndex, String spellName, int rank) {
        if (cooldownActive(player, false)) {
            return;
        }
        Plan plan = plan(player, entryIndex, spellName, rank, false);
        if (plan == null) {
            return;
        }
        // Only one thing waits for a target at a time: an old pending action or spell is replaced.
        ActionService.clearPending(player.getUuid());
        SpellDefinition def = plan.def();
        if (def.maxTargets() <= 0 || def.kind() == SpellKind.UTILITY) {
            resolve(player, plan, List.of());
            return;
        }
        Pending pending = new Pending(entryIndex, spellName, rank, new ArrayList<>());
        PENDING.put(player.getUuid(), pending);
        if (ServerPlayNetworking.canSend(player, PendingActionPayload.ID)) {
            ServerPlayNetworking.send(player, new PendingActionPayload(PENDING_ID));
        }
        player.sendMessage(pendingHint(def, pending), true);
    }

    /**
     * The click on a creature while a spell is pending. An invalid target keeps the pending spell and shows a
     * hint; a failed check of the cast itself (turn over, no actions or slots left) drops it.
     *
     * @return whether the click was consumed
     */
    public static boolean executePending(ServerPlayerEntity player, Entity target) {
        Pending pending = PENDING.get(player.getUuid());
        if (pending == null || cooldownActive(player, true)) {
            return false;
        }
        Plan plan = plan(player, pending.entry(), pending.spell(), pending.rank(), true);
        if (plan == null) {
            ActionService.clearPending(player.getUuid());
            return true;
        }
        MutableText error = checkTarget(player, plan, target);
        if (error == null && pending.targets().contains(target.getUuid())) {
            error = SpellMessages.duplicateTarget();
        }
        if (error != null) {
            AttackService.hint(player, error);
            return false;
        }
        pending.targets().add(target.getUuid());
        if (pending.targets().size() >= plan.def().maxTargets()) {
            finish(player, plan, pending);
        } else {
            player.sendMessage(pendingHint(plan.def(), pending), true);
        }
        return true;
    }

    /** Resolves the pending multi-target spell with the targets chosen so far. */
    public static void confirm(ServerPlayerEntity player) {
        Pending pending = PENDING.get(player.getUuid());
        if (pending == null) {
            CombatMessages.error(player, SpellMessages.noPending());
            return;
        }
        if (pending.targets().isEmpty()) {
            CombatMessages.error(player, SpellMessages.confirmNone());
            return;
        }
        if (cooldownActive(player, false)) {
            return;
        }
        Plan plan = plan(player, pending.entry(), pending.spell(), pending.rank(), false);
        if (plan == null) {
            ActionService.clearPending(player.getUuid());
            return;
        }
        finish(player, plan, pending);
    }

    private static MutableText pendingHint(SpellDefinition def, Pending pending) {
        return def.maxTargets() <= 1 ? SpellMessages.pendingSingle(def.name())
                : SpellMessages.pendingMulti(def.name(), pending.targets().size(), def.maxTargets());
    }

    private static boolean cooldownActive(ServerPlayerEntity player, boolean interactive) {
        Long last = LAST_CAST.get(player.getUuid());
        if (last != null && System.currentTimeMillis() - last < COOLDOWN_MS) {
            if (interactive) {
                AttackService.hint(player, CombatMessages.attackCooldown());
            } else {
                CombatMessages.error(player, CombatMessages.attackCooldown());
            }
            return true;
        }
        return false;
    }

    // ---- validation ------------------------------------------------------------------------------------------

    /** Checks the cast against the server's own state; null (with a message) if it is not possible now. */
    private static Plan plan(ServerPlayerEntity player, int entryIndex, String spellName, int rank,
                             boolean interactive) {
        Optional<CharacterSheet> sheet = CharacterStore.get(player);
        if (sheet.isEmpty()) {
            return fail(player, SpellMessages.noCharacter(), interactive);
        }
        Spellbook book = SpellbookStore.get(player);
        if (entryIndex < 0 || entryIndex >= book.entries().size()) {
            return fail(player, SpellMessages.noEntry(), interactive);
        }
        Optional<SpellDefinition> found = SpellData.catalog().byName(spellName);
        if (found.isEmpty()) {
            return fail(player, SpellMessages.notInCatalog(), interactive);
        }
        SpellDefinition def = found.get();
        SpellcastingEntry entry = book.entries().get(entryIndex);
        OptionalInt listed = listedRank(entry, def);
        if (listed.isEmpty()) {
            return fail(player, SpellMessages.notInEntry(), interactive);
        }
        if (rank < 0 || rank >= SpellcastingEntry.RANKS) {
            return fail(player, SpellMessages.badRank(), interactive);
        }
        int autoRank = SpellMath.autoHeightenRank(sheet.get().level());
        int castRank;
        int slotRank = 0;
        boolean usesFocus = false;
        if (listed.getAsInt() == 0) {
            // Cantrips are free and heighten by themselves.
            if (rank != 0) {
                return fail(player, SpellMessages.badRank(), interactive);
            }
            castRank = autoRank;
        } else if (entry.type() == CastingType.FOCUS) {
            castRank = autoRank;
            usesFocus = true;
            if (book.focusCurrent() <= 0) {
                return fail(player, SpellMessages.noFocus(), interactive);
            }
        } else if (entry.type() == CastingType.INNATE) {
            return fail(player, SpellMessages.innateUnsupported(), interactive);
        } else {
            boolean rankOk = entry.type() == CastingType.PREPARED
                    ? rank == listed.getAsInt() : rank >= listed.getAsInt();
            if (!rankOk) {
                return fail(player, SpellMessages.badRank(), interactive);
            }
            if (book.slotsLeft(entryIndex, rank) <= 0) {
                return fail(player, SpellMessages.noSlot(), interactive);
            }
            castRank = rank;
            slotRank = rank;
        }
        int cost = Math.max(1, def.actions());
        boolean inCombat = CombatService.active() && CombatService.contains(player.getUuid());
        if (inCombat) {
            if (CombatService.currentId().filter(player.getUuid().toString()::equals).isEmpty()) {
                return fail(player, CombatMessages.notYourTurn(), interactive);
            }
            if (!ConditionService.canAct(player.getUuid().toString())) {
                return fail(player, CombatMessages.cannotAct(), interactive);
            }
            if (CombatService.turn().actionsLeft() < cost) {
                return fail(player, CombatMessages.actionNotEnough(cost), interactive);
            }
        }
        return new Plan(sheet.get(), book, entry, entryIndex, def, castRank, slotRank, usesFocus, cost, inCombat);
    }

    /** The rank under which the entry lists the spell (name or id, ignoring case); empty if it is not listed. */
    private static OptionalInt listedRank(SpellcastingEntry entry, SpellDefinition def) {
        for (Map.Entry<Integer, List<String>> e : entry.spellsByRank().entrySet()) {
            for (String name : e.getValue()) {
                if (name.equalsIgnoreCase(def.name()) || name.equalsIgnoreCase(def.id())) {
                    return OptionalInt.of(e.getKey());
                }
            }
        }
        return OptionalInt.empty();
    }

    private static Plan fail(ServerPlayerEntity player, MutableText text, boolean interactive) {
        if (interactive) {
            AttackService.hint(player, text);
        } else {
            CombatMessages.error(player, text);
        }
        return null;
    }

    /** Null if the creature is a valid target for the planned spell, else the reason. */
    private static MutableText checkTarget(ServerPlayerEntity caster, Plan plan, Entity target) {
        boolean heal = plan.def().kind() == SpellKind.HEAL;
        if (target instanceof NpcEntity npc) {
            if (npc.isRemoved() || npc.stats() == null || (!heal && npc.stats().defeated())) {
                return SpellMessages.badTarget();
            }
        } else if (target instanceof ServerPlayerEntity player) {
            if (CharacterStore.get(player).isEmpty()) {
                return SpellMessages.badTarget();
            }
            if (player == caster && !heal) {
                return SpellMessages.selfNotAllowed();
            }
        } else {
            return SpellMessages.badTarget();
        }
        if (target.getWorld() != caster.getWorld()) {
            return SpellMessages.badTarget();
        }
        int feet = plan.def().rangeFeet();
        double reach = feet <= 0 ? TOUCH_BLOCKS : feet / 5.0;
        double distance = AttackService.edgeDistance(caster.getBoundingBox(), target.getBoundingBox());
        if (distance > reach + EPSILON) {
            return CombatMessages.tooFar();
        }
        return null;
    }

    // ---- resolving -------------------------------------------------------------------------------------------

    /** Re-validates the chosen targets (they may have moved or died meanwhile) and resolves. */
    private static void finish(ServerPlayerEntity player, Plan plan, Pending pending) {
        List<LivingEntity> valid = new ArrayList<>();
        for (UUID id : pending.targets()) {
            Entity e = player.getServerWorld().getEntity(id);
            if (e instanceof LivingEntity living && checkTarget(player, plan, e) == null) {
                valid.add(living);
            }
        }
        if (valid.isEmpty()) {
            AttackService.hint(player, SpellMessages.badTarget());
            pending.targets().clear();
            return;
        }
        resolve(player, plan, valid);
    }

    private static void resolve(ServerPlayerEntity player, Plan plan, List<LivingEntity> targets) {
        List<Result> results = new ArrayList<>();
        // Everything that can throw is rolled before anything is spent.
        try {
            for (LivingEntity target : targets) {
                results.add(roll(player, plan, target));
            }
        } catch (RuntimeException e) {
            Pf2Craft.LOGGER.error("Spell failed", e);
            CombatMessages.error(player, SpellMessages.failed(String.valueOf(e.getMessage())));
            ActionService.clearPending(player.getUuid());
            return;
        }
        Spellbook book = plan.book();
        if (plan.slotRank() > 0) {
            book.spendSlot(plan.entryIndex(), plan.slotRank());
        } else if (plan.usesFocus()) {
            book.spendFocus();
        }
        if (plan.slotRank() > 0 || plan.usesFocus()) {
            SpellbookStore.set(player, book);
            SheetService.sendSpellbook(player.getServer(), player);
        }
        LAST_CAST.put(player.getUuid(), System.currentTimeMillis());
        ActionService.clearPending(player.getUuid());
        if (plan.inCombat()) {
            CombatService.turn().spendActions(plan.cost());
            CombatService.budget().interrupt(); // a Stride cannot be resumed after another action
        }
        try {
            announce(player, plan, results);
            apply(plan, results);
        } catch (RuntimeException e) {
            Pf2Craft.LOGGER.error("Spell failed", e);
            CombatMessages.error(player, SpellMessages.failed(String.valueOf(e.getMessage())));
        }
        if (CombatService.active()) {
            CombatService.sync(null);
        }
        Pf2Craft.LOGGER.info("{} casts {} at rank {} ({} target(s))", player.getName().getString(),
                plan.def().name(), plan.castRank(), targets.size());
    }

    private static Result roll(ServerPlayerEntity caster, Plan plan, LivingEntity target) {
        SpellDefinition def = plan.def();
        AttackService.Vitals before = AttackService.vitals(target);
        String targetId = target.getUuid().toString();
        if (def.kind() == SpellKind.UTILITY) {
            return new Result(target, before, null, 0, 0, 0, 0, null, 0);
        }
        if (def.kind() == SpellKind.HEAL) {
            int healed = 0;
            for (DamageInstance part : rollParts(plan, 1)) {
                healed += part.amount();
            }
            return new Result(target, before, null, 0, 0, 0, 0, null, healed);
        }
        int natural = 0;
        int total = 0;
        int dc = 0;
        int map = 0;
        DegreeOfSuccess degree = null;
        double factor = 1;
        if (def.kind() == SpellKind.ATTACK) {
            int bonus = SpellMath.spellAttack(plan.sheet(), plan.entry())
                    + ConditionEffects.attackPenalty(ConditionService.of(caster.getUuid().toString()));
            if (plan.inCombat()) {
                map = AttackMath.mapPenalty(Math.min(3, CombatService.turn().nextAttackNumber()), false);
            }
            dc = AttackService.armorClassOf(target);
            AttackOutcome outcome = AttackMath.resolve(rollNatural(), bonus, map, 0, dc);
            natural = outcome.natural();
            total = outcome.total();
            degree = outcome.degree();
            factor = SpellResolver.multiplier(SpellKind.ATTACK, def.basicSave(), degree);
        } else if (def.kind() == SpellKind.SAVE) {
            dc = SpellMath.spellDc(plan.sheet(), plan.entry());
            natural = rollNatural();
            total = natural + saveBonus(target, saveOf(def.save()))
                    + ConditionEffects.checkPenalty(ConditionService.of(targetId));
            degree = DegreeOfSuccess.determine(total, dc, OptionalInt.of(natural));
            factor = SpellResolver.multiplier(SpellKind.SAVE, def.basicSave(), degree);
        }
        List<DamageInstance> parts = rollParts(plan, factor);
        DamageOutcome damage = factor > 0 && !parts.isEmpty()
                ? DamageResolver.resolve(parts, AttackService.defensesOf(target)) : null;
        return new Result(target, before, degree, natural, total, dc, map, damage, 0);
    }

    /** Rolls the damage (or healing) components at the cast rank and scales each by {@code factor}. */
    private static List<DamageInstance> rollParts(Plan plan, double factor) {
        List<DamageDice> dice = new ArrayList<>();
        for (SpellResolver.DamageRoll r : SpellResolver.damageAt(plan.def(), plan.castRank())) {
            int bonus = r.bonus();
            if (dice.isEmpty() && plan.def().addAbilityMod()) {
                bonus += plan.sheet().abilityMod(plan.entry().ability());
            }
            dice.add(new DamageDice(r.dice(), bonus, r.type()));
        }
        List<DamageInstance> out = new ArrayList<>();
        for (DamageInstance rolled : DamageRoller.roll(dice, false, RollService.roller())) {
            out.add(new DamageInstance(rolled.type(), SpellResolver.applyFactor(rolled.amount(), factor)));
        }
        return out;
    }

    private static int rollNatural() {
        try {
            return RollService.rollQuiet("1d20").naturalD20().orElseThrow();
        } catch (DiceParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Save saveOf(String id) {
        for (Save s : Save.values()) {
            if (s.id().equalsIgnoreCase(id)) {
                return s;
            }
        }
        throw new IllegalStateException("Spell has no valid save: " + id);
    }

    private static int saveBonus(LivingEntity target, Save save) {
        if (target instanceof ServerPlayerEntity player) {
            return CharacterMath.save(CharacterStore.get(player).orElseThrow(), save);
        }
        MonsterData base = ((NpcEntity) target).stats().base();
        return switch (save) {
            case FORTITUDE -> base.fortitude();
            case REFLEX -> base.reflex();
            case WILL -> base.will();
        };
    }

    // ---- announcing and applying -----------------------------------------------------------------------------

    /** Public lines for everybody (NPC rolls and hit points hidden), the details for the GMs. */
    private static void announce(ServerPlayerEntity caster, Plan plan, List<Result> results) {
        SpellDefinition def = plan.def();
        List<Text> publicLines = new ArrayList<>();
        List<Text> gmLines = new ArrayList<>();
        publicLines.add(SpellMessages.say(AttackService.nameOf(caster, plan.sheet()), def.name(),
                def.rank() == 0 ? 0 : plan.castRank()));
        if (results.isEmpty()) {
            if (def.kind() == SpellKind.UTILITY) {
                publicLines.add(SpellMessages.utilityLine());
            }
            gmLines.add(SpellMessages.gmNumbers(SpellMath.spellAttack(plan.sheet(), plan.entry()),
                    SpellMath.spellDc(plan.sheet(), plan.entry())));
        }
        for (Result r : results) {
            String name = r.before().name();
            int applied = r.damage() == null ? 0 : Math.max(0, r.damage().total());
            switch (def.kind()) {
                case HEAL -> {
                    int gained = Math.max(0, Math.min(r.heal(), r.before().max() - r.before().hp()));
                    publicLines.add(r.target() instanceof NpcEntity ? SpellMessages.healLineHidden(name)
                            : SpellMessages.healLine(name, gained));
                    gmLines.add(SpellMessages.gmHeal(name, gained, r.before().hp() + gained, r.before().max()));
                }
                case UTILITY -> {
                    publicLines.add(SpellMessages.utilityLine());
                    gmLines.add(SpellMessages.gmNumbers(SpellMath.spellAttack(plan.sheet(), plan.entry()),
                            SpellMath.spellDc(plan.sheet(), plan.entry())));
                }
                default -> {
                    MutableText outcome = switch (def.kind()) {
                        case ATTACK -> SpellMessages.attackOutcome(r.degree());
                        case SAVE -> SpellMessages.saveOutcome(r.degree());
                        default -> SpellMessages.autoOutcome();
                    };
                    publicLines.add(SpellMessages.targetLine(name, outcome, applied));
                    if (def.kind() == SpellKind.ATTACK) {
                        gmLines.add(SpellMessages.gmAttack(name, r.natural(), r.total(), r.dc(), r.map()));
                    } else if (def.kind() == SpellKind.SAVE) {
                        gmLines.add(SpellMessages.gmSave(name, def.save(), r.natural(), r.total(), r.dc()));
                    }
                    if (r.damage() != null && !r.damage().parts().isEmpty()) {
                        gmLines.add(SpellMessages.gmDamage(r.damage().parts().stream()
                                .map(p -> p.type() + " " + p.rolled() + " -> " + p.applied())
                                .collect(Collectors.joining(", "))));
                    }
                    gmLines.add(SpellMessages.gmHp(name, Math.max(0, r.before().hp() - applied), r.before().max()));
                }
            }
        }
        for (ServerPlayerEntity r : caster.getServer().getPlayerManager().getPlayerList()) {
            publicLines.forEach(line -> r.sendMessage(line, false));
            if (GmRoles.isGm(r)) {
                gmLines.forEach(line -> r.sendMessage(line, false));
            }
        }
    }

    private static void apply(Plan plan, List<Result> results) {
        SpellKind kind = plan.def().kind();
        for (Result r : results) {
            if (kind == SpellKind.HEAL) {
                if (r.target() instanceof ServerPlayerEntity player) {
                    SheetService.changeHp(player.getServer(), player, r.heal());
                } else {
                    NpcService.heal((NpcEntity) r.target(), r.heal());
                }
            } else if (r.damage() != null) {
                // A critical hit, or a critical failure of the save, makes a dying player dying 2.
                boolean crit = kind == SpellKind.ATTACK && r.degree() == DegreeOfSuccess.CRITICAL_SUCCESS
                        || kind == SpellKind.SAVE && r.degree() == DegreeOfSuccess.CRITICAL_FAILURE;
                AttackService.applyDamage(r.target(), r.damage().total(), crit);
            }
        }
    }
}
