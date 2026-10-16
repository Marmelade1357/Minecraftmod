package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Client → server: GM allows a player to level up. */
public record GrantLevelUpPayload(UUID target) implements CustomPayload {
    public static final CustomPayload.Id<GrantLevelUpPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("grant_level_up"));
    public static final PacketCodec<RegistryByteBuf, GrantLevelUpPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, GrantLevelUpPayload::target,
            GrantLevelUpPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
