package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: the GM takes over the selected NPC ({@code take = true}) or lets go of it. */
public record TakeOverPayload(boolean take) implements CustomPayload {
    public static final CustomPayload.Id<TakeOverPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("take_over"));
    public static final PacketCodec<RegistryByteBuf, TakeOverPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, TakeOverPayload::take,
            TakeOverPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
