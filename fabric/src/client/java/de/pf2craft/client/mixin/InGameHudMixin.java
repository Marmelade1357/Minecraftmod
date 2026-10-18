package de.pf2craft.client.mixin;

import de.pf2craft.client.CombatHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla hearts and hunger bar; {@link CombatHud} draws "TP: x/y" instead. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
    private void pf2craft$hideHearts(DrawContext context, PlayerEntity player, int x, int y, int lines,
                                     int regeneratingHeartIndex, float maxHealth, int lastHealth, int health,
                                     int absorption, boolean blinking, CallbackInfo ci) {
        if (CombatHud.replacesVanillaHealth()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void pf2craft$hideFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        if (CombatHud.replacesVanillaHealth()) {
            ci.cancel();
        }
    }
}
