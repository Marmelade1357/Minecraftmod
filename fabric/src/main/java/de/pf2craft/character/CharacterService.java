package de.pf2craft.character;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.character.CharacterImportException;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderId;
import de.pf2craft.network.OpenImportPayload;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

/** Join handling and the Pathbuilder import flow. */
public final class CharacterService {
    private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private CharacterService() {
    }

    public static void onJoin(ServerPlayerEntity player) {
        if (GmRoles.isGm(player) || CharacterStore.get(player).isPresent()) {
            return;
        }
        player.changeGameMode(GameMode.SPECTATOR);
        player.sendMessage(Text.translatableWithFallback("pf2craft.character.need_import",
                "You need a character first: import it from Pathbuilder 2e.").formatted(Formatting.YELLOW), false);
        if (ServerPlayNetworking.canSend(player, OpenImportPayload.ID)) {
            ServerPlayNetworking.send(player, new OpenImportPayload());
        }
    }

    public static void startImport(ServerPlayerEntity player, String rawId) {
        Optional<String> id = PathbuilderId.parse(rawId);
        if (id.isEmpty()) {
            error(player, Text.translatableWithFallback("pf2craft.character.invalid_id",
                    "Invalid Pathbuilder id (digits only)."));
            return;
        }
        UUID uuid = player.getUuid();
        if (!IN_FLIGHT.add(uuid)) {
            error(player, Text.translatableWithFallback("pf2craft.character.import_busy",
                    "An import is already running."));
            return;
        }
        MinecraftServer server = player.getServer();
        PathbuilderFetcher.fetch(id.get()).whenComplete((sheet, failure) -> server.execute(() -> {
            IN_FLIGHT.remove(uuid);
            ServerPlayerEntity target = server.getPlayerManager().getPlayer(uuid);
            if (target == null) {
                return;
            }
            if (failure != null) {
                Throwable cause = failure.getCause() != null ? failure.getCause() : failure;
                String message = "Unexpected error while importing";
                if (cause instanceof CharacterImportException) {
                    message = cause.getMessage();
                }
                Pf2Craft.LOGGER.warn("Character import failed for {}", target.getName().getString(), cause);
                error(target, Text.translatableWithFallback("pf2craft.character.import_failed",
                        "Import failed: %s", message));
                return;
            }
            complete(server, target, sheet);
        }));
    }

    private static void complete(MinecraftServer server, ServerPlayerEntity player, CharacterSheet sheet) {
        boolean hadCharacter = CharacterStore.get(player).isPresent();
        CharacterStore.set(player, sheet);
        if (!hadCharacter && !GmRoles.isGm(player)) {
            player.changeGameMode(GameMode.SURVIVAL);
        }
        player.sendMessage(Text.translatableWithFallback("pf2craft.character.imported",
                "Character imported: %s (%s, level %s)", sheet.name(), sheet.className(), sheet.level())
                .formatted(Formatting.GREEN), false);
        SheetService.broadcast(server, player, sheet);
        Text gmNotice = Text.translatableWithFallback("pf2craft.character.imported_gm",
                "%s imported %s, level %s", player.getName(), sheet.name(), sheet.level());
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (GmRoles.isGm(other)) {
                other.sendMessage(gmNotice, false);
            }
        }
        Pf2Craft.LOGGER.info("{} imported character {} ({} {})", player.getName().getString(),
                sheet.name(), sheet.className(), sheet.level());
    }

    /** Removes the character and sends the player back to spectator mode. */
    public static void reset(ServerPlayerEntity target) {
        CharacterStore.remove(target);
        SheetService.forget(target.getUuid());
        if (!GmRoles.isGm(target)) {
            target.changeGameMode(GameMode.SPECTATOR);
        }
        if (ServerPlayNetworking.canSend(target, OpenImportPayload.ID)) {
            ServerPlayNetworking.send(target, new OpenImportPayload());
        }
        target.sendMessage(Text.translatableWithFallback("pf2craft.character.reset_notice",
                "Your character was reset. Import a new one.").formatted(Formatting.YELLOW), false);
    }

    private static void error(ServerPlayerEntity player, Text text) {
        player.sendMessage(text.copy().formatted(Formatting.RED), false);
    }
}
