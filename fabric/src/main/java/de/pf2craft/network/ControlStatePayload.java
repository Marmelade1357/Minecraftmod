package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Server → client: the NPC the GM now controls, or empty once released. */
public record ControlStatePayload(Optional<UUID> npcEntityUuid) implements CustomPayload {
    public static final CustomPayload.Id<ControlStatePayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("control_state"));
    public static final PacketCodec<RegistryByteBuf, ControlStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.optional(Uuids.PACKET_CODEC), ControlStatePayload::npcEntityUuid,
            ControlStatePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
