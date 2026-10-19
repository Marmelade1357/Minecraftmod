package de.pf2craft;

import de.pf2craft.item.Pf2Items;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Long interaction range for GMs while they hold the command staff or the combat bell, so they can fly
 * over a fight. Implemented with temporary (unsaved) attribute modifiers; vanilla's own server-side
 * reach check then accepts the far clicks. Independent of the combat reach in CombatService.
 */
public final class GmReach {
    public static final double BONUS = 96.0;
    /** Minimum look distance of the server-side raycasts (commands, fallback attack). */
    public static final double MIN_LOOK = 16.0;
    /** NPC/candidate lookups for a GM (screens, combat window) reach at least this far. */
    public static final double GM_SCAN = 128.0;
    private static final Identifier ID = Pf2Craft.id("gm_far_reach");
    private static final int INTERVAL = 4;

    private GmReach() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GmReach::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            update(p);
        }
    }

    private static boolean isGmItem(ItemStack s) {
        return s.isOf(Pf2Items.COMMAND_STAFF) || s.isOf(Pf2Items.COMBAT_BELL);
    }

    /** Adds or removes the far-reach modifiers depending on role and held item. */
    public static void update(ServerPlayerEntity player) {
        boolean want = GmRoles.isGm(player)
                && (isGmItem(player.getMainHandStack()) || isGmItem(player.getOffHandStack()));
        set(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, want);
        set(player, EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, want);
    }

    public static void clear(ServerPlayerEntity player) {
        set(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, false);
        set(player, EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, false);
    }

    private static void set(ServerPlayerEntity player, RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attr,
                            boolean want) {
        EntityAttributeInstance inst = player.getAttributeInstance(attr);
        if (inst == null) {
            return;
        }
        boolean has = inst.getModifier(ID) != null;
        if (want && !has) {
            inst.addTemporaryModifier(new EntityAttributeModifier(ID, BONUS,
                    EntityAttributeModifier.Operation.ADD_VALUE));
        } else if (!want && has) {
            inst.removeModifier(ID);
        }
    }

    /** How far a GM's raycasts should look: the current entity reach, at least {@link #MIN_LOOK}. */
    public static double lookRange(ServerPlayerEntity player) {
        if (!GmRoles.isGm(player)) {
            return MIN_LOOK;
        }
        return Math.max(MIN_LOOK, player.getAttributeValue(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE));
    }
}
