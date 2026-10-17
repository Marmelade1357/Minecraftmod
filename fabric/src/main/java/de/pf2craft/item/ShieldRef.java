package de.pf2craft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Uuids;

/** Marks an item as the shield of the owner's sheet with that stamp. */
public record ShieldRef(UUID owner, long sheetStamp) {
    public static final Codec<ShieldRef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Uuids.CODEC.fieldOf("owner").forGetter(ShieldRef::owner),
            Codec.LONG.fieldOf("stamp").forGetter(ShieldRef::sheetStamp)
    ).apply(i, ShieldRef::new));

    public static final PacketCodec<ByteBuf, ShieldRef> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ShieldRef::owner,
            PacketCodecs.VAR_LONG, ShieldRef::sheetStamp,
            ShieldRef::new);
}
