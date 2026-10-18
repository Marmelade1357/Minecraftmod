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
 * Server → client: the state of the combat, built per recipient. Hidden values are {@code -1}
 * (NPC initiative and hit points for everyone but GMs). {@code label} is the ordinal of
 * {@code HealthLabel} for NPCs and {@link Entry#PLAYER_LABEL} for players.
 */
public record CombatSyncPayload(boolean active, int round, List<Entry> entries, int actionsLeft,
                                boolean reactionAvailable, int strideBlocksLeft) implements CustomPayload {
    public static final int MAX_ENTRIES = 64;
    public static final CustomPayload.Id<CombatSyncPayload> ID = new CustomPayload.Id<>(Pf2Craft.id("combat_sync"));
    public static final PacketCodec<RegistryByteBuf, CombatSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, CombatSyncPayload::active,
            PacketCodecs.INTEGER, CombatSyncPayload::round,
            Entry.CODEC.collect(PacketCodecs.toList(MAX_ENTRIES)), CombatSyncPayload::entries,
            PacketCodecs.INTEGER, CombatSyncPayload::actionsLeft,
            PacketCodecs.BOOL, CombatSyncPayload::reactionAvailable,
            PacketCodecs.INTEGER, CombatSyncPayload::strideBlocksLeft,
            CombatSyncPayload::new);

    public static CombatSyncPayload inactive() {
        return new CombatSyncPayload(false, 0, List.of(), 0, false, 0);
    }

    public record Entry(UUID id, String name, boolean npc, int initiative, byte label, int hp, int maxHp,
                        boolean active, boolean current) {
        public static final byte PLAYER_LABEL = (byte) 255;
        public static final PacketCodec<RegistryByteBuf, Entry> CODEC = PacketCodec.of(
                (entry, buf) -> {
                    Uuids.PACKET_CODEC.encode(buf, entry.id());
                    buf.writeString(entry.name(), 64);
                    buf.writeBoolean(entry.npc());
                    buf.writeInt(entry.initiative());
                    buf.writeByte(entry.label());
                    buf.writeInt(entry.hp());
                    buf.writeInt(entry.maxHp());
                    buf.writeBoolean(entry.active());
                    buf.writeBoolean(entry.current());
                },
                buf -> new Entry(Uuids.PACKET_CODEC.decode(buf), buf.readString(64), buf.readBoolean(),
                        buf.readInt(), buf.readByte(), buf.readInt(), buf.readInt(), buf.readBoolean(),
                        buf.readBoolean()));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
