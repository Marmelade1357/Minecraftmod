package de.pf2craft.combat;

import de.pf2craft.npc.NpcEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * An NPC's walk during a combat: the whole path is paid for in Strides when the GM clicks, and a tick check
 * stops the NPC once it has covered the paid distance. Static, server thread only.
 */
public final class NpcWalk {
    private static final double WALK_SPEED = 0.6;
    private static final double SLACK = 0.5;

    private static final class Walk {
        final double paid;
        Vec3d last;
        double travelled;

        Walk(double paid, Vec3d start) {
            this.paid = paid;
            this.last = start;
        }
    }

    private static final Map<UUID, Walk> WALKS = new HashMap<>();

    private NpcWalk() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(NpcWalk::tick);
    }

    /**
     * The staff's block click during a combat. Only the current combatant may walk; the path must exist and
     * its length in Strides must fit into the actions left, otherwise nothing is spent.
     */
    public static void walk(ServerPlayerEntity gm, NpcEntity npc, BlockPos target) {
        if (CombatService.currentId().filter(npc.getUuid().toString()::equals).isEmpty()) {
            CombatMessages.error(gm, CombatMessages.npcNotCurrent());
            return;
        }
        double x = target.getX() + 0.5;
        double z = target.getZ() + 0.5;
        Path path = npc.getNavigation().findPathTo(x, target.getY(), z, 0);
        if (path == null || path.getLength() <= 0) {
            CombatMessages.error(gm, CombatMessages.noPath());
            return;
        }
        if (!path.reachesTarget()) {
            gm.sendMessage(CombatMessages.pathPartial());
        }
        double length = length(path);
        if (!CombatService.budget().payWalk(length, CombatService.turn())) {
            CombatMessages.error(gm, CombatMessages.notEnoughActions());
            return;
        }
        WALKS.put(npc.getUuid(), new Walk(length, npc.getPos()));
        npc.getNavigation().startMovingAlong(path, WALK_SPEED);
        CombatService.sync(null);
    }

    /** The NPC was defeated: forget its paid walk and stop it. */
    public static void cancel(NpcEntity npc) {
        if (WALKS.remove(npc.getUuid()) != null) {
            npc.getNavigation().stop();
        }
    }

    /** Path length in blocks: the sum of the distances from node to node. */
    private static double length(Path path) {
        double sum = 0;
        for (int i = 1; i < path.getLength(); i++) {
            var a = path.getNode(i - 1);
            var b = path.getNode(i);
            double dx = b.x - a.x;
            double dy = b.y - a.y;
            double dz = b.z - a.z;
            sum += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return sum;
    }

    private static void tick(MinecraftServer server) {
        if (WALKS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Walk>> it = WALKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Walk> entry = it.next();
            NpcEntity npc = find(server, entry.getKey());
            Walk walk = entry.getValue();
            if (npc == null || !CombatService.active()) {
                it.remove();
                continue;
            }
            Vec3d now = npc.getPos();
            walk.travelled += now.distanceTo(walk.last);
            walk.last = now;
            if (walk.travelled > walk.paid + SLACK) {
                npc.getNavigation().stop();
                it.remove();
            } else if (npc.getNavigation().isIdle()) {
                it.remove();
            }
        }
    }

    private static NpcEntity find(MinecraftServer server, UUID id) {
        for (var world : server.getWorlds()) {
            if (world.getEntity(id) instanceof NpcEntity npc && !npc.isRemoved()) {
                return npc;
            }
        }
        return null;
    }
}
