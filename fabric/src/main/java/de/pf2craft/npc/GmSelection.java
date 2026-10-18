package de.pf2craft.npc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * The NPC each GM has selected with the command staff. Kept in server memory only (GM UUID to NPC UUID);
 * entries disappear on disconnect, and a selection whose NPC is gone resolves to nothing.
 */
public final class GmSelection {
    private static final Map<UUID, UUID> SELECTED = new ConcurrentHashMap<>();

    private GmSelection() {
    }

    public static void select(ServerPlayerEntity gm, NpcEntity npc) {
        SELECTED.put(gm.getUuid(), npc.getUuid());
    }

    /** The selected NPC if it still exists in a loaded world. */
    public static Optional<NpcEntity> get(ServerPlayerEntity gm) {
        UUID npcId = SELECTED.get(gm.getUuid());
        if (npcId == null) {
            return Optional.empty();
        }
        for (ServerWorld world : gm.getServer().getWorlds()) {
            Entity entity = world.getEntity(npcId);
            if (entity instanceof NpcEntity npc && !npc.isRemoved() && npc.stats() != null) {
                return Optional.of(npc);
            }
        }
        SELECTED.remove(gm.getUuid(), npcId);
        return Optional.empty();
    }

    public static void clear(ServerPlayerEntity gm) {
        SELECTED.remove(gm.getUuid());
    }

    /** Drops every selection of the given NPC, e.g. after it was removed. */
    public static void forget(UUID npcId) {
        SELECTED.values().removeIf(npcId::equals);
    }
}
