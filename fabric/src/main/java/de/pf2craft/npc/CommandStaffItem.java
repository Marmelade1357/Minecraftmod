package de.pf2craft.npc;

import de.pf2craft.GmRoles;
import de.pf2craft.network.NpcSheetPayload;
import de.pf2craft.network.OpenNpcScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The GM's command staff. Right-click in the air opens the spawn screen, on an NPC selects it (with
 * sneaking: opens its sheet), on a block sends the selected NPC there. All decisions are made on the server.
 */
public class CommandStaffItem extends Item {
    private static final double WALK_SPEED = 0.6;

    public CommandStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity gm && requireGm(gm)) {
            ServerPlayNetworking.send(gm, new OpenNpcScreenPayload("spawn"));
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity gm && requireGm(gm)) {
            BlockPos target = context.getBlockPos().up();
            GmSelection.get(gm).ifPresentOrElse(npc -> {
                if (npc.getWorld() != gm.getWorld() || npc.stats().defeated()) {
                    gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.cannot_walk",
                            "The selected NPC is defeated or in another world.").formatted(Formatting.RED), true);
                    return;
                }
                npc.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                        WALK_SPEED);
            }, () -> gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.none_selected",
                    "No NPC selected. Right-click an NPC with the staff first.")
                    .formatted(Formatting.RED), true));
        }
        return ActionResult.success(context.getWorld().isClient);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof NpcEntity npc)) {
            return ActionResult.PASS;
        }
        if (user instanceof ServerPlayerEntity gm && requireGm(gm) && npc.stats() != null) {
            if (gm.isSneaking()) {
                NpcNetwork.sendSheet(gm, npc);
            } else {
                GmSelection.select(gm, npc);
                gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.selected", "Selected: %s",
                        npc.stats().displayName()), true);
            }
        }
        return ActionResult.success(user.getWorld().isClient);
    }

    private static boolean requireGm(ServerPlayerEntity player) {
        if (GmRoles.isGm(player)) {
            return true;
        }
        NpcNetwork.notGm(player);
        return false;
    }
}
