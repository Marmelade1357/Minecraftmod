package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client → server: the creature the player picked for their pending spell with a raycast (reaches further than
 * the vanilla click). The server handles it like a click and checks everything again.
 */
public record SpellTargetPayload(int entityId) implements CustomPayload {
    public static final CustomPayload.Id<SpellTargetPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("spell_target"));
    public static final PacketCodec<RegistryByteBuf, SpellTargetPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SpellTargetPayload::entityId, SpellTargetPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
