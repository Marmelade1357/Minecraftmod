package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Server → client: a character sheet as JSON. {@code open} asks the client to show the screen. */
public record SheetSyncPayload(UUID owner, String sheetJson, boolean gmView,
                               boolean classSupported, boolean open) implements CustomPayload {
    public static final CustomPayload.Id<SheetSyncPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("sheet_sync"));
    public static final PacketCodec<RegistryByteBuf, SheetSyncPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SheetSyncPayload::owner,
            PacketCodecs.string(262144), SheetSyncPayload::sheetJson,
            PacketCodecs.BOOL, SheetSyncPayload::gmView,
            PacketCodecs.BOOL, SheetSyncPayload::classSupported,
            PacketCodecs.BOOL, SheetSyncPayload::open,
            SheetSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
