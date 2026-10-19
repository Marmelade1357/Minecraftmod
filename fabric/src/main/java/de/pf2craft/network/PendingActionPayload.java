package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: the action waiting for a target ("" = none), for the HUD hint. */
public record PendingActionPayload(String actionId) implements CustomPayload {
    public static final CustomPayload.Id<PendingActionPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("pending_action"));
    public static final PacketCodec<RegistryByteBuf, PendingActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(ActionRequestPayload.MAX_ID), PendingActionPayload::actionId,
            PendingActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
