package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/**
 * Client → server: the attack chosen in the attack window. Only a wish; the server re-derives the strike, the
 * bonus and the target and validates every field.
 */
public record AttackConfirmPayload(UUID attackerId, int targetEntityId, int strikeIndex, int attackNumber)
        implements CustomPayload {
    public static final CustomPayload.Id<AttackConfirmPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("attack_confirm"));
    public static final PacketCodec<RegistryByteBuf, AttackConfirmPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, AttackConfirmPayload::attackerId,
            PacketCodecs.VAR_INT, AttackConfirmPayload::targetEntityId,
            PacketCodecs.VAR_INT, AttackConfirmPayload::strikeIndex,
            PacketCodecs.VAR_INT, AttackConfirmPayload::attackNumber,
            AttackConfirmPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
