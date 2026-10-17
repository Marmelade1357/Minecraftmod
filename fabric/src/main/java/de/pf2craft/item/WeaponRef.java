package de.pf2craft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

/** Marks an item as the weapon for strike {@code strikeIndex} of the owner's sheet with that stamp. */
public record WeaponRef(UUID owner, int strikeIndex, long sheetStamp) {
    public static final Codec<WeaponRef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Uuids.CODEC.fieldOf("owner").forGetter(WeaponRef::owner),
            Codec.INT.fieldOf("strike").forGetter(WeaponRef::strikeIndex),
            Codec.LONG.fieldOf("stamp").forGetter(WeaponRef::sheetStamp)
    ).apply(i, WeaponRef::new));

    public static final PacketCodec<ByteBuf, WeaponRef> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, WeaponRef::owner,
            PacketCodecs.VAR_INT, WeaponRef::strikeIndex,
            PacketCodecs.VAR_LONG, WeaponRef::sheetStamp,
            WeaponRef::new);
}
