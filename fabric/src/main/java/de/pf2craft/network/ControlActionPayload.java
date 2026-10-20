package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server: an action of the controlled NPC; {@code attack} strikes whatever the NPC is looking at. */
public record ControlActionPayload(String kind) implements CustomPayload {
    public static final CustomPayload.Id<ControlActionPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("control_action"));
    public static final PacketCodec<RegistryByteBuf, ControlActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.string(16), ControlActionPayload::kind,
            ControlActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
