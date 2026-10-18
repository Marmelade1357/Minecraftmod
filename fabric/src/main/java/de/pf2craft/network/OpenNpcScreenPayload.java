package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: "open this NPC screen" (currently only {@code spawn}). */
public record OpenNpcScreenPayload(String screen) implements CustomPayload {
    public static final CustomPayload.Id<OpenNpcScreenPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("open_npc_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenNpcScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(32), OpenNpcScreenPayload::screen,
            OpenNpcScreenPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
