package de.pf2craft;

import de.pf2craft.item.Pf2Items;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Game master role. Stored as an entity command tag, so it is saved with the player data
 * and can also be managed with vanilla {@code /tag}.
 */
public final class GmRoles {
    public static final String TAG = "pf2craft_gm";

    private GmRoles() {
    }

    public static boolean isGm(ServerPlayerEntity player) {
        return player.getCommandTags().contains(TAG);
    }

    /** @return {@code true} if the role changed */
    public static boolean setGm(ServerPlayerEntity player, boolean gm) {
        if (!gm) {
            return player.removeCommandTag(TAG);
        }
        boolean changed = player.addCommandTag(TAG);
        Pf2Items.giveStaff(player);
        Pf2Items.giveBell(player);
        return changed;
    }
}
