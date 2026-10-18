package de.pf2craft.item;

import de.pf2craft.character.CharacterStore;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Strike;
import de.pf2craft.core.item.ItemStamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/** Hands out and takes back the items that belong to a player's character. */
public final class CharacterItems {
    private CharacterItems() {
    }

    /** Removes the player's old character items and gives the ones for {@code sheet}. */
    public static void replace(ServerPlayerEntity player, CharacterSheet sheet) {
        remove(player);
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : Pf2ItemFactory.create(player.getUuid(), sheet)) {
            Text name = stack.getName();
            // Creative mode reports success even when nothing fits, so check for room first.
            boolean hasRoom = inventory.getEmptySlot() != -1
                    || inventory.getOccupiedSlotWithRoomForStack(stack) != -1;
            if (!hasRoom || !inventory.insertStack(stack)) {
                ItemEntity dropped = player.dropItem(stack, false, false);
                if (dropped != null) {
                    // Lay it at the feet instead of throwing it forward like a Q-drop.
                    dropped.setPosition(player.getX(), player.getY(), player.getZ());
                    dropped.setVelocity(Vec3d.ZERO);
                }
                player.sendMessage(Text.translatableWithFallback("pf2craft.items.dropped",
                        "Your inventory is full: %s was dropped at your feet.", name)
                        .formatted(Formatting.YELLOW), false);
            }
        }
    }

    /** Removes every stack in main inventory, armor and off-hand that belongs to this player. */
    public static void remove(ServerPlayerEntity player) {
        UUID owner = player.getUuid();
        PlayerInventory inventory = player.getInventory();
        for (List<ItemStack> list : List.of(inventory.main, inventory.armor, inventory.offHand)) {
            for (int i = 0; i < list.size(); i++) {
                if (ownedBy(list.get(i), owner)) {
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }
        inventory.markDirty();
    }

    /** The strike behind a weapon item, if the holder owns it and it matches the current sheet. */
    public static Optional<Strike> strikeFor(ServerPlayerEntity holder, ItemStack stack) {
        WeaponRef ref = stack.get(Pf2Components.WEAPON);
        if (ref == null || !ref.owner().equals(holder.getUuid())) {
            return Optional.empty();
        }
        return CharacterStore.get(holder)
                .filter(sheet -> ItemStamp.of(sheet) == ref.sheetStamp())
                .filter(sheet -> ref.strikeIndex() >= 0 && ref.strikeIndex() < sheet.strikes().size())
                .map(sheet -> sheet.strikes().get(ref.strikeIndex()));
    }

    private static boolean ownedBy(ItemStack stack, UUID owner) {
        WeaponRef weapon = stack.get(Pf2Components.WEAPON);
        if (weapon != null && weapon.owner().equals(owner)) {
            return true;
        }
        ShieldRef shield = stack.get(Pf2Components.SHIELD);
        return shield != null && shield.owner().equals(owner);
    }
}
