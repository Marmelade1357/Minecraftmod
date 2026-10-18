package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: GM searches the monster catalog. */
public record NpcSearchPayload(String text) implements CustomPayload {
    public static final int MAX_TEXT = 32;
    public static final CustomPayload.Id<NpcSearchPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("npc_search"));
    public static final PacketCodec<RegistryByteBuf, NpcSearchPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(MAX_TEXT), NpcSearchPayload::text,
            NpcSearchPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
