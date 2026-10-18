package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: a GM controls the running combat ({@code skip} or {@code end}). */
public record CombatControlPayload(String action) implements CustomPayload {
    public static final CustomPayload.Id<CombatControlPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("combat_control"));
    public static final PacketCodec<RegistryByteBuf, CombatControlPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(16), CombatControlPayload::action,
            CombatControlPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
