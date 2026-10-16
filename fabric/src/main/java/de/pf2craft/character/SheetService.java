package de.pf2craft.character;

import de.pf2craft.GmRoles;
import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.CharacterJson;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.CheckId;
import de.pf2craft.core.character.GmEdit;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.progression.InvalidChoiceException;
import de.pf2craft.core.progression.LevelUp;
import de.pf2craft.core.progression.LevelUpChoices;
import de.pf2craft.core.progression.UnsupportedClassException;
import de.pf2craft.network.SheetSyncPayload;
import de.pf2craft.roll.RollService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Sheet synchronisation, click-to-roll, GM edits and level-ups. All calls run on the server thread. */
public final class SheetService {
    /** Owner uuid to the GMs who looked at that sheet in this session and get live updates. */
    private static final Map<UUID, Set<UUID>> VIEWERS = new HashMap<>();

    private SheetService() {
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        VIEWERS.remove(id);
        VIEWERS.values().forEach(set -> set.remove(id));
    }

    // --- sync -------------------------------------------------------------------------------

    /** Handles a sheet request: the own sheet always, other sheets only for GMs. */
    public static void request(ServerPlayerEntity requester, UUID target) {
        if (target.equals(requester.getUuid())) {
            Optional<CharacterSheet> sheet = CharacterStore.get(requester);
            sheet.ifPresent(value -> send(requester, requester, value, true));
            return;
        }
        if (GmRoles.isGm(requester)) {
            view(requester, target);
        }
    }

    /** Opens another player's sheet for {@code viewer}; the caller checks the permission. */
    public static boolean view(ServerPlayerEntity viewer, UUID target) {
        ServerPlayerEntity owner = viewer.getServer().getPlayerManager().getPlayer(target);
        if (owner == null) {
            offline(viewer);
            return false;
        }
        Optional<CharacterSheet> sheet = CharacterStore.get(owner);
        if (sheet.isEmpty()) {
            error(viewer, Text.translatableWithFallback("pf2craft.character.none",
                    "%s has no character.", owner.getName()));
            return false;
        }
        if (viewer != owner) {
            VIEWERS.computeIfAbsent(target, k -> new HashSet<>()).add(viewer.getUuid());
        }
        send(viewer, owner, sheet.get(), true);
        return true;
    }

    /** Re-sends the sheet (open = false) to its owner and to every GM watching it. */
    public static void broadcast(MinecraftServer server, ServerPlayerEntity owner, CharacterSheet sheet) {
        send(owner, owner, sheet, false);
        Set<UUID> watchers = VIEWERS.get(owner.getUuid());
        if (watchers == null) {
            return;
        }
        for (UUID id : new ArrayList<>(watchers)) {
            ServerPlayerEntity gm = server.getPlayerManager().getPlayer(id);
            if (gm == null || !GmRoles.isGm(gm)) {
                watchers.remove(id);
            } else {
                send(gm, owner, sheet, false);
            }
        }
    }

    private static void send(ServerPlayerEntity recipient, ServerPlayerEntity owner, CharacterSheet sheet,
                             boolean open) {
        if (!ServerPlayNetworking.canSend(recipient, SheetSyncPayload.ID)) {
            return;
        }
        boolean supported = sheet.level() < LevelUp.MAX_LEVEL
                && ClassData.registry().find(sheet.className()).isPresent();
        boolean gmView = recipient != owner || GmRoles.isGm(recipient);
        ServerPlayNetworking.send(recipient, new SheetSyncPayload(owner.getUuid(), CharacterJson.write(sheet),
                gmView, supported, open));
    }

    // --- rolling ----------------------------------------------------------------------------

    public static void rollCheck(ServerPlayerEntity player, String checkId, boolean secret) {
        Optional<CheckId> id = CheckId.parse(checkId);
        Optional<CharacterSheet> sheet = CharacterStore.get(player);
        if (id.isEmpty() || sheet.isEmpty()) {
            return;
        }
        OptionalInt bonus = CharacterMath.checkBonus(sheet.get(), id.get());
        if (bonus.isEmpty() || !RollService.checkCooldown(player)) {
            return;
        }
        String expression = "1d20" + (bonus.getAsInt() >= 0 ? "+" : "") + bonus.getAsInt();
        try {
            RollService.roll(player.getServer(), player, player.getName().getString(), expression,
                    id.get().label(sheet.get()), secret);
        } catch (DiceParseException e) {
            error(player, Text.literal(e.getMessage()));
        }
    }

    // --- GM actions -------------------------------------------------------------------------

    public static void gmEdit(ServerPlayerEntity gm, UUID target, String field, int value) {
        if (!GmRoles.isGm(gm)) {
            return;
        }
        ServerPlayerEntity owner = gm.getServer().getPlayerManager().getPlayer(target);
        if (owner == null) {
            offline(gm);
            return;
        }
        Optional<CharacterSheet> sheet = CharacterStore.get(owner);
        if (sheet.isEmpty()) {
            return;
        }
        try {
            CharacterSheet edited = GmEdit.apply(sheet.get(), field, value);
            CharacterStore.set(owner, edited);
            broadcast(gm.getServer(), owner, edited);
        } catch (InvalidChoiceException e) {
            error(gm, Text.literal(e.getMessage()));
        }
    }

    public static void grantLevelUp(ServerPlayerEntity gm, UUID target) {
        if (!GmRoles.isGm(gm)) {
            return;
        }
        ServerPlayerEntity owner = gm.getServer().getPlayerManager().getPlayer(target);
        if (owner == null) {
            offline(gm);
            return;
        }
        Optional<CharacterSheet> sheet = CharacterStore.get(owner);
        if (sheet.isEmpty()) {
            return;
        }
        CharacterSheet granted = sheet.get().withLevelUpGranted(true);
        CharacterStore.set(owner, granted);
        owner.sendMessage(Text.translatableWithFallback("pf2craft.character.levelup_granted",
                "The GM allows you to level up!").formatted(Formatting.GOLD), false);
        broadcast(gm.getServer(), owner, granted);
    }

    /** Applies a relative HP change; returns the new HP, or empty if the player has no character. */
    public static OptionalInt changeHp(MinecraftServer server, ServerPlayerEntity target, int delta) {
        Optional<CharacterSheet> sheet = CharacterStore.get(target);
        if (sheet.isEmpty()) {
            return OptionalInt.empty();
        }
        long raw = (long) sheet.get().currentHp() + delta;
        int hp = (int) Math.max(0, Math.min(CharacterMath.maxHp(sheet.get()), raw));
        CharacterSheet updated = sheet.get().withCurrentHp(hp);
        CharacterStore.set(target, updated);
        broadcast(server, target, updated);
        return OptionalInt.of(hp);
    }

    // --- level-up ---------------------------------------------------------------------------

    public static void levelUp(ServerPlayerEntity player, String skillIncrease, List<String> boostKeys,
                               List<String> featNames) {
        Optional<CharacterSheet> sheet = CharacterStore.get(player);
        if (sheet.isEmpty()) {
            return;
        }
        Set<Ability> boosts = EnumSet.noneOf(Ability.class);
        for (String key : boostKeys) {
            Ability ability = null;
            for (Ability candidate : Ability.values()) {
                if (candidate.pathbuilderKey().equalsIgnoreCase(key)) {
                    ability = candidate;
                }
            }
            if (ability == null) {
                error(player, Text.literal("Unknown ability: " + key));
                return;
            }
            boosts.add(ability);
        }
        LevelUpChoices choices = new LevelUpChoices(
                skillIncrease.isEmpty() ? Optional.empty() : Optional.of(skillIncrease), boosts, featNames);
        try {
            CharacterSheet leveled = LevelUp.apply(sheet.get(), ClassData.registry(), choices);
            CharacterStore.set(player, leveled);
            MinecraftServer server = player.getServer();
            Text notice = Text.translatableWithFallback("pf2craft.character.leveled",
                    "%s reached level %s!", player.getName(), leveled.level()).formatted(Formatting.GOLD);
            player.sendMessage(notice, false);
            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                if (other != player && GmRoles.isGm(other)) {
                    other.sendMessage(notice, false);
                }
            }
            broadcast(server, player, leveled);
        } catch (UnsupportedClassException | InvalidChoiceException e) {
            error(player, Text.literal(e.getMessage()));
        }
    }

    private static void offline(ServerPlayerEntity to) {
        error(to, Text.translatableWithFallback("pf2craft.character.player_offline",
                "That player is not online."));
    }

    private static void error(ServerPlayerEntity to, Text text) {
        to.sendMessage(text.copy().formatted(Formatting.RED), false);
    }
}
