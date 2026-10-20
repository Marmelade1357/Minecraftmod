package de.pf2craft.network;

import de.pf2craft.Pf2Craft;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Client → server, every client tick while the GM controls an NPC: the movement input and the look direction. */
public record ControlInputPayload(float forward, float sideways, boolean jump, boolean sprint, float yaw,
                                  float pitch) implements CustomPayload {
    public static final CustomPayload.Id<ControlInputPayload> ID =
            new CustomPayload.Id<>(Pf2Craft.id("control_input"));
    public static final PacketCodec<RegistryByteBuf, ControlInputPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, ControlInputPayload::forward,
            PacketCodecs.FLOAT, ControlInputPayload::sideways,
            PacketCodecs.BOOL, ControlInputPayload::jump,
            PacketCodecs.BOOL, ControlInputPayload::sprint,
            PacketCodecs.FLOAT, ControlInputPayload::yaw,
            PacketCodecs.FLOAT, ControlInputPayload::pitch,
            ControlInputPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
