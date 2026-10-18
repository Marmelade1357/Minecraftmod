package de.pf2craft.combat;

import de.pf2craft.GmRoles;
import de.pf2craft.network.OpenCombatScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** The GM's combat bell: right-click opens the combat screen (start, or skip/end while a combat runs). */
public class CombatItem extends Item {
    public CombatItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity gm) {
            if (!GmRoles.isGm(gm)) {
                CombatMessages.error(gm, CombatMessages.notGm());
            } else if (CombatService.active()) {
                ServerPlayNetworking.send(gm, new OpenCombatScreenPayload(true, CombatService.newCandidates(gm)));
            } else {
                ServerPlayNetworking.send(gm, new OpenCombatScreenPayload(false, CombatService.candidates(gm)));
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }
}
