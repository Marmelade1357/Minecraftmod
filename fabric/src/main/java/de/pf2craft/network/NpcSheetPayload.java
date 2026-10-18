package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: the stat block of an NPC ({@code NpcJson.write}) for the GM's sheet screen. */
public record NpcSheetPayload(int entityId, String json) implements CustomPayload {
    public static final CustomPayload.Id<NpcSheetPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("npc_sheet"));
    public static final PacketCodec<RegistryByteBuf, NpcSheetPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, NpcSheetPayload::entityId,
            PacketCodecs.STRING, NpcSheetPayload::json,
            NpcSheetPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
