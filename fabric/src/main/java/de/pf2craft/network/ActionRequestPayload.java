package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/**
 * Client → server: a basic action chosen in the action menu. Only a wish; the server validates the actor,
 * the turn, the cost and the target. {@code targetEntityId} is -1 for "no target yet" (the action becomes
 * pending). The id {@link #CANCEL} clears the sender's pending action (or spell);
 * {@link #CONFIRM} resolves the sender's pending multi-target spell.
 */
public record ActionRequestPayload(UUID actorId, String actionId, int targetEntityId) implements CustomPayload {
    public static final String CANCEL = "cancel";
    /** Confirms a pending multi-target spell with the targets chosen so far. */
    public static final String CONFIRM = "confirm";
    public static final int MAX_ID = 24;

    public static final CustomPayload.Id<ActionRequestPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("action_request"));
    public static final PacketCodec<RegistryByteBuf, ActionRequestPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ActionRequestPayload::actorId,
            PacketCodecs.string(MAX_ID), ActionRequestPayload::actionId,
            PacketCodecs.VAR_INT, ActionRequestPayload::targetEntityId,
            ActionRequestPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
