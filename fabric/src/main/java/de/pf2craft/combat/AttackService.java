package de.pf2craft.combat;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.character.CharacterStore;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Strike;
import de.pf2craft.core.check.DegreeOfSuccess;
import de.pf2craft.core.combat.AttackMath;
import de.pf2craft.core.combat.AttackOutcome;
import de.pf2craft.core.combat.DamageInstance;
import de.pf2craft.core.combat.DamageOutcome;
import de.pf2craft.core.combat.DamageResolver;
import de.pf2craft.core.combat.DamageRoller;
import de.pf2craft.core.combat.Defenses;
import de.pf2craft.core.combat.RangeRules;
import de.pf2craft.core.combat.StrikeDamage;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.item.WeaponData;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.item.CharacterItems;
import de.pf2craft.item.ItemData;
import de.pf2craft.network.AttackConfirmPayload;
import de.pf2craft.network.OpenAttackScreenPayload;
import de.pf2craft.npc.NpcEntity;
import de.pf2craft.npc.NpcService;
import de.pf2craft.roll.RollService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

/**
 * A player's attack against an NPC: opens the attack window, then (on confirmation, or via the fallback
 * command) validates everything again, spends the action, rolls the attack and the damage and tells everybody
 * the outcome without revealing the numbers. Static, server thread only.
 */
public final class AttackService {
    private static final long COOLDOWN_MS = 300;
    private static final int HINT_GAP_TICKS = 40;
    private static final double EPSILON = 1e-6;
    private static final int DEFAULT_THROWN_RANGE_FEET = 60;
    private static final Map<UUID, Long> LAST_CONFIRM = new HashMap<>();
    private static final Map<UUID, Integer> LAST_HINT = new HashMap<>();

    private AttackService() {
    }

    /** Everything an attack needs, derived from the server's own state. */
    private record Prepared(CharacterSheet sheet, int strikeIndex, Strike strike, NpcEntity npc, int rangePenalty) {
    }

    // ---- opening the window ----------------------------------------------------------------------------------

    /** The attack click: checks the preconditions and opens the window on the client, or shows a short hint. */
    public static void openFor(ServerPlayerEntity attacker, Entity target) {
        Prepared p = prepare(attacker, target, true);
        if (p == null) {
            return;
        }
        boolean agile = AttackMath.isAgile(p.strike().traits());
        int bonus = CharacterMath.strikeAttack(p.sheet(), p.strikeIndex());
        List<Integer> bonuses = List.of(bonus + AttackMath.mapPenalty(1, agile),
                bonus + AttackMath.mapPenalty(2, agile), bonus + AttackMath.mapPenalty(3, agile));
        int suggested = Math.min(3, CombatService.turn().nextAttackNumber());
        if (ServerPlayNetworking.canSend(attacker, OpenAttackScreenPayload.ID)) {
            ServerPlayNetworking.send(attacker, new OpenAttackScreenPayload(attacker.getUuid(), target.getId(),
                    List.of(new OpenAttackScreenPayload.StrikeOption(clip(p.strike().name()), bonuses, agile)),
                    suggested));
        }
    }

    /** A short red hint above the hotbar, at most every two seconds per player. */
    public static void hint(ServerPlayerEntity player, Text text) {
        int now = player.server.getTicks();
        Integer last = LAST_HINT.get(player.getUuid());
        if (last == null || now - last >= HINT_GAP_TICKS || now < last) {
            LAST_HINT.put(player.getUuid(), now);
            player.sendMessage(text.copy().formatted(Formatting.RED), true);
        }
    }

    public static void forget(UUID player) {
        LAST_CONFIRM.remove(player);
        LAST_HINT.remove(player);
    }

    // ---- confirming ------------------------------------------------------------------------------------------

    /** Handles the window's answer: nothing in it is trusted, the strike, bonus and target are re-derived. */
    public static void confirm(ServerPlayerEntity sender, AttackConfirmPayload payload) {
        if (!payload.attackerId().equals(sender.getUuid()) || payload.strikeIndex() != 0
                || payload.attackNumber() < 1 || payload.attackNumber() > 3) {
            return;
        }
        Entity target = sender.getServerWorld().getEntityById(payload.targetEntityId());
        perform(sender, target, payload.attackNumber());
    }

    /** Fallback {@code /pf2 combat attack <n>}: attacks the NPC the player is looking at. */
    public static boolean attackLookedAt(ServerPlayerEntity player, int number) {
        Optional<NpcEntity> npc = NpcService.lookedAt(player);
        if (npc.isEmpty()) {
            CombatMessages.error(player, CombatMessages.noTarget());
            return false;
        }
        return perform(player, npc.get(), number);
    }

    private static boolean perform(ServerPlayerEntity attacker, Entity target, int number) {
        long now = System.currentTimeMillis();
        Long last = LAST_CONFIRM.put(attacker.getUuid(), now);
        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }
        Prepared p = prepare(attacker, target, false);
        if (p == null) {
            return false;
        }
        // Nothing is rejected any more: from here on the action is spent.
        CombatService.turn().spendActions(1);
        CombatService.turn().recordAttack();

        NpcEntity npc = p.npc();
        MonsterData base = npc.stats().base();
        boolean agile = AttackMath.isAgile(p.strike().traits());
        int natural;
        try {
            natural = RollService.rollQuiet("1d20").naturalD20().orElseThrow();
        } catch (DiceParseException e) {
            throw new IllegalStateException(e);
        }
        AttackOutcome outcome = AttackMath.resolve(natural, CharacterMath.strikeAttack(p.sheet(), p.strikeIndex()),
                AttackMath.mapPenalty(number, agile), p.rangePenalty(), base.ac());
        DegreeOfSuccess degree = outcome.degree();

        DamageOutcome damage = null;
        if (degree.isSuccess()) {
            List<DamageInstance> rolled = DamageRoller.roll(StrikeDamage.forStrike(p.sheet(), p.strikeIndex()),
                    degree == DegreeOfSuccess.CRITICAL_SUCCESS, RollService.roller());
            damage = DamageResolver.resolve(rolled,
                    new Defenses(base.resistances(), base.weaknesses(), base.immunities()));
        }
        int applied = damage == null ? -1 : Math.max(0, damage.total());
        int hpLeft = Math.max(0, npc.stats().currentHp() - Math.max(0, applied));

        String attackerName = nameOf(attacker, p.sheet());
        String targetName = npc.stats().displayName();
        MutableText publicLine = CombatMessages.attackLine(attackerName, targetName, number, degree, applied);
        List<Text> detail = CombatMessages.attackDetail(outcome.natural(), outcome.total(), base.ac(),
                p.rangePenalty(), damage == null ? List.of() : damage.parts(),
                damage == null ? List.of() : damage.unmatched(), hpLeft, base.hp());
        for (ServerPlayerEntity r : attacker.getServer().getPlayerManager().getPlayerList()) {
            r.sendMessage(publicLine, false);
            if (GmRoles.isGm(r)) {
                detail.forEach(line -> r.sendMessage(line, false));
            }
        }
        Pf2Craft.LOGGER.info("{} attacks {} ({}): d20 {} total {} vs AC {} -> {}, damage {}", attackerName,
                targetName, number, outcome.natural(), outcome.total(), base.ac(), degree, applied);

        if (applied > 0) {
            NpcService.damage(npc, applied); // may defeat the NPC and move the combat on
        }
        CombatService.sync(null);
        return true;
    }

    // ---- validation ------------------------------------------------------------------------------------------

    /**
     * Checks all preconditions against the server's own state. On failure the attacker is told (a rate-limited
     * hint above the hotbar when {@code interactive}, else chat) and null is returned; nothing is spent here.
     */
    private static Prepared prepare(ServerPlayerEntity attacker, Entity target, boolean interactive) {
        if (!CombatService.active()) {
            return fail(attacker, CombatMessages.notRunning(), interactive);
        }
        if (CombatService.currentId().filter(attacker.getUuid().toString()::equals).isEmpty()) {
            return fail(attacker, CombatMessages.notYourTurn(), interactive);
        }
        if (CombatService.turn().actionsLeft() < 1) {
            return fail(attacker, CombatMessages.noAttackActions(), interactive);
        }
        ItemStack held = attacker.getMainHandStack();
        Optional<CharacterSheet> sheet = CharacterStore.get(attacker);
        Optional<Strike> strike = CharacterItems.strikeFor(attacker, held);
        OptionalInt index = CharacterItems.strikeIndexFor(attacker, held);
        if (sheet.isEmpty() || strike.isEmpty() || index.isEmpty()) {
            return fail(attacker, CombatMessages.noWeapon(), interactive);
        }
        if (!(target instanceof NpcEntity npc) || npc.isRemoved() || npc.stats() == null || npc.stats().defeated()
                || npc.getWorld() != attacker.getWorld()) {
            return fail(attacker, CombatMessages.badTarget(), interactive);
        }
        double distance = edgeDistance(attacker.getBoundingBox(), npc.getBoundingBox());
        OptionalInt rangePenalty = rangePenalty(strike.get(), distance);
        if (rangePenalty.isEmpty()) {
            return fail(attacker, CombatMessages.tooFar(), interactive);
        }
        return new Prepared(sheet.get(), index.getAsInt(), strike.get(), npc, rangePenalty.getAsInt());
    }

    private static Prepared fail(ServerPlayerEntity player, MutableText text, boolean interactive) {
        if (interactive) {
            hint(player, text);
        } else {
            CombatMessages.error(player, text);
        }
        return null;
    }

    /** Range penalty for a strike at this distance (blocks, hitbox edge to edge); empty if out of reach/range. */
    private static OptionalInt rangePenalty(Strike strike, double distance) {
        Optional<WeaponData> weapon = ItemData.catalog().weapon(strike.name());
        int incrementFeet = 0;
        if (weapon.isPresent()) {
            incrementFeet = weapon.get().range();
        } else if (strike.traits().contains("thrown") || strike.traits().contains("propulsive")) {
            incrementFeet = DEFAULT_THROWN_RANGE_FEET;
        }
        if (incrementFeet <= 0) {
            return distance <= RangeRules.meleeReachBlocks(strike.traits()) + EPSILON
                    ? OptionalInt.of(0) : OptionalInt.empty();
        }
        return RangeRules.rangePenalty(Math.max(0, distance - EPSILON), incrementFeet);
    }

    private static double edgeDistance(Box a, Box b) {
        double dx = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String nameOf(ServerPlayerEntity player, CharacterSheet sheet) {
        return sheet.name() == null || sheet.name().isBlank() ? player.getName().getString() : sheet.name();
    }

    private static String clip(String s) {
        return s.length() <= OpenAttackScreenPayload.MAX_NAME ? s : s.substring(0, OpenAttackScreenPayload.MAX_NAME);
    }
}
