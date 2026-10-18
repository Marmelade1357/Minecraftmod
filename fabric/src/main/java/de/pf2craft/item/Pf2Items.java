package de.pf2craft.item;

import de.pf2craft.Pf2Craft;
import de.pf2craft.npc.CommandStaffItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Items of the mod itself (as opposed to the vanilla items stamped for characters). */
public final class Pf2Items {
    public static final Item COMMAND_STAFF = Registry.register(Registries.ITEM, Pf2Craft.id("command_staff"),
            new CommandStaffItem(new Item.Settings().maxCount(1)));

    private Pf2Items() {
    }

    /** Forces class loading, which registers the items. Call early in onInitialize. */
    public static void register() {
    }

    /**
     * Gives the GM a command staff unless they already carry one.
     *
     * @return {@code true} if a staff was handed out
     */
    public static boolean giveStaff(ServerPlayerEntity player) {
        if (player.getInventory().containsAny(s -> s.isOf(COMMAND_STAFF))) {
            return false;
        }
        ItemStack stack = new ItemStack(COMMAND_STAFF);
        if (!player.getInventory().insertStack(stack)) {
            ItemEntity dropped = player.dropItem(stack, false, false);
            if (dropped != null) {
                dropped.setPosition(player.getX(), player.getY(), player.getZ());
                dropped.setVelocity(Vec3d.ZERO);
            }
        }
        return true;
    }
}
