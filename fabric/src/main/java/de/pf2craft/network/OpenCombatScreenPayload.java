package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

/**
 * Server → client (GM): open the combat screen. With {@code running} the screen offers skip/end instead
 * of the participant list.
 */
public record OpenCombatScreenPayload(boolean running, List<Candidate> candidates) implements CustomPayload {
    public static final int MAX_CANDIDATES = 64;
    public static final int MAX_NAME = 64;
    public static final CustomPayload.Id<OpenCombatScreenPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("open_combat_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenCombatScreenPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, OpenCombatScreenPayload::running,
            Candidate.CODEC.collect(PacketCodecs.toList(MAX_CANDIDATES)), OpenCombatScreenPayload::candidates,
            OpenCombatScreenPayload::new);

    public record Candidate(UUID id, String name, boolean npc) {
        public static final PacketCodec<RegistryByteBuf, Candidate> CODEC = PacketCodec.tuple(
                Uuids.PACKET_CODEC, Candidate::id,
                PacketCodecs.string(MAX_NAME), Candidate::name,
                PacketCodecs.BOOL, Candidate::npc,
                Candidate::new);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
