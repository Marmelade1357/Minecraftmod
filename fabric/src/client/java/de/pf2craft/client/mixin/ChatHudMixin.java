package de.pf2craft.client.mixin;

import de.pf2craft.client.ClientActions;
import de.pf2craft.client.ClientCombat;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the chat smaller while a combat runs so the combat HUD and the world stay visible: the text scale and
 * the height of the open chat are multiplied by the factors from the client config. The width and the saved
 * options stay untouched; the vanilla getters are only adjusted on the way out.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow
    public abstract boolean isChatFocused();

    @Inject(method = "getChatScale", at = @At("RETURN"), cancellable = true)
    private void pf2craft$combatScale(CallbackInfoReturnable<Double> cir) {
        if (ClientCombat.get().isPresent()) {
            cir.setReturnValue(cir.getReturnValueD() * ClientActions.combatChatScale());
        }
    }

    @Inject(method = "getHeight()I", at = @At("RETURN"), cancellable = true)
    private void pf2craft$combatHeight(CallbackInfoReturnable<Integer> cir) {
        if (ClientCombat.get().isPresent() && isChatFocused()) {
            cir.setReturnValue(Math.max(20, (int) (cir.getReturnValueI() * ClientActions.combatChatHeight())));
        }
    }
}
