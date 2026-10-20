package de.pf2craft.mixin;

import de.pf2craft.npc.NpcEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Players and NPCs never push each other (player-player, player-NPC, NPC-NPC). Runs on both sides so the
 * client prediction matches the server. Other mobs still push as in vanilla.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void pf2craft$noPush(Entity other, CallbackInfo ci) {
        if (pf2craft$isActor((Entity) (Object) this) && pf2craft$isActor(other)) {
            ci.cancel();
        }
    }

    private static boolean pf2craft$isActor(Entity entity) {
        return entity instanceof PlayerEntity || entity instanceof NpcEntity;
    }
}
