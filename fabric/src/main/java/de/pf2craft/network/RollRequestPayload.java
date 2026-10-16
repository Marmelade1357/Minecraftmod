package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: "please roll this for me". The server parses and rolls, never the client. */
public record RollRequestPayload(String expression, String label, boolean secret) implements CustomPayload {
    public static final CustomPayload.Id<RollRequestPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("roll_request"));
    public static final PacketCodec<RegistryByteBuf, RollRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(256), RollRequestPayload::expression,
            PacketCodecs.string(64), RollRequestPayload::label,
            PacketCodecs.BOOL, RollRequestPayload::secret,
            RollRequestPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
