package de.pf2craft.network;

import de.pf2craft.character.CharacterService;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.roll.RollService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Pf2Networking {
    private Pf2Networking() {
    }

    /** Runs on both sides, the payload types must be known to client and server. */
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(RollRequestPayload.ID, RollRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RollResultPayload.ID, RollResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ImportCharacterPayload.ID, ImportCharacterPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenImportPayload.ID, OpenImportPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ImportCharacterPayload.ID,
                (payload, context) -> CharacterService.startImport(context.player(), payload.pathbuilderId()));
        ServerPlayNetworking.registerGlobalReceiver(RollRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!RollService.checkCooldown(player)) {
                return;
            }
            try {
                RollService.roll(player.getServer(), player, player.getName().getString(),
                        payload.expression(), payload.label(), payload.secret());
            } catch (DiceParseException e) {
                player.sendMessage(Text.translatableWithFallback("pf2craft.roll.invalid",
                        "Invalid dice expression: %s", e.getMessage()).formatted(Formatting.RED), false);
            }
        });
    }
}
