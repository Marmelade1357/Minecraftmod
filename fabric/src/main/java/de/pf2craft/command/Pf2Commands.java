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
import de.pf2craft.combat.ActionService;
import de.pf2craft.combat.AttackService;
import de.pf2craft.combat.CombatService;
import de.pf2craft.combat.ConditionService;
import de.pf2craft.combat.Enforcement;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.combat.BasicActions;
import de.pf2craft.core.combat.Condition;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.NpcEdit;
import de.pf2craft.core.monster.NpcStats;
import de.pf2craft.core.progression.InvalidChoiceException;
import de.pf2craft.item.CharacterItems;
import de.pf2craft.item.Pf2Items;
import de.pf2craft.npc.GmSelection;
import de.pf2craft.npc.MonsterDataLoader;
import de.pf2craft.npc.NpcControl;
import de.pf2craft.npc.NpcEntity;
import de.pf2craft.npc.NpcService;
import de.pf2craft.npc.NpcSpeech;
import de.pf2craft.roll.RollService;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

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
                                .executes(Pf2Commands::removeNpc))
                        .then(literal("control")
                                .executes(ctx -> NpcControl.take(ctx.getSource().getPlayerOrThrow()) ? 1 : 0)
                                .then(literal("release")
                                        .executes(ctx -> {
                                            NpcControl.release(ctx.getSource().getPlayerOrThrow());
                                            return 1;
                                        })))
                        .then(literal("say")
                                .then(argument("text", StringArgumentType.greedyString())
                                        .executes(Pf2Commands::npcSay)))
                        .then(literal("hp")
                                .then(argument("value", IntegerArgumentType.integer())
                                        .executes(Pf2Commands::npcHp))))
                .then(literal("tools")
                        .requires(source -> source.getPlayer() != null && GmRoles.isGm(source.getPlayer()))
                        .executes(Pf2Commands::giveTools))
                .then(literal("combat")
                        .then(literal("start")
                                .requires(Pf2Commands::isGmOrOp)
                                .executes(Pf2Commands::combatStart))
                        .then(literal("status")
                                .requires(Pf2Commands::isGmOrOp)
                                .executes(Pf2Commands::combatStatus))
                        .then(literal("add")
                                .requires(Pf2Commands::isGmOrOp)
                                .executes(ctx -> {
                                    if (!CombatService.active()) {
                                        ctx.getSource().sendError(Text.translatableWithFallback(
                                                "pf2craft.combat.not_running", "No combat is running."));
                                        return 0;
                                    }
                                    int added = CombatService.addNearby(ctx.getSource().getPlayerOrThrow());
                                    ctx.getSource().sendFeedback(() -> Text.translatableWithFallback(
                                            "pf2craft.combat.added_count", "%s added to the combat.", added), false);
                                    return added;
                                }))
                        .then(literal("add")
                                .requires(Pf2Commands::isGmOrOp)
                                .then(argument("players", EntityArgumentType.players())
                                        .executes(ctx -> {
                                            int added = CombatService.addPlayers(ctx.getSource().getPlayerOrThrow(),
                                                    EntityArgumentType.getPlayers(ctx, "players"));
                                            ctx.getSource().sendFeedback(() -> Text.translatableWithFallback(
                                                    "pf2craft.combat.added_count", "%s added to the combat.", added),
                                                    false);
                                            return added;
                                        })))
                        .then(literal("remove")
                                .requires(Pf2Commands::isGmOrOp)
                                .executes(ctx -> {
                                    Optional<NpcEntity> npc = lookedAtNpc(ctx);
                                    if (npc.isEmpty()) {
                                        return 0;
                                    }
                                    return removeFromCombat(ctx, npc.get().getUuid());
                                })
                                .then(argument("target", EntityArgumentType.entity())
                                        .executes(ctx -> {
                                            return removeFromCombat(ctx,
                                                    EntityArgumentType.getEntity(ctx, "target").getUuid());
                                        })))
                        .then(literal("anchor")
                                .requires(Pf2Commands::isGmOrOp)
                                .then(argument("target", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                            return anchor(ctx, target);
                                        })
                                        .then(argument("pos", Vec3ArgumentType.vec3())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                                    Vec3d pos = Vec3ArgumentType.getVec3(ctx, "pos");
                                                    target.teleport(target.getServerWorld(), pos.x, pos.y, pos.z,
                                                            target.getYaw(), target.getPitch());
                                                    return anchor(ctx, target);
                                                }))))
                        .then(literal("next")
                                .requires(Pf2Commands::isGmOrOp)
                                .executes(ctx -> {
                                    CombatService.skip(ctx.getSource().getPlayerOrThrow());
                                    return 1;
                                }))
                        .then(literal("end")
                                .requires(Pf2Commands::isGmOrOp)
                                .executes(ctx -> {
                                    CombatService.end(ctx.getSource().getPlayerOrThrow());
                                    return 1;
                                }))
                        .then(literal("attack")
                                .then(argument("number", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> AttackService.attackLookedAt(
                                                ctx.getSource().getPlayerOrThrow(),
                                                IntegerArgumentType.getInteger(ctx, "number"), 0) ? 1 : 0)
                                        .then(argument("strike", IntegerArgumentType.integer(0, 7))
                                                .executes(ctx -> AttackService.attackLookedAt(
                                                        ctx.getSource().getPlayerOrThrow(),
                                                        IntegerArgumentType.getInteger(ctx, "number"),
                                                        IntegerArgumentType.getInteger(ctx, "strike")) ? 1 : 0))))
                        .then(literal("action")
                                .then(argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            BasicActions.ALL.forEach(a -> builder.suggest(a.id()));
                                            builder.suggest("cancel");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> ActionService.fallback(ctx.getSource().getPlayerOrThrow(),
                                                StringArgumentType.getString(ctx, "id")) ? 1 : 0)))
                        .then(literal("condition")
                                .requires(Pf2Commands::isGmOrOp)
                                .then(literal("player")
                                        .then(argument("player", EntityArgumentType.player())
                                                .then(argument("id", StringArgumentType.word())
                                                        .suggests(Pf2Commands::suggestConditions)
                                                        .then(argument("value", IntegerArgumentType.integer(0, 4))
                                                                .executes(ctx -> setCondition(ctx,
                                                                        EntityArgumentType.getPlayer(ctx, "player")
                                                                                .getUuid()))))))
                                .then(literal("npc")
                                        .then(argument("id", StringArgumentType.word())
                                                .suggests(Pf2Commands::suggestConditions)
                                                .then(argument("value", IntegerArgumentType.integer(0, 4))
                                                        .executes(ctx -> {
                                                            Optional<NpcEntity> npc = lookedAtNpc(ctx);
                                                            return npc.isEmpty() ? 0
                                                                    : setCondition(ctx, npc.get().getUuid());
                                                        })))))
                        .then(literal("endturn")
                                .executes(ctx -> {
                                    CombatService.endTurn(ctx.getSource().getPlayerOrThrow());
                                    return 1;
                                })))
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

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
            suggestConditions(CommandContext<ServerCommandSource> ctx,
                              com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (Condition c : Condition.values()) {
            builder.suggest(c.id());
        }
        return builder.buildFuture();
    }

    /** {@code /pf2 combat condition ...}: sets one condition of a combatant (value 0 removes it). */
    private static int setCondition(CommandContext<ServerCommandSource> ctx, java.util.UUID target) {
        Optional<Condition> condition = Condition.byId(StringArgumentType.getString(ctx, "id"));
        if (condition.isEmpty()) {
            ctx.getSource().sendError(ConditionService.unknownCondition());
            return 0;
        }
        int value = IntegerArgumentType.getInteger(ctx, "value");
        Optional<Text> error = ConditionService.editFromCommand(target, condition.get(), value);
        if (error.isPresent()) {
            ctx.getSource().sendError(error.get());
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.condition.set",
                "Condition set: %s", ConditionService.describe(condition.get(), value)), false);
        return 1;
    }

    /** Command path of "remove": game masters and level-2 operators alike, with feedback. */
    private static int removeFromCombat(CommandContext<ServerCommandSource> ctx, java.util.UUID id) {
        if (!CombatService.active()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.combat.not_running",
                    "No combat is running."));
            return 0;
        }
        if (!CombatService.removeFromCommand(id)) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.combat.not_participant",
                    "That creature does not take part in the combat."));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.combat.remove_done",
                "Removed from the combat."), false);
        return 1;
    }

    /** Holds the player where they now stand (after an optional move by the command itself). */
    private static int anchor(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        Enforcement.reanchor(target.getUuid());
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.combat.anchored",
                "%s is now held at the current position.", target.getName()), true);
        return 1;
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
        NpcService.SpawnResult result = NpcService.spawn(gm, data.get());
        if (result.npc() == null) {
            if (result.error() != null) {
                ctx.getSource().sendError(result.error());
            }
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.npc.spawned",
                "Spawned %s.", result.npc().stats().displayName()), false);
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

    private static int npcSay(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity gm = ctx.getSource().getPlayerOrThrow();
        Optional<NpcEntity> npc = GmSelection.get(gm);
        if (npc.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.npc.none_selected",
                    "No NPC selected. Right-click an NPC with the staff first."));
            return 0;
        }
        return NpcSpeech.say(gm, npc.get(), StringArgumentType.getString(ctx, "text")) ? 1 : 0;
    }

    private static int npcHp(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Optional<NpcEntity> npc = GmSelection.get(ctx.getSource().getPlayerOrThrow());
        if (npc.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.npc.none_selected",
                    "No NPC selected. Right-click an NPC with the staff first."));
            return 0;
        }
        try {
            NpcService.apply(npc.get(), NpcEdit.apply(npc.get().stats(), "hp",
                    String.valueOf(IntegerArgumentType.getInteger(ctx, "value"))));
        } catch (InvalidChoiceException e) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.npc.edit_failed",
                    "Invalid value: %s", e.getMessage()));
            return 0;
        }
        NpcStats stats = npc.get().stats();
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback("pf2craft.npc.hp_set",
                "%s now has %s HP.", stats.displayName(), stats.currentHp()), false);
        return 1;
    }

    private static int combatStart(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity executor = ctx.getSource().getPlayerOrThrow();
        CombatService.start(ctx.getSource().getServer(), executor,
                CombatService.nearbyIds(executor.getServerWorld(), executor.getPos()));
        return 1;
    }

    private static int combatStatus(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity executor = ctx.getSource().getPlayerOrThrow();
        java.util.List<Text> lines = CombatService.statusLines(GmRoles.isGm(executor));
        if (lines.isEmpty()) {
            ctx.getSource().sendError(Text.translatableWithFallback("pf2craft.combat.not_running",
                    "No combat is running."));
            return 0;
        }
        for (Text line : lines) {
            ctx.getSource().sendFeedback(() -> line, false);
        }
        return 1;
    }

    private static int giveTools(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Pf2Items.giveBell(ctx.getSource().getPlayerOrThrow());
        boolean given = Pf2Items.giveStaff(ctx.getSource().getPlayerOrThrow());
        ctx.getSource().sendFeedback(() -> Text.translatableWithFallback(
                given ? "pf2craft.npc.staff_given" : "pf2craft.npc.staff_present",
                given ? "You received the command staff." : "You already carry the command staff."), false);
        return 1;
    }

    private static Optional<NpcEntity> lookedAtNpc(CommandContext<ServerCommandSource> ctx)
            throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        Optional<NpcEntity> npc = NpcService.lookedAt(player);
        if (npc.isEmpty()) {
            npc = GmSelection.get(player);
        }
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
