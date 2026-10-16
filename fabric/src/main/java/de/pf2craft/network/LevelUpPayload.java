package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: the player's level-up choices. Empty skillIncrease means none. */
public record LevelUpPayload(String skillIncrease, List<String> boosts, List<String> featNames) implements CustomPayload {
    public static final CustomPayload.Id<LevelUpPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("level_up"));
    public static final PacketCodec<RegistryByteBuf, LevelUpPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(64), LevelUpPayload::skillIncrease,
            PacketCodecs.string(64).collect(PacketCodecs.toList(8)), LevelUpPayload::boosts,
            PacketCodecs.string(64).collect(PacketCodecs.toList(8)), LevelUpPayload::featNames,
            LevelUpPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
