package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: GM changes one NPC field (see {@code NpcEdit}). */
public record NpcEditPayload(int entityId, String field, String value) implements CustomPayload {
    public static final CustomPayload.Id<NpcEditPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("npc_edit"));
    public static final PacketCodec<RegistryByteBuf, NpcEditPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, NpcEditPayload::entityId,
            PacketCodecs.string(64), NpcEditPayload::field,
            PacketCodecs.string(32), NpcEditPayload::value,
            NpcEditPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
