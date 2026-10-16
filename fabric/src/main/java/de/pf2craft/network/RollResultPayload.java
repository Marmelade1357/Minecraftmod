package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server → client: the outcome of a roll, shown in chat and on the HUD. */
public record RollResultPayload(String roller, String label, String expression, String breakdown, int total, int flags)
        implements CustomPayload {
    public static final int FLAG_SECRET = 1;
    public static final int FLAG_NATURAL_20 = 1 << 1;
    public static final int FLAG_NATURAL_1 = 1 << 2;

    public static final CustomPayload.Id<RollResultPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("roll_result"));
    public static final PacketCodec<RegistryByteBuf, RollResultPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, RollResultPayload::roller,
            PacketCodecs.STRING, RollResultPayload::label,
            PacketCodecs.STRING, RollResultPayload::expression,
            PacketCodecs.STRING, RollResultPayload::breakdown,
            PacketCodecs.VAR_INT, RollResultPayload::total,
            PacketCodecs.VAR_INT, RollResultPayload::flags,
            RollResultPayload::new);

    public boolean secret() {
        return (flags & FLAG_SECRET) != 0;
    }

    public boolean natural20() {
        return (flags & FLAG_NATURAL_20) != 0;
    }

    public boolean natural1() {
        return (flags & FLAG_NATURAL_1) != 0;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
