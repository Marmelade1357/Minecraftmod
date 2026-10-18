package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: the player whose turn it is ends it. */
public record EndTurnPayload() implements CustomPayload {
    public static final CustomPayload.Id<EndTurnPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("end_turn"));
    public static final PacketCodec<RegistryByteBuf, EndTurnPayload> CODEC = PacketCodec.unit(new EndTurnPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
