package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Client → server: "send me the sheet of this player". Others' sheets only for GMs. */
public record SheetRequestPayload(UUID target) implements CustomPayload {
    public static final CustomPayload.Id<SheetRequestPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("sheet_request"));
    public static final PacketCodec<RegistryByteBuf, SheetRequestPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SheetRequestPayload::target,
            SheetRequestPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
