package de.pf2craft.command;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.pf2craft.GmRoles;
import de.pf2craft.character.CharacterService;
import de.pf2craft.character.CharacterStore;
import de.pf2craft.character.SheetService;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.item.CharacterItems;
import de.pf2craft.npc.MonsterDataLoader;
import de.pf2craft.npc.NpcEntity;
import de.pf2craft.npc.NpcService;
import de.pf2craft.roll.RollService;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * {@code /roll <dice>}, {@code /gmroll <dice>} (secret) and {@code /pf2 gm add|remove|list}.
 * The dice window is the main way to roll; the commands are a fallback.
 */
public final class Pf2Commands {
    private Pf2Commands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("roll")
                .then(argument("dice", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, false))));
        dispatcher.register(literal("gmroll")
                .then(argument("dice", StringArgumentType.greedyString())
                        .executes(ctx -> roll(ctx, true))));

        dispatcher.register(literal("pf2")
                .then(literal("gm")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(literal("add")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> setGm(ctx, true))))
                        .then(literal("remove")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> setGm(ctx, false))))
                        .then(literal("list")
                                .executes(Pf2Commands::listGms)))
                .then(literal("hp")
                        .requires(Pf2Commands::isGmOrOp)
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("amount", IntegerArgumentType.integer())
                                        .executes(Pf2Commands::changeHp))))
                .then(literal("items")
                        .requires(Pf2Commands::isGmOrOp)
                        .then(argument("player", EntityArgumentType.player())
                                .executes(Pf2Commands::reissueItems)))
                .then(literal("npc")
                        .requires(Pf2Commands::isGmOrOp)
                        .then(literal("spawn")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(Pf2Commands::spawnNpc)))
                        .then(literal("info")
                                .executes(Pf2Commands::npcInfo))
                        .then(literal("remove")
                                .executes(Pf2Commands::removeNpc)))
                .then(literal("sheet")
                        .requires(Pf2Commands::isGmOrOp)
                        .then(argument("player", EntityArgumentType.player())
                                .executes(Pf2Commands::openSheet)))
                .then(literal("character")
                        .then(literal("import")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(Pf2Commands::importCharacter)))
                        .then(literal("reset")
                                .requires(Pf2Commands::isGmOrOp)
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(Pf2Commands::resetCharacter)))));
    }

    private static boolean isGmOrOp(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        return source.hasPermissionLevel(2) || (player != null && GmRoles.isGm(player));
    }

    private static int spawnNpc(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity gm = ctx.getSource().getPlayerOrThrow();
        String id = StringArgumentType.getString(ctx, "id");
        Optional<MonsterData> data = MonsterDataLoader.catalog().byId(id);
        if (data.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.npc.unknown",
                    "Unknown monster: %s", id));
            return 0;
        }
        NpcEntity npc = NpcService.spawn(gm, data.get());
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.npc.spawned",
                "Spawned %s.", npc.stats().displayName()), true);
        return 1;
    }

    private static int npcInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Optional<NpcEntity> npc = lookedAtNpc(ctx);
        if (npc.isEmpty()) {
            return 0;
        }
        ctx.getSource().sendFeedback(() -> NpcService.info(npc.get()), false);
        return 1;
    }

    private static int removeNpc(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Optional<NpcEntity> npc = lookedAtNpc(ctx);
        if (npc.isEmpty()) {
            return 0;
        }
        String name = npc.get().stats().displayName();
        NpcService.remove(npc.get());
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.npc.removed",
                "Removed %s.", name), true);
        return 1;
    }

    private static Optional<NpcEntity> lookedAtNpc(CommandContext<ServerCommandSource> ctx)
            throws CommandSyntaxException {
        Optional<NpcEntity> npc = NpcService.lookedAt(ctx.getSource().getPlayerOrThrow());
        if (npc.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.npc.none_in_view",
                    "No NPC in view (within 16 blocks)."));
        }
        return npc;
    }

    private static int importCharacter(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        CharacterService.startImport(player, StringArgumentType.getString(ctx, "id"));
        return 1;
    }

    private static int changeHp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        OptionalInt hp = SheetService.changeHp(ctx.getSource().getServer(), target, amount);
        if (hp.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.character.none",
                    "%s has no character.", target.getDisplayName()));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.character.hp_changed",
                "%s now has %s HP.", target.getDisplayName(), hp.getAsInt()), true);
        return 1;
    }

    private static int reissueItems(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        Optional<CharacterSheet> sheet = CharacterStore.get(target);
        if (sheet.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.character.none",
                    "%s has no character.", target.getDisplayName()));
            return 0;
        }
        CharacterItems.replace(target, sheet.get());
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.items.reissued",
                "Re-issued the character items of %s.", target.getDisplayName()), true);
        return 1;
    }

    private static int openSheet(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity viewer = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        return SheetService.view(viewer, target.getUuid()) ? 1 : 0;
    }

    private static int resetCharacter(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        CharacterService.reset(target);
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.character.reset",
                "Character of %s was reset.", target.getDisplayName()), true);
        return 1;
    }

    private static int roll(CommandContext<ServerCommandSource> ctx, boolean secret) {
        ServerCommandSource source = ctx.getSource();
        try {
            RollService.roll(source.getServer(), source.getPlayer(), source.getName(),
                    StringArgumentType.getString(ctx, "dice"), "", secret);
            return 1;
        } catch (DiceParseException e) {
            source.sendError(Text.translatableWithFallback("pf2craft.roll.invalid",
                    "Invalid dice expression: %s", e.getMessage()));
            return 0;
        }
    }

    private static int setGm(CommandContext<ServerCommandSource> ctx, boolean gm) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
        GmRoles.setGm(player, gm);
        ctx.getSource().getServer().getPlayerManager().sendCommandTree(player);
        String key = gm ? "pf2craft.gm.added" : "pf2craft.gm.removed";
        String fallback = gm ? "%s is now a game master." : "%s is no longer a game master.";
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback(key, fallback, player.getDisplayName()), true);
        return 1;
    }

    private static int listGms(CommandContext<ServerCommandSource> ctx) {
        String names = ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                .filter(GmRoles::isGm)
                .map(player -> player.getName().getString())
                .collect(Collectors.joining(", "));
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.gm.list",
                "Game masters online: %s", names.isEmpty() ? "-" : names), false);
        return 1;
    }
}
