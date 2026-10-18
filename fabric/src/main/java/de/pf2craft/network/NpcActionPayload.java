package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: GM runs an action on an NPC ({@code save_template} or {@code remove}). */
public record NpcActionPayload(int entityId, String action) implements CustomPayload {
    public static final CustomPayload.Id<NpcActionPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("npc_action"));
    public static final PacketCodec<RegistryByteBuf, NpcActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, NpcActionPayload::entityId,
            PacketCodecs.string(32), NpcActionPayload::action,
            NpcActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
