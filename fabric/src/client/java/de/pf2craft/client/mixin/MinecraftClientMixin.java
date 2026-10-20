package de.pf2craft.client.mixin;

import de.pf2craft.client.ClientControl;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While the GM controls an NPC, attack becomes the NPC's strike and block breaking / using items is off. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void pf2craft$npcAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ClientControl.isControlling()) {
            ClientControl.sendAttack();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void pf2craft$noBreaking(boolean breaking, CallbackInfo ci) {
        if (ClientControl.isControlling()) {
            ci.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void pf2craft$noUse(CallbackInfo ci) {
        if (ClientControl.isControlling()) {
            ci.cancel();
        }
    }
}
