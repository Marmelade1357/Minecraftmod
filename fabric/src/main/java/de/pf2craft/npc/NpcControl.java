package de.pf2craft.npc;

import de.pf2craft.GmRoles;
import de.pf2craft.combat.ActionService;
import de.pf2craft.combat.AttackService;
import de.pf2craft.combat.CombatMessages;
import de.pf2craft.combat.CombatService;
import de.pf2craft.combat.ConditionService;
import de.pf2craft.network.ControlInputPayload;
import de.pf2craft.network.ControlStatePayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A GM takes over an NPC: the NPC's own AI is off and the GM's movement input and look direction drive it.
 * During a combat the NPC obeys the same stride rules as a player (see {@code Enforcement.moveCurrent});
 * outside a combat it moves freely. The GM's own body stays where it was and cannot be hurt. Static, server
 * thread only, in memory (a restart releases everything).
 */
public final class NpcControl {
    public static final double MAX_DISTANCE = 64.0;
    /** Beyond this the NPC is out of the GM's tracking range (the camera cannot follow): control is released. */
    private static final double RELEASE_DISTANCE = 80.0;
    /** How far the controlled NPC's crosshair reaches for an attack target. */
    private static final double ATTACK_RAY = 64.0;
    private static final double EDGE = 0.05;
    private static final double MAX_STEP = 2.5;
    private static final double GM_DRIFT = 0.3;
    private static final int STALE_TICKS = 4; // movement input older than this counts as released keys
    private static final int MAX_INPUTS_PER_TICK = 4;
    private static final int HINT_GAP_TICKS = 40;
    /** Scales the vanilla walking speed (a villager's attribute is made for goals, which use about 0.6 of it). */
    private static final float SPEED_SCALE = 0.17f;

    private static final class Control {
        final UUID npcId;
        final Vec3d gmAnchor;
        final RegistryKey<World> gmWorld;
        float forward;
        float sideways;
        boolean jump;
        boolean sprint;
        float yaw;
        float pitch;
        boolean hasLook;
        int inputTick = Integer.MIN_VALUE;
        int inputsThisTick;
        // combat movement tracking (mirrors Enforcement)
        Object trackedBudget;
        Vec3d lastAccepted;
        Vec3d blockAnchor;
        boolean strideEnded;
        int lastHint = Integer.MIN_VALUE;

        Control(UUID npcId, Vec3d gmAnchor, RegistryKey<World> gmWorld) {
            this.npcId = npcId;
            this.gmAnchor = gmAnchor;
            this.gmWorld = gmWorld;
        }
    }

    private static final Map<UUID, Control> CONTROLS = new HashMap<>();

    private NpcControl() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NpcControl::tick);
        // The GM's own body is protected while the GM looks through the NPC's eyes.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !CONTROLS.containsKey(entity.getUuid()));
    }

    // ---- state ------------------------------------------------------------------------------------------------

    public static boolean controlling(UUID gm) {
        return CONTROLS.containsKey(gm);
    }

    public static Optional<NpcEntity> controlled(MinecraftServer server, UUID gm) {
        Control c = CONTROLS.get(gm);
        return c == null ? Optional.empty() : Optional.ofNullable(find(server, c.npcId));
    }

    /** True if some GM controls this NPC. */
    public static boolean isControlled(UUID npcId) {
        for (Control c : CONTROLS.values()) {
            if (c.npcId.equals(npcId)) {
                return true;
            }
        }
        return false;
    }

    // ---- take and release -------------------------------------------------------------------------------------

    /** Handler of {@code TakeOverPayload} and the command fallback. */
    public static void request(ServerPlayerEntity gm, boolean take) {
        if (take) {
            take(gm);
        } else if (controlling(gm.getUuid())) {
            release(gm);
        }
    }

    public static boolean take(ServerPlayerEntity gm) {
        if (!GmRoles.isGm(gm)) {
            CombatMessages.error(gm, CombatMessages.notGm());
            return false;
        }
        if (controlling(gm.getUuid())) {
            CombatMessages.error(gm, CombatMessages.controlAlready());
            return false;
        }
        NpcEntity npc = GmSelection.get(gm).orElse(null);
        if (npc == null) {
            CombatMessages.error(gm, net.minecraft.text.Text.translatableWithFallback("pf2craft.npc.none_selected",
                    "No NPC selected. Right-click an NPC with the staff first."));
            return false;
        }
        if (npc.stats().defeated()) {
            CombatMessages.error(gm, CombatMessages.controlDefeated());
            return false;
        }
        if (npc.getWorld() != gm.getWorld() || npc.squaredDistanceTo(gm) >= MAX_DISTANCE * MAX_DISTANCE) {
            CombatMessages.error(gm, CombatMessages.controlTooFar());
            return false;
        }
        if (isControlled(npc.getUuid())) {
            CombatMessages.error(gm, CombatMessages.controlAlready());
            return false;
        }
        if (CombatService.active()
                && CombatService.currentId().filter(npc.getUuid().toString()::equals).isEmpty()) {
            CombatMessages.error(gm, CombatMessages.controlNotNpcTurn());
            return false;
        }
        de.pf2craft.combat.NpcWalk.cancel(npc); // a click-walk is replaced by the take-over
        npc.setControlled(true);
        CONTROLS.put(gm.getUuid(), new Control(npc.getUuid(), gm.getPos(), gm.getServerWorld().getRegistryKey()));
        sendState(gm, Optional.of(npc.getUuid()));
        gm.sendMessage(CombatMessages.controlTaken(npc.stats().displayName()), true);
        return true;
    }

    /** The GM lets go (key pressed again, command). */
    public static void release(ServerPlayerEntity gm) {
        Control c = CONTROLS.get(gm.getUuid());
        if (c == null) {
            CombatMessages.error(gm, CombatMessages.controlNone());
            return;
        }
        NpcEntity npc = find(gm.getServer(), c.npcId);
        end(gm.getUuid(), gm, npc);
        gm.sendMessage(CombatMessages.controlReleased(npc == null || npc.stats() == null ? "NPC"
                : npc.stats().displayName()), true);
    }

    /** Releases every GM (server stop, combat end). */
    public static void releaseAll(MinecraftServer server) {
        for (UUID gmId : new java.util.ArrayList<>(CONTROLS.keySet())) {
            releaseQuietly(server, gmId);
        }
        CONTROLS.clear();
    }

    /** Called when an NPC leaves the combat or is removed: nobody keeps driving it. */
    public static void releaseNpc(MinecraftServer server, UUID npcId) {
        for (Map.Entry<UUID, Control> e : new java.util.ArrayList<>(CONTROLS.entrySet())) {
            if (e.getValue().npcId.equals(npcId)) {
                releaseQuietly(server, e.getKey());
            }
        }
    }

    /** A turn change: control only survives for the NPC that is up (or outside a combat). */
    public static void onTurnChanged(MinecraftServer server) {
        if (!CombatService.active()) {
            return;
        }
        for (Map.Entry<UUID, Control> e : new java.util.ArrayList<>(CONTROLS.entrySet())) {
            if (CombatService.currentId().filter(e.getValue().npcId.toString()::equals).isEmpty()) {
                releaseQuietly(server, e.getKey());
            }
        }
    }

    /** The GM left the server: just forget the state, the NPC gets its AI back. */
    public static void onDisconnect(ServerPlayerEntity gm) {
        Control c = CONTROLS.remove(gm.getUuid());
        if (c != null) {
            restore(find(gm.getServer(), c.npcId));
        }
    }

    private static void releaseQuietly(MinecraftServer server, UUID gmId) {
        Control c = CONTROLS.get(gmId);
        if (c == null) {
            return;
        }
        ServerPlayerEntity gm = server == null ? null : server.getPlayerManager().getPlayer(gmId);
        NpcEntity npc = server == null ? null : find(server, c.npcId);
        end(gmId, gm, npc);
        if (gm != null) {
            String name = npc == null || npc.stats() == null ? "NPC" : npc.stats().displayName();
            gm.sendMessage(CombatMessages.controlEnded(name), true);
        }
    }

    private static void end(UUID gmId, ServerPlayerEntity gm, NpcEntity npc) {
        CONTROLS.remove(gmId);
        restore(npc);
        if (gm != null) {
            sendState(gm, Optional.empty());
        }
    }

    private static void restore(NpcEntity npc) {
        if (npc == null) {
            return;
        }
        npc.setControlled(false);
        npc.setSprinting(false);
        npc.getNavigation().stop();
    }

    private static void sendState(ServerPlayerEntity gm, Optional<UUID> npc) {
        if (ServerPlayNetworking.canSend(gm, ControlStatePayload.ID)) {
            ServerPlayNetworking.send(gm, new ControlStatePayload(npc));
        }
    }

    // ---- input ------------------------------------------------------------------------------------------------

    /** Handler of {@code ControlInputPayload}; input from anybody who does not control an NPC is ignored. */
    public static void input(ServerPlayerEntity gm, ControlInputPayload in) {
        Control c = CONTROLS.get(gm.getUuid());
        if (c == null) {
            return;
        }
        int now = gm.getServer().getTicks();
        if (c.inputTick == now) {
            if (++c.inputsThisTick > MAX_INPUTS_PER_TICK) {
                return; // a flood: the newest accepted input stays in force
            }
        } else {
            c.inputTick = now;
            c.inputsThisTick = 1;
        }
        if (!Float.isFinite(in.forward()) || !Float.isFinite(in.sideways()) || !Float.isFinite(in.yaw())
                || !Float.isFinite(in.pitch())) {
            return;
        }
        c.forward = MathHelper.clamp(in.forward(), -1.0f, 1.0f);
        c.sideways = MathHelper.clamp(in.sideways(), -1.0f, 1.0f);
        c.jump = in.jump();
        c.sprint = in.sprint();
        c.yaw = MathHelper.wrapDegrees(in.yaw());
        c.pitch = MathHelper.clamp(in.pitch(), -90.0f, 90.0f);
        c.hasLook = true;
    }

    // ---- attack -----------------------------------------------------------------------------------------------

    /** Handler of {@code ControlActionPayload}. */
    public static void action(ServerPlayerEntity gm, String kind) {
        Control c = CONTROLS.get(gm.getUuid());
        if (c == null || !"attack".equals(kind)) {
            return;
        }
        NpcEntity npc = find(gm.getServer(), c.npcId);
        if (npc == null || npc.stats() == null) {
            return;
        }
        if (!CombatService.active()) {
            CombatMessages.error(gm, CombatMessages.notRunning());
            return;
        }
        Vec3d start = npc.getEyePos();
        Vec3d dir = npc.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(ATTACK_RAY));
        EntityHitResult hit = ProjectileUtil.raycast(npc, start, end,
                npc.getBoundingBox().stretch(dir.multiply(ATTACK_RAY)).expand(1.0),
                e -> e != npc && !e.isSpectator() && (e instanceof NpcEntity || e instanceof ServerPlayerEntity),
                ATTACK_RAY * ATTACK_RAY);
        if (hit == null) {
            hint(gm, c, CombatMessages.controlNoTarget());
            return;
        }
        Entity target = hit.getEntity();
        if (ActionService.hasPending(gm) && ActionService.executePending(gm, target)) {
            return;
        }
        AttackService.openForNpc(gm, npc, target);
    }

    // ---- tick -------------------------------------------------------------------------------------------------

    private static void tick(MinecraftServer server) {
        if (CONTROLS.isEmpty()) {
            return;
        }
        Iterator<UUID> ids = new java.util.ArrayList<>(CONTROLS.keySet()).iterator();
        while (ids.hasNext()) {
            UUID gmId = ids.next();
            Control c = CONTROLS.get(gmId);
            ServerPlayerEntity gm = server.getPlayerManager().getPlayer(gmId);
            NpcEntity npc = find(server, c.npcId);
            if (gm == null || !GmRoles.isGm(gm) || npc == null || npc.stats() == null || npc.stats().defeated()
                    || npc.getWorld() != gm.getWorld()
                    || npc.squaredDistanceTo(gm) > RELEASE_DISTANCE * RELEASE_DISTANCE
                    || (CombatService.active()
                    && CombatService.currentId().filter(c.npcId.toString()::equals).isEmpty())) {
                if (gm == null) {
                    CONTROLS.remove(gmId);
                    restore(npc);
                } else {
                    releaseQuietly(server, gmId);
                }
                continue;
            }
            holdGm(gm, c);
            if (CombatService.active()) {
                checkCombatMove(gm, npc, c);
            } else {
                c.lastAccepted = null;
                c.blockAnchor = null;
                c.trackedBudget = null;
            }
            drive(server, npc, c);
        }
    }

    /** The GM's body stays put while their view is the NPC's. */
    private static void holdGm(ServerPlayerEntity gm, Control c) {
        if (gm.getServerWorld().getRegistryKey() != c.gmWorld) {
            return;
        }
        if (gm.getPos().squaredDistanceTo(c.gmAnchor) > GM_DRIFT * GM_DRIFT) {
            gm.networkHandler.requestTeleport(c.gmAnchor.x, c.gmAnchor.y, c.gmAnchor.z, gm.getYaw(), gm.getPitch());
            gm.setVelocity(Vec3d.ZERO);
            gm.velocityModified = true;
        }
    }

    /** Sets the NPC's look direction and the movement input the next vanilla movement tick will use. */
    private static void drive(MinecraftServer server, NpcEntity npc, Control c) {
        if (c.hasLook) {
            npc.setYaw(c.yaw);
            npc.setPitch(c.pitch);
            npc.setHeadYaw(c.yaw);
            npc.setBodyYaw(c.yaw);
        }
        boolean fresh = (long) server.getTicks() - c.inputTick <= STALE_TICKS;
        float forward = fresh ? c.forward : 0.0f;
        float sideways = fresh ? c.sideways : 0.0f;
        boolean sprint = fresh && c.sprint && forward > 0.0f;
        double len = Math.sqrt(forward * forward + sideways * sideways);
        if (len > 1.0) {
            forward /= (float) len;
            sideways /= (float) len;
        }
        npc.setSprinting(sprint);
        float speed = (float) npc.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * SPEED_SCALE;
        npc.setDrive(forward, sideways, speed, fresh && c.jump);
    }

    /**
     * Same rules as {@code Enforcement.moveCurrent}: without a Stride the NPC stays on its block, with one every
     * accepted step is paid from the stride; steps that are not paid for are undone.
     */
    private static void checkCombatMove(ServerPlayerEntity gm, NpcEntity npc, Control c) {
        Vec3d pos = npc.getPos();
        if (c.trackedBudget != CombatService.budget() || c.lastAccepted == null) {
            c.trackedBudget = CombatService.budget();
            c.lastAccepted = pos;
            c.blockAnchor = null;
            c.strideEnded = false;
            return;
        }
        String id = npc.getUuid().toString();
        if (!ConditionService.canAct(id) || !CombatService.budget().strideActive()) {
            if (c.blockAnchor == null) {
                c.blockAnchor = c.lastAccepted;
            }
            if (outsideBlock(c.blockAnchor, pos)) {
                moveBack(npc, c.blockAnchor.x, pos.y, c.blockAnchor.z);
                hint(gm, c, c.strideEnded ? CombatMessages.strideEnd() : CombatMessages.chooseStride());
            } else {
                c.lastAccepted = pos; // the first accepted position of the next Stride is the current one
            }
            return;
        }
        c.blockAnchor = null;
        c.strideEnded = false;
        double dx = pos.x - c.lastAccepted.x;
        double dz = pos.z - c.lastAccepted.z;
        double delta = Math.sqrt(dx * dx + dz * dz);
        if (delta <= 0.0) {
            c.lastAccepted = pos;
            return;
        }
        if (delta > MAX_STEP) {
            moveBack(npc, c.lastAccepted.x, pos.y, c.lastAccepted.z);
            return;
        }
        int before = CombatService.turn().actionsLeft();
        int strideBefore = (int) Math.ceil(CombatService.budget().remainingInStride());
        if (CombatService.budget().moveWithinStride(delta)) {
            c.lastAccepted = pos;
            c.strideEnded = !CombatService.budget().strideActive();
            if (delta > EDGE) {
                CombatService.effects().onMoved(id);
                ConditionService.onGrabberMoved(id);
            }
            if (CombatService.turn().actionsLeft() != before
                    || (int) Math.ceil(CombatService.budget().remainingInStride()) != strideBefore) {
                CombatService.sync(null);
            }
        } else {
            moveBack(npc, c.lastAccepted.x, pos.y, c.lastAccepted.z);
            hint(gm, c, CombatMessages.strideEnd());
        }
    }

    private static boolean outsideBlock(Vec3d anchor, Vec3d pos) {
        double minX = MathHelper.floor(anchor.x) - EDGE;
        double minZ = MathHelper.floor(anchor.z) - EDGE;
        return pos.x < minX || pos.x > minX + 1 + 2 * EDGE || pos.z < minZ || pos.z > minZ + 1 + 2 * EDGE;
    }

    private static void moveBack(NpcEntity npc, double x, double y, double z) {
        npc.refreshPositionAndAngles(x, y, z, npc.getYaw(), npc.getPitch());
        npc.setVelocity(Vec3d.ZERO);
    }

    private static void hint(ServerPlayerEntity gm, Control c, net.minecraft.text.MutableText text) {
        int now = gm.getServer().getTicks();
        if (c.lastHint == Integer.MIN_VALUE || now - c.lastHint >= HINT_GAP_TICKS || now < c.lastHint) {
            c.lastHint = now;
            gm.sendMessage(text.formatted(net.minecraft.util.Formatting.RED), true);
        }
    }

    private static NpcEntity find(MinecraftServer server, UUID id) {
        if (server == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (world.getEntity(id) instanceof NpcEntity npc && !npc.isRemoved()) {
                return npc;
            }
        }
        return null;
    }
}
