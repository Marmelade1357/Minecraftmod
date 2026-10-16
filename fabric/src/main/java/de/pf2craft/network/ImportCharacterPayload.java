package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: "import my character from this Pathbuilder id". The server validates the id. */
public record ImportCharacterPayload(String pathbuilderId) implements CustomPayload {
    public static final CustomPayload.Id<ImportCharacterPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("import_character"));
    public static final PacketCodec<RegistryByteBuf, ImportCharacterPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(32), ImportCharacterPayload::pathbuilderId,
            ImportCharacterPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
