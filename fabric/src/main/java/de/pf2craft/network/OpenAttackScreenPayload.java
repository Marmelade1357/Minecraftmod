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
 * Server → client: open the attack window for {@code attackerId} against the entity {@code targetEntityId}.
 * Each option is one strike with the attack bonuses of the 1st to 3rd attack (multiple attack penalty
 * included, range penalty not); {@code suggestedNumber} (1..3) is pre-selected.
 */
public record OpenAttackScreenPayload(UUID attackerId, int targetEntityId, List<StrikeOption> options,
                                      int suggestedNumber) implements CustomPayload {
    public static final int MAX_OPTIONS = 8;
    public static final int MAX_NAME = 64;
    public static final CustomPayload.Id<OpenAttackScreenPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("open_attack_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenAttackScreenPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, OpenAttackScreenPayload::attackerId,
            PacketCodecs.VAR_INT, OpenAttackScreenPayload::targetEntityId,
            StrikeOption.CODEC.collect(PacketCodecs.toList(MAX_OPTIONS)), OpenAttackScreenPayload::options,
            PacketCodecs.VAR_INT, OpenAttackScreenPayload::suggestedNumber,
            OpenAttackScreenPayload::new);

    public OpenAttackScreenPayload {
        options = List.copyOf(options);
    }

    public record StrikeOption(String name, List<Integer> bonuses, boolean agile) {
        public static final PacketCodec<RegistryByteBuf, StrikeOption> CODEC = PacketCodec.tuple(
                PacketCodecs.string(MAX_NAME), StrikeOption::name,
                PacketCodecs.VAR_INT.collect(PacketCodecs.toList(MAX_OPTIONS)), StrikeOption::bonuses,
                PacketCodecs.BOOL, StrikeOption::agile,
                StrikeOption::new);

        public StrikeOption {
            bonuses = List.copyOf(bonuses);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
