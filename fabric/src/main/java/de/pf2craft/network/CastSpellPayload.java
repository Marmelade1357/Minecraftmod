package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client → server: the wish to cast a spell from one of the sender's own spellcasting entries at a rank.
 * The server re-checks the entry, the spell, the rank, the slots, the turn and the actions; targets are chosen
 * afterwards by clicking (or with {@code ActionRequestPayload.CONFIRM} for multi-target spells).
 */
public record CastSpellPayload(int entry, String spell, int rank) implements CustomPayload {
    public static final int MAX_SPELL = 64;

    public static final CustomPayload.Id<CastSpellPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("cast_spell"));
    public static final PacketCodec<RegistryByteBuf, CastSpellPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CastSpellPayload::entry,
            PacketCodecs.string(MAX_SPELL), CastSpellPayload::spell,
            PacketCodecs.VAR_INT, CastSpellPayload::rank,
            CastSpellPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
