package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/** Client → server: the GM starts a combat with these participants (players and NPCs by UUID). */
public record CombatStartPayload(List<UUID> participants) implements CustomPayload {
    public static final int MAX_PARTICIPANTS = 64;
    public static final CustomPayload.Id<CombatStartPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("combat_start"));
    public static final PacketCodec<RegistryByteBuf, CombatStartPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC.collect(PacketCodecs.toList(MAX_PARTICIPANTS)), CombatStartPayload::participants,
            CombatStartPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
