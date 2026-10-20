package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Server → client: an NPC says something, shown as a speech bubble over its head. */
public record NpcSpeechPayload(UUID npcEntityUuid, String text) implements CustomPayload {
    public static final int MAX_TEXT = 120;
    public static final CustomPayload.Id<NpcSpeechPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("npc_speech"));
    public static final PacketCodec<RegistryByteBuf, NpcSpeechPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, NpcSpeechPayload::npcEntityUuid,
            PacketCodecs.string(MAX_TEXT), NpcSpeechPayload::text,
            NpcSpeechPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
