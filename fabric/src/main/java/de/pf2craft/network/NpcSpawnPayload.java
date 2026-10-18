package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: GM spawns the monster with this catalog id next to themselves. */
public record NpcSpawnPayload(String monsterId) implements CustomPayload {
    public static final CustomPayload.Id<NpcSpawnPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("npc_spawn"));
    public static final PacketCodec<RegistryByteBuf, NpcSpawnPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(NpcSearchResultPayload.MAX_ID), NpcSpawnPayload::monsterId,
            NpcSpawnPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
