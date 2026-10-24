package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Server → client: a player's spellbook as JSON (to the owner and to GMs viewing the sheet). */
public record SpellbookSyncPayload(UUID owner, String json) implements CustomPayload {
    public static final CustomPayload.Id<SpellbookSyncPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("spellbook_sync"));
    public static final PacketCodec<RegistryByteBuf, SpellbookSyncPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, SpellbookSyncPayload::owner,
            PacketCodecs.string(32767), SpellbookSyncPayload::json,
            SpellbookSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
