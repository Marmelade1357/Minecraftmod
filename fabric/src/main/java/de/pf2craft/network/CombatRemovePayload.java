package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Client to server: a GM takes one participant out of the running combat. */
public record CombatRemovePayload(UUID id) implements CustomPayload {
    public static final CustomPayload.Id<CombatRemovePayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("combat_remove"));
    public static final PacketCodec<RegistryByteBuf, CombatRemovePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, CombatRemovePayload::id,
            CombatRemovePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
