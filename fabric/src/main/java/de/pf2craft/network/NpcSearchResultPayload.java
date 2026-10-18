package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: monsters matching the last search (at most {@link #MAX_RESULTS}). */
public record NpcSearchResultPayload(List<Entry> entries) implements CustomPayload {
    public static final int MAX_RESULTS = 50;
    public static final int MAX_ID = 64;
    public static final int MAX_NAME = 64;
    public static final CustomPayload.Id<NpcSearchResultPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("npc_search_result"));
    public static final PacketCodec<RegistryByteBuf, NpcSearchResultPayload> CODEC = PacketCodec.tuple(
            Entry.CODEC.collect(PacketCodecs.toList(MAX_RESULTS)), NpcSearchResultPayload::entries,
            NpcSearchResultPayload::new);

    public record Entry(String id, String name, int level) {
        public static final PacketCodec<RegistryByteBuf, Entry> CODEC = PacketCodec.tuple(
                PacketCodecs.string(MAX_ID), Entry::id,
                PacketCodecs.string(MAX_NAME), Entry::name,
                PacketCodecs.INTEGER, Entry::level,
                Entry::new);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
