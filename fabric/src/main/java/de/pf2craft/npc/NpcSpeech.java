package de.pf2craft.npc;

import de.pf2craft.GmRoles;
import de.pf2craft.combat.CombatMessages;
import de.pf2craft.core.dialogue.SpeechText;
import de.pf2craft.network.NpcSpeechPayload;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** A GM lets an NPC speak: a chat line for everybody and a speech bubble for players nearby. */
public final class NpcSpeech {
    public static final double BUBBLE_RANGE = 64.0;

    private NpcSpeech() {
    }

    public static void register() {
        // While a GM controls an NPC, their normal chat is the NPC's speech. Commands are not chat.
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!NpcControl.controlling(sender.getUuid())) {
                return true;
            }
            NpcControl.controlled(sender.getServer(), sender.getUuid())
                    .ifPresent(npc -> say(sender, npc, message.getSignedContent()));
            return false;
        });
    }

    public static boolean say(ServerPlayerEntity gm, NpcEntity npc, String raw) {
        if (!GmRoles.isGm(gm)) {
            CombatMessages.error(gm, CombatMessages.notGm());
            return false;
        }
        if (npc == null || npc.stats() == null) {
            return false;
        }
        String text = SpeechText.sanitize(raw);
        if (text.isEmpty()) {
            gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.say_empty", "There is nothing to say."), false);
            return false;
        }
        String name = npc.stats().displayName();
        gm.getServer().getPlayerManager().broadcast(
                Text.translatableWithFallback("pf2craft.npc.say", "%s: %s", name, text), false);
        NpcSpeechPayload payload = new NpcSpeechPayload(npc.getUuid(), text);
        for (ServerPlayerEntity p : gm.getServer().getPlayerManager().getPlayerList()) {
            if (p.getWorld() == npc.getWorld() && p.squaredDistanceTo(npc) <= BUBBLE_RANGE * BUBBLE_RANGE
                    && ServerPlayNetworking.canSend(p, NpcSpeechPayload.ID)) {
                ServerPlayNetworking.send(p, payload);
            }
        }
        return true;
    }
}
