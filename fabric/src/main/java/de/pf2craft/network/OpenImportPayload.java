package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: "open the character import screen". */
public record OpenImportPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenImportPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("open_import"));
    public static final PacketCodec<RegistryByteBuf, OpenImportPayload> CODEC =
            PacketCodec.unit(new OpenImportPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
