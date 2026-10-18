package de.pf2craft.npc;

import de.pf2craft.core.monster.DamagePart;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.MonsterStrike;
import de.pf2craft.core.monster.NpcStats;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Server-side operations on NPCs: spawn, change stats, remove, describe, find the one a player looks at. */
public final class NpcService {
    public static final double LOOK_RANGE = 16.0;
    public static final int MAX_NPCS_PER_WORLD = 200;
    private static final long SPAWN_COOLDOWN_MS = 250;
    private static final Map<UUID, Long> LAST_SPAWN = new ConcurrentHashMap<>();

    private NpcService() {
    }

    /**
     * Outcome of {@link #spawn}: the NPC, or {@code error} explaining why not. Both are {@code null} when the
     * request was dropped by the spawn cooldown.
     */
    public record SpawnResult(NpcEntity npc, Text error) {
    }

    /** Spawns a fresh NPC one block in front of the GM, facing them. */
    public static SpawnResult spawn(ServerPlayerEntity gm, MonsterData data) {
        long now = System.currentTimeMillis();
        Long last = LAST_SPAWN.put(gm.getUuid(), now);
        if (last != null && now - last < SPAWN_COOLDOWN_MS) {
            return new SpawnResult(null, null);
        }
        ServerWorld world = gm.getServerWorld();
        if (world.getEntitiesByType(Pf2Entities.NPC, e -> true).size() >= MAX_NPCS_PER_WORLD) {
            return new SpawnResult(null, Text.translatableWithFallback("pf2craft.npc.limit",
                    "This world already has the maximum of %s NPCs.", MAX_NPCS_PER_WORLD));
        }
        Vec3d look = Vec3d.fromPolar(0, gm.getYaw()).normalize();
        Vec3d pos = gm.getPos().add(look);
        NpcEntity npc = new NpcEntity(Pf2Entities.NPC, world);
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, gm.getYaw() + 180.0f, 0.0f);
        npc.setHeadYaw(npc.getYaw());
        npc.setBodyYaw(npc.getYaw());
        npc.setStats(NpcStats.fresh(data));
        if (!world.spawnEntity(npc)) {
            return new SpawnResult(null, Text.translatableWithFallback("pf2craft.npc.spawn_failed",
                    "The NPC could not be spawned here."));
        }
        return new SpawnResult(npc, null);
    }

    public static void forgetPlayer(UUID player) {
        LAST_SPAWN.remove(player);
    }

    public static void apply(NpcEntity npc, NpcStats stats) {
        npc.setStats(stats);
    }

    public static void remove(NpcEntity npc) {
        GmSelection.forget(npc.getUuid());
        npc.discard();
    }

    /** The NPC the player is looking at within {@link #LOOK_RANGE} blocks, if any. */
    public static Optional<NpcEntity> lookedAt(ServerPlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(LOOK_RANGE));
        Box box = player.getBoundingBox().stretch(dir.multiply(LOOK_RANGE)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(player, start, end, box,
                e -> e instanceof NpcEntity && !e.isSpectator(), LOOK_RANGE * LOOK_RANGE);
        if (hit != null && hit.getEntity() instanceof NpcEntity npc && npc.stats() != null) {
            return Optional.of(npc);
        }
        return Optional.empty();
    }

    /** GM-only description of an NPC. */
    public static Text info(NpcEntity npc) {
        NpcStats s = npc.stats();
        MonsterData m = s.base();
        MutableText out = Text.translatableWithFallback("pf2craft.npc.info.header",
                "%s (%s, level %s)", s.displayName(), m.name(), m.level());
        out.append("\n").append(Text.translatableWithFallback("pf2craft.npc.info.defense",
                "HP %s/%s, AC %s, Fort %s, Ref %s, Will %s, Perception %s, Speed %s ft.",
                s.currentHp(), m.hp(), m.ac(), signed(m.fortitude()), signed(m.reflex()), signed(m.will()),
                signed(m.perception()), m.speed()));
        for (MonsterStrike strike : m.strikes()) {
            String damage = strike.damage().stream().map(NpcService::damage).collect(Collectors.joining(" + "));
            out.append("\n").append(Text.translatableWithFallback("pf2craft.npc.info.strike",
                    "%s %s: %s", strike.name(), signed(strike.attackBonus()), damage));
        }
        return out;
    }

    private static String damage(DamagePart d) {
        String bonus = d.bonus() == 0 ? "" : (d.bonus() > 0 ? "+" : "") + d.bonus();
        String amount = d.dice().isEmpty() ? String.valueOf(d.bonus()) : d.dice() + bonus;
        return amount + " " + d.type();
    }

    private static String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }
}
