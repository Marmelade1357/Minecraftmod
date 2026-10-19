package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Client to server: a GM sets a condition of a combatant to an exact value (0 removes it). */
public record ConditionEditPayload(UUID target, String conditionId, int value) implements CustomPayload {
    public static final CustomPayload.Id<ConditionEditPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("condition_edit"));
    public static final PacketCodec<RegistryByteBuf, ConditionEditPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ConditionEditPayload::target,
            PacketCodecs.string(32), ConditionEditPayload::conditionId,
            PacketCodecs.INTEGER, ConditionEditPayload::value,
            ConditionEditPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
