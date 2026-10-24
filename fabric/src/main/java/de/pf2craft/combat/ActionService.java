package de.pf2craft.combat;

import de.pf2craft.GmReach;
import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.character.CharacterStore;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.check.DegreeOfSuccess;
import de.pf2craft.core.combat.ActionDcs;
import de.pf2craft.core.combat.ActionKind;
import de.pf2craft.core.combat.AttackMath;
import de.pf2craft.core.combat.BasicAction;
import de.pf2craft.core.combat.BasicActions;
import de.pf2craft.core.combat.CombatEffects;
import de.pf2craft.core.combat.Condition;
import de.pf2craft.core.combat.ConditionEffects;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.item.ItemStamp;
import de.pf2craft.item.Pf2Components;
import de.pf2craft.item.ShieldRef;
import de.pf2craft.network.ActionRequestPayload;
import de.pf2craft.network.PendingActionPayload;
import de.pf2craft.npc.GmSelection;
import de.pf2craft.npc.NpcEntity;
import de.pf2craft.roll.RollService;
import de.pf2craft.spell.SpellService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Server side of the basic actions (Step, Raise a Shield, Take Cover, Seek, Demoralize, Trip, Grapple). Every
 * request is a wish: the actor, the turn, the cost, the shield and the target are checked again, and only then
 * the actions are spent. Target actions without a target become a pending action that the next click on a
 * creature (or the fallback command) completes. Static, server thread only.
 */
public final class ActionService {
    private static final long COOLDOWN_MS = 300;
    private static final double EPSILON = 1e-6;
    private static final double MELEE_REACH_BLOCKS = 1.0;

    /** An action waiting for its target; {@code actorId} is the player or the NPC the sender acts for. */
    private record Pending(String actionId, UUID actorId) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTION = new HashMap<>();

    private ActionService() {
    }

    /** Who performs the action: a combatant player (with a sheet) or an NPC controlled by a GM. */
    private record Actor(String id, String name, Entity entity, CharacterSheet sheet, NpcEntity npc) {
        int skillBonus(String skill) {
            return sheet != null ? ActionDcs.skillBonus(sheet, skill)
                    : ActionDcs.skillBonus(npc.stats().base(), skill);
        }
    }

    /** A validated target with the DC the action tests against. */
    private record Target(Entity entity, String name, int dc) {
    }

    /** Either a validated target or the reason it is not valid. */
    private record Checked(Target target, MutableText error) {
    }

    // ---- state -----------------------------------------------------------------------------------------------

    /** True if the sender has an action or a spell waiting for its target. */
    public static boolean hasPending(ServerPlayerEntity sender) {
        return PENDING.containsKey(sender.getUuid()) || SpellService.hasPending(sender);
    }

    /** Drops the pending action (or spell) of one sender and tells their client. */
    public static void clearPending(UUID sender) {
        boolean hadAction = PENDING.remove(sender) != null;
        boolean hadSpell = SpellService.dropPending(sender);
        if (!hadAction && !hadSpell) {
            return;
        }
        MinecraftServer server = CombatService.server();
        ServerPlayerEntity player = server == null ? null : server.getPlayerManager().getPlayer(sender);
        if (player != null) {
            sendPending(player, "");
        }
    }

    public static void clearAllPending() {
        java.util.Set<UUID> all = new java.util.HashSet<>(PENDING.keySet());
        all.addAll(SpellService.pendingPlayers());
        for (UUID sender : all) {
            clearPending(sender);
        }
    }

    /** A player left the server: nothing to tell them, just forget. */
    public static void forget(UUID sender) {
        PENDING.remove(sender);
        SpellService.forget(sender);
        LAST_ACTION.remove(sender);
    }

    private static void sendPending(ServerPlayerEntity player, String actionId) {
        if (ServerPlayNetworking.canSend(player, PendingActionPayload.ID)) {
            ServerPlayNetworking.send(player, new PendingActionPayload(actionId));
        }
    }

    // ---- entry points ----------------------------------------------------------------------------------------

    /** The menu's request. Nothing in it is trusted; see the class comment. */
    public static void request(ServerPlayerEntity sender, ActionRequestPayload p) {
        if (ActionRequestPayload.CANCEL.equals(p.actionId())) {
            clearPending(sender.getUuid());
            return;
        }
        if (ActionRequestPayload.CONFIRM.equals(p.actionId())) {
            SpellService.confirm(sender);
            return;
        }
        Optional<BasicAction> found = BasicActions.byId(p.actionId());
        if (found.isEmpty()) {
            CombatMessages.error(sender, CombatMessages.actionUnknown());
            return;
        }
        BasicAction action = found.get();
        if (cooldownActive(sender, false)) {
            return;
        }
        Actor actor = prepareActor(sender, p.actorId(), action, false);
        if (actor == null) {
            return;
        }
        if (action.kind() == ActionKind.SELF) {
            execute(sender, actor, action, null);
            return;
        }
        if (p.targetEntityId() < 0) {
            setPending(sender, new Pending(action.id(), p.actorId()));
            return;
        }
        Checked checked = prepareTarget(actor, action, actor.entity().getWorld().getEntityById(p.targetEntityId()));
        if (checked.error() != null) {
            CombatMessages.error(sender, checked.error());
            return;
        }
        execute(sender, actor, action, checked.target());
    }

    /**
     * The click on a creature while an action is pending. A failed target check keeps the pending action and
     * shows a hint; a failed actor check (turn over, no actions left) drops it.
     *
     * @return whether the click was consumed: true only if the action ran or the pending action was dropped
     *         because it could no longer be performed; false for an invalid target or the cooldown
     */
    public static boolean executePending(ServerPlayerEntity sender, Entity target) {
        if (SpellService.hasPending(sender)) {
            return SpellService.executePending(sender, target);
        }
        Pending pending = PENDING.get(sender.getUuid());
        if (pending == null) {
            return false;
        }
        Optional<BasicAction> found = BasicActions.byId(pending.actionId());
        if (found.isEmpty()) {
            clearPending(sender.getUuid());
            return true;
        }
        if (cooldownActive(sender, true)) {
            return false;
        }
        BasicAction action = found.get();
        Actor actor = prepareActor(sender, pending.actorId(), action, true);
        if (actor == null) {
            clearPending(sender.getUuid());
            return true;
        }
        Checked checked = prepareTarget(actor, action, target);
        if (checked.error() != null) {
            AttackService.hint(sender, checked.error());
            return false;
        }
        execute(sender, actor, action, checked.target());
        return true;
    }

    /**
     * Fallback {@code /pf2 combat action <id>}: a combatant acts for themselves, a GM for the selected NPC unless
     * the GM is up themselves. Target actions use the creature in view; without one they become pending.
     */
    public static boolean fallback(ServerPlayerEntity player, String actionId) {
        if (ActionRequestPayload.CANCEL.equals(actionId)) {
            clearPending(player.getUuid());
            return true;
        }
        if (ActionRequestPayload.CONFIRM.equals(actionId)) {
            SpellService.confirm(player);
            return true;
        }
        Optional<BasicAction> found = BasicActions.byId(actionId);
        if (found.isEmpty()) {
            CombatMessages.error(player, CombatMessages.actionUnknown());
            return false;
        }
        BasicAction action = found.get();
        UUID actorId = player.getUuid();
        Entity actorEntity = player;
        boolean playerIsCurrent = CombatService.currentId().filter(player.getUuid().toString()::equals).isPresent();
        if (GmRoles.isGm(player) && !playerIsCurrent) {
            Optional<NpcEntity> npc = GmSelection.get(player);
            if (npc.isEmpty()) {
                CombatMessages.error(player, CombatMessages.actionNoSelection());
                return false;
            }
            actorId = npc.get().getUuid();
            actorEntity = npc.get();
        }
        int targetId = -1;
        if (action.kind() != ActionKind.SELF) {
            Entity looked = lookedAtCreature(player, actorEntity);
            if (looked != null) {
                targetId = looked.getId();
            }
        }
        request(player, new ActionRequestPayload(actorId, actionId, targetId));
        return true;
    }

    /** True if it is this player's turn in a running combat and the stack is a shield made for them. */
    public static boolean isOwnShield(ServerPlayerEntity player, ItemStack stack) {
        ShieldRef ref = stack.get(Pf2Components.SHIELD);
        return ref != null && ref.owner().equals(player.getUuid()) && CombatService.active()
                && CombatService.currentId().filter(player.getUuid().toString()::equals).isPresent();
    }

    /** Right-click with the own shield: the same request the menu sends for Raise a Shield. */
    public static void raiseShieldByItem(ServerPlayerEntity player) {
        request(player, new ActionRequestPayload(player.getUuid(), "raise_shield", -1));
    }

    private static Entity lookedAtCreature(ServerPlayerEntity player, Entity actorEntity) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        double range = GmReach.lookRange(player);
        EntityHitResult hit = ProjectileUtil.raycast(player, start, start.add(dir.multiply(range)),
                player.getBoundingBox().stretch(dir.multiply(range)).expand(1.0),
                e -> e != actorEntity && !e.isSpectator() && (e instanceof NpcEntity || e instanceof ServerPlayerEntity),
                range * range);
        return hit == null ? null : hit.getEntity();
    }

    // ---- validation ------------------------------------------------------------------------------------------

    private static boolean cooldownActive(ServerPlayerEntity sender, boolean interactive) {
        Long last = LAST_ACTION.get(sender.getUuid());
        if (last != null && System.currentTimeMillis() - last < COOLDOWN_MS) {
            if (interactive) {
                AttackService.hint(sender, CombatMessages.attackCooldown());
            } else {
                CombatMessages.error(sender, CombatMessages.attackCooldown());
            }
            return true;
        }
        return false;
    }

    /** Checks sender, turn, cost and shield; null (with a message) if the action cannot be performed now. */
    private static Actor prepareActor(ServerPlayerEntity sender, UUID actorId, BasicAction action,
                                      boolean interactive) {
        if (!CombatService.active()) {
            return actorFail(sender, CombatMessages.notRunning(), interactive);
        }
        if (CombatService.currentId().filter(actorId.toString()::equals).isEmpty()) {
            boolean forNpc = !actorId.equals(sender.getUuid()) && GmRoles.isGm(sender);
            return actorFail(sender, forNpc ? CombatMessages.npcNotCurrent() : CombatMessages.notYourTurn(),
                    interactive);
        }
        Actor actor;
        if (actorId.equals(sender.getUuid())) {
            Optional<CharacterSheet> sheet = CharacterStore.get(sender);
            if (sheet.isEmpty()) {
                return actorFail(sender, CombatMessages.actionNoCharacter(), interactive);
            }
            actor = new Actor(actorId.toString(), AttackService.nameOf(sender, sheet.get()), sender,
                    sheet.get(), null);
        } else {
            if (!GmRoles.isGm(sender)) {
                return actorFail(sender, CombatMessages.notYourTurn(), interactive);
            }
            Entity entity = findEntity(sender.getServer(), actorId);
            if (!(entity instanceof NpcEntity npc) || npc.isRemoved() || npc.stats() == null
                    || npc.stats().defeated() || npc.getWorld() != sender.getWorld()) {
                return actorFail(sender, CombatMessages.npcNotCurrent(), interactive);
            }
            actor = new Actor(actorId.toString(), npc.stats().displayName(), npc, null, npc);
        }
        if (!ConditionService.canAct(actor.id())) {
            return actorFail(sender, CombatMessages.cannotAct(), interactive);
        }
        if (CombatService.turn().actionsLeft() < action.cost()) {
            return actorFail(sender, CombatMessages.actionNotEnough(action.cost()), interactive);
        }
        if (action.id().equals("stride")) {
            if (CombatService.budget().strideActive()) {
                return actorFail(sender, CombatMessages.strideActive(), interactive);
            }
            if (CombatService.budget().blocksPerStride() == 0) {
                return actorFail(sender, CombatMessages.noActions(), interactive); // speed 0: cannot move at all
            }
        }
        if (action.id().equals("stand_up") && !ConditionService.of(actor.id()).has(Condition.PRONE)) {
            return actorFail(sender, CombatMessages.notProne(), interactive);
        }
        if (action.id().equals("escape")) {
            if (!ConditionService.of(actor.id()).has(Condition.GRABBED)) {
                return actorFail(sender, CombatMessages.notGrabbed(), interactive);
            }
            if (escapeDc(sender.getServer(), actor).isEmpty()) {
                return actorFail(sender, CombatMessages.actionNoDc(), interactive);
            }
        }
        if (action.needsShield() && shieldBonus(actor).isEmpty()) {
            return actorFail(sender, CombatMessages.noShield(), interactive);
        }
        if (action.needsShield() && CombatService.effects().sources(actor.id()).contains("raise_shield")) {
            return actorFail(sender, CombatMessages.shieldAlreadyRaised(), interactive);
        }
        return actor;
    }

    private static Actor actorFail(ServerPlayerEntity sender, MutableText text, boolean interactive) {
        if (interactive) {
            AttackService.hint(sender, text);
        } else {
            CombatMessages.error(sender, text);
        }
        return null;
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        for (var world : server.getWorlds()) {
            Entity e = world.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** The AC bonus of the actor's shield, if a player holds the shield of their current sheet (NPCs never do). */
    private static OptionalInt shieldBonus(Actor actor) {
        if (actor.sheet() == null || actor.sheet().shield().isEmpty() || !(actor.entity() instanceof ServerPlayerEntity p)) {
            return OptionalInt.empty();
        }
        long stamp = ItemStamp.of(actor.sheet());
        for (ItemStack stack : List.of(p.getMainHandStack(), p.getOffHandStack())) {
            ShieldRef ref = stack.get(Pf2Components.SHIELD);
            if (ref != null && ref.owner().equals(p.getUuid()) && ref.sheetStamp() == stamp) {
                return OptionalInt.of(actor.sheet().shield().get().acBonus());
            }
        }
        return OptionalInt.empty();
    }

    /** The grabber's Fortitude DC for Escape; empty if nobody (known) holds the actor. */
    private static OptionalInt escapeDc(MinecraftServer server, Actor actor) {
        Optional<String> grabber = ConditionService.grabberOf(actor.id());
        if (grabber.isEmpty()) {
            return OptionalInt.empty();
        }
        Entity entity;
        try {
            entity = findEntity(server, UUID.fromString(grabber.get()));
        } catch (IllegalArgumentException e) {
            return OptionalInt.empty();
        }
        if (entity instanceof NpcEntity npc && npc.stats() != null) {
            return ActionDcs.defenseDc(npc.stats().base(), "fortitude");
        }
        if (entity instanceof ServerPlayerEntity player) {
            return CharacterStore.get(player).map(sh -> ActionDcs.defenseDc(sh, "fortitude"))
                    .orElse(OptionalInt.empty());
        }
        return OptionalInt.empty();
    }

    private static Checked prepareTarget(Actor actor, BasicAction action, Entity target) {
        Target ok = null;
        if (target instanceof NpcEntity npc && target != actor.entity() && !npc.isRemoved() && npc.stats() != null
                && !npc.stats().defeated()) {
            OptionalInt dc = ActionDcs.defenseDc(npc.stats().base(), action.defense());
            if (dc.isEmpty()) {
                return bad(CombatMessages.actionNoDc());
            }
            ok = new Target(npc, npc.stats().displayName(), dc.getAsInt());
        } else if (target instanceof ServerPlayerEntity player && target != actor.entity()
                && CombatService.contains(player.getUuid())) {
            Optional<CharacterSheet> sheet = CharacterStore.get(player);
            if (sheet.isPresent() && sheet.get().currentHp() > 0) {
                OptionalInt dc = ActionDcs.defenseDc(sheet.get(), action.defense());
                if (dc.isEmpty()) {
                    return bad(CombatMessages.actionNoDc());
                }
                ok = new Target(player, AttackService.nameOf(player, sheet.get()), dc.getAsInt());
            }
        }
        if (ok == null || ok.entity().getWorld() != actor.entity().getWorld()) {
            return bad(CombatMessages.badNpcTarget());
        }
        double distance = AttackService.edgeDistance(actor.entity().getBoundingBox(), ok.entity().getBoundingBox());
        double reach = action.kind() == ActionKind.TARGET_MELEE ? MELEE_REACH_BLOCKS : action.rangeBlocks();
        if (distance > reach + EPSILON) {
            return bad(CombatMessages.tooFar());
        }
        return new Checked(ok, null);
    }

    private static Checked bad(MutableText text) {
        return new Checked(null, text);
    }

    // ---- pending ---------------------------------------------------------------------------------------------

    private static void setPending(ServerPlayerEntity sender, Pending pending) {
        SpellService.dropPending(sender.getUuid()); // only one thing can wait for a target
        PENDING.put(sender.getUuid(), pending);
        sendPending(sender, pending.actionId());
        sender.sendMessage(CombatMessages.actionPending(pending.actionId()), true);
    }

    // ---- execution -------------------------------------------------------------------------------------------

    /** Everything is validated; roll (before anything is spent), then spend and announce. */
    private static void execute(ServerPlayerEntity sender, Actor actor, BasicAction action, Target target) {
        int natural = 0;
        int total = 0;
        int map = 0;
        DegreeOfSuccess degree = null;
        int escapeDc = 0;
        try {
            if (action.id().equals("escape")) {
                escapeDc = escapeDc(sender.getServer(), actor).orElseThrow();
                natural = RollService.rollQuiet("1d20").naturalD20().orElseThrow();
                total = natural + Math.max(actor.skillBonus("athletics"), actor.skillBonus("acrobatics"))
                        + ConditionEffects.checkPenalty(ConditionService.of(actor.id()));
                degree = DegreeOfSuccess.determine(total, escapeDc, OptionalInt.of(natural));
            } else if (!action.skill().isEmpty()) {
                natural = RollService.rollQuiet("1d20").naturalD20().orElseThrow();
                if (action.countsAsAttack()) {
                    map = AttackMath.mapPenalty(CombatService.turn().nextAttackNumber(), false);
                }
                total = natural + actor.skillBonus(action.skill()) + map
                        + ConditionEffects.checkPenalty(ConditionService.of(actor.id()));
                if (target != null) {
                    degree = DegreeOfSuccess.determine(total, target.dc(), OptionalInt.of(natural));
                }
            }
        } catch (DiceParseException | RuntimeException e) {
            Pf2Craft.LOGGER.error("Action failed", e);
            CombatMessages.error(sender, CombatMessages.attackError(String.valueOf(e.getMessage())));
            return;
        }
        LAST_ACTION.put(sender.getUuid(), System.currentTimeMillis()); // only an accepted action starts the cooldown
        clearPending(sender.getUuid());
        if (!action.id().equals("stride")) {
            CombatService.turn().spendActions(action.cost()); // a Stride is paid by startStride below
        }
        CombatEffects effects = CombatService.effects();
        List<Text> publicLines = new ArrayList<>();
        List<Text> gmLines = new ArrayList<>();
        Runnable applyAfter = null; // conditions are announced after the action's own line
        switch (action.id()) {
            case "stride" -> {
                // Spends the action and fills the budget; deliberately no interrupt() (that would drop the stride).
                CombatService.budget().startStride(CombatService.turn());
                publicLines.add(CombatMessages.actionSay("stride", actor.name()));
            }
            case "step" -> {
                CombatService.budget().grantStep(); // the block is free of Stride costs (NPCs walk it via NpcWalk)
                if (actor.npc() != null) {
                    gmLines.add(CombatMessages.actionGmNpcStep(actor.name()));
                }
                publicLines.add(CombatMessages.actionSay("step", actor.name()));
            }
            case "raise_shield" -> {
                CombatService.budget().interrupt();
                int bonus = shieldBonus(actor).orElse(0);
                effects.addAcBonus(actor.id(), "raise_shield", bonus, CombatEffects.Expiry.OWN_TURN_START);
                publicLines.add(CombatMessages.actionSay("raise_shield", actor.name()));
                gmLines.add(CombatMessages.actionGmShield(bonus));
            }
            case "take_cover" -> {
                CombatService.budget().interrupt();
                effects.addAcBonus(actor.id(), "take_cover", 2, CombatEffects.Expiry.MOVE_OR_OWN_TURN_START);
                publicLines.add(CombatMessages.actionSay("take_cover", actor.name()));
            }
            case "seek" -> {
                CombatService.budget().interrupt();
                publicLines.add(CombatMessages.actionSay("seek", actor.name()));
                gmLines.add(CombatMessages.actionGmSeek(natural, total));
            }
            case "stand_up" -> {
                CombatService.budget().interrupt();
                publicLines.add(CombatMessages.actionSay("stand_up", actor.name()));
                applyAfter = () -> ConditionService.remove(actor.id(), Condition.PRONE);
            }
            case "escape" -> {
                CombatService.budget().interrupt();
                String grabberName = ConditionService.grabberOf(actor.id()).map(ConditionService::nameOf)
                        .orElse("?");
                publicLines.add(CombatMessages.actionLine(actor.name(), "escape", grabberName, degree));
                gmLines.add(CombatMessages.actionGmRoll(natural, total, escapeDc, 0));
                if (degree.isSuccess()) {
                    boolean crit = degree == DegreeOfSuccess.CRITICAL_SUCCESS;
                    applyAfter = () -> {
                        ConditionService.remove(actor.id(), Condition.GRABBED);
                        if (crit) {
                            CombatService.budget().grantStep(); // a critical success also allows a free Step
                        }
                    };
                }
            }
            default -> {
                if (action.countsAsAttack()) {
                    CombatService.turn().recordAttack();
                }
                CombatService.budget().interrupt();
                publicLines.add(CombatMessages.actionLine(actor.name(), action.id(), target.name(), degree));
                gmLines.add(CombatMessages.actionGmRoll(natural, total, target.dc(), map));
                CombatMessages.actionConditionHint(action.id(), degree).ifPresent(gmLines::add);
                if (degree != null && degree.isSuccess()) {
                    String targetId = target.entity().getUuid().toString();
                    boolean crit = degree == DegreeOfSuccess.CRITICAL_SUCCESS;
                    switch (action.id()) {
                        case "demoralize" -> applyAfter = () -> ConditionService.add(targetId, Condition.FRIGHTENED,
                                crit ? 2 : 1, "demoralize");
                        case "trip" -> applyAfter = () -> ConditionService.add(targetId, Condition.PRONE, 1, "trip");
                        case "grapple" -> applyAfter = () -> ConditionService.grab(targetId, actor.id(), "grapple");
                        default -> { }
                    }
                }
            }
        }
        for (ServerPlayerEntity r : sender.getServer().getPlayerManager().getPlayerList()) {
            publicLines.forEach(line -> r.sendMessage(line, false));
            if (GmRoles.isGm(r)) {
                gmLines.forEach(line -> r.sendMessage(line, false));
            }
        }
        Pf2Craft.LOGGER.info("{} uses {} (cost {})", actor.name(), action.id(), action.cost());
        if (applyAfter != null) {
            applyAfter.run();
        }
        CombatService.sync(null);
        // The turn ends by itself shortly after the last action: CombatService.tickAutoEnd looks at it every tick.
    }
}
