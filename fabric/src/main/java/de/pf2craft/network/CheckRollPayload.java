package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: "roll this check of my sheet". The server computes the bonus itself. */
public record CheckRollPayload(String checkId, boolean secret) implements CustomPayload {
    public static final CustomPayload.Id<CheckRollPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("check_roll"));
    public static final PacketCodec<RegistryByteBuf, CheckRollPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(32), CheckRollPayload::checkId,
            PacketCodecs.BOOL, CheckRollPayload::secret,
            CheckRollPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
