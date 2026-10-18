package de.pf2craft.combat;

import de.pf2craft.core.combat.Encounter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

/**
 * Makes the combat rules binding for players: whoever is not up is held in place and cannot interact, the
 * active player's walking costs Strides, and vanilla damage and hunger are switched off. Server thread only.
 * Nothing here runs per tick unless a combat is active (except the food refill every 20 ticks).
 */
public final class Enforcement {
    private static final double DRIFT = 0.02;
    private static final double JUMP = 0.5;
    private static final double MAX_STEP = 2.5;
    private static final int MESSAGE_GAP_TICKS = 40;
    private static final int FOOD_INTERVAL = 20;

    /** Where frozen combatant players are held. */
    private static final Map<UUID, Vec3d> ANCHORS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TURN_MSG = new HashMap<>();
    private static final Map<UUID, Integer> LAST_ACTION_MSG = new HashMap<>();
    private static String trackedCurrent;
    private static Object trackedBudget;
    private static Vec3d lastAccepted;

    private Enforcement() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(Enforcement::tick);
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                blocked(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) ->
                blocked(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            return blocked(player) ? TypedActionResult.fail(stack) : TypedActionResult.pass(stack);
        });
        // The current player's attack click belongs to the attack flow; only non-current ones are stopped here.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                blocked(player) ? ActionResult.FAIL : ActionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> !blocked(player));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity) || bypasses(source));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                !(entity instanceof ServerPlayerEntity) || bypasses(source));
    }

    /** Called when a combatant is removed or defeated; nobody stays frozen. */
    public static void onLeft(UUID id) {
        ANCHORS.remove(id);
        LAST_TURN_MSG.remove(id);
        LAST_ACTION_MSG.remove(id);
    }

    /** Called when the combat ends or the server stops. */
    public static void onCombatEnded() {
        ANCHORS.clear();
        LAST_TURN_MSG.clear();
        LAST_ACTION_MSG.clear();
        trackedCurrent = null;
        trackedBudget = null;
        lastAccepted = null;
    }

    private static boolean bypasses(DamageSource source) {
        return source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** True for a combatant player who may not act right now (not current, or out of the order). */
    private static boolean blocked(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity sp) || !CombatService.active()) {
            return false;
        }
        String id = sp.getUuid().toString();
        String current = CombatService.currentId().orElse(null);
        for (Encounter.Entry e : CombatService.encounter().get().order()) {
            if (e.combatant().id().equals(id)) {
                if (e.active() && id.equals(current)) {
                    return false;
                }
                sendLimited(sp, LAST_TURN_MSG, CombatMessages.notYourTurn());
                return true;
            }
        }
        return false;
    }

    private static void sendLimited(ServerPlayerEntity p, Map<UUID, Integer> gate, Text text) {
        int now = p.server.getTicks();
        Integer last = gate.get(p.getUuid());
        if (last == null || now - last >= MESSAGE_GAP_TICKS || now < last) {
            gate.put(p.getUuid(), now);
            p.sendMessage(text.copy().formatted(Formatting.RED), true);
        }
    }

    // ---- tick ------------------------------------------------------------------------------------------------

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % FOOD_INTERVAL == 0) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                p.getHungerManager().setFoodLevel(20);
                p.getHungerManager().setSaturationLevel(20.0f);
            }
        }
        if (!CombatService.active()) {
            if (!ANCHORS.isEmpty() || trackedCurrent != null) {
                onCombatEnded();
            }
            return;
        }
        Encounter enc = CombatService.encounter().get();
        String current = CombatService.currentId().orElse(null);
        Set<UUID> present = new HashSet<>();

        // Turn change: the previous player is held where their turn ended.
        if (current == null ? trackedCurrent != null : !current.equals(trackedCurrent)) {
            if (trackedCurrent != null && lastAccepted != null) {
                ANCHORS.put(UUID.fromString(trackedCurrent), lastAccepted);
            }
            trackedCurrent = current;
            lastAccepted = null;
            trackedBudget = null;
        }
        if (trackedBudget != CombatService.budget()) {
            trackedBudget = CombatService.budget();
            lastAccepted = null; // a fresh turn: count from wherever the player stands now
        }

        for (Encounter.Entry e : enc.order()) {
            if (e.combatant().npc()) {
                continue;
            }
            UUID id = UUID.fromString(e.combatant().id());
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null) {
                continue;
            }
            present.add(id);
            if (e.active() && e.combatant().id().equals(current)) {
                ANCHORS.remove(id);
                moveCurrent(p);
            } else {
                hold(p);
            }
        }
        ANCHORS.keySet().retainAll(present);
    }

    private static void hold(ServerPlayerEntity p) {
        Vec3d pos = p.getPos();
        Vec3d anchor = ANCHORS.get(p.getUuid());
        if (anchor == null) {
            ANCHORS.put(p.getUuid(), pos);
            return;
        }
        double dx = pos.x - anchor.x;
        double dz = pos.z - anchor.z;
        boolean jumped = pos.y - anchor.y > JUMP;
        if (dx * dx + dz * dz > DRIFT * DRIFT || jumped) {
            double y = jumped ? anchor.y : pos.y;
            teleport(p, anchor.x, y, anchor.z);
        } else if (p.isOnGround()) {
            ANCHORS.put(p.getUuid(), new Vec3d(anchor.x, pos.y, anchor.z));
        }
    }

    private static void moveCurrent(ServerPlayerEntity p) {
        Vec3d pos = p.getPos();
        if (lastAccepted == null) {
            lastAccepted = pos;
            return;
        }
        double dx = pos.x - lastAccepted.x;
        double dz = pos.z - lastAccepted.z;
        double delta = Math.sqrt(dx * dx + dz * dz);
        if (delta <= 0.0) {
            lastAccepted = pos;
            return;
        }
        if (delta > MAX_STEP) {
            teleport(p, lastAccepted.x, pos.y, lastAccepted.z); // lag spike or teleport: undone, nothing spent
            return;
        }
        int before = CombatService.turn().actionsLeft();
        if (CombatService.budget().tryMove(delta, CombatService.turn())) {
            lastAccepted = pos;
            if (CombatService.turn().actionsLeft() != before) {
                CombatService.sync(null);
            }
        } else {
            teleport(p, lastAccepted.x, pos.y, lastAccepted.z);
            sendLimited(p, LAST_ACTION_MSG, CombatMessages.noActions());
        }
    }

    private static void teleport(ServerPlayerEntity p, double x, double y, double z) {
        p.networkHandler.requestTeleport(x, y, z, p.getYaw(), p.getPitch());
        p.setVelocity(Vec3d.ZERO);
        p.velocityModified = true;
    }
}
