package de.pf2craft.combat;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.character.CharacterStore;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.combat.Combatant;
import de.pf2craft.core.combat.Encounter;
import de.pf2craft.core.combat.HealthLabel;
import de.pf2craft.core.combat.MovementBudget;
import de.pf2craft.core.combat.TurnState;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.monster.NpcStats;
import de.pf2craft.network.CombatSyncPayload;
import de.pf2craft.network.OpenCombatScreenPayload;
import de.pf2craft.npc.NpcEntity;
import de.pf2craft.roll.RollService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Server side of the combat: owns the one running {@link Encounter}, rolls initiative, hands out turns and
 * keeps all clients in sync. Static, server thread only. Combat is not persisted; a restart ends it.
 */
public final class CombatService {
    /** Candidates and fallback-command participants must be this close to the GM. */
    public static final double RADIUS = 32.0;
    private static final int FALLBACK_SPEED = 25;

    private static Encounter encounter;
    private static MovementBudget budget = new MovementBudget(FALLBACK_SPEED);
    private static int announcedRound;
    private static MinecraftServer server;
    private static final Set<String> DEFEATED = new HashSet<>();

    /** Temporary attribute bonus that lets the click reach far targets; the server's reach rules still decide. */
    private static final Identifier REACH_ID = Pf2Craft.id("combat_reach");
    private static final double REACH_BONUS = 12.0;

    private CombatService() {
    }

    private static void applyReach(ServerPlayerEntity player) {
        EntityAttributeInstance range = player.getAttributeInstance(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
        if (range != null && range.getModifier(REACH_ID) == null) {
            range.addTemporaryModifier(new EntityAttributeModifier(REACH_ID, REACH_BONUS,
                    EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeReach(ServerPlayerEntity player) {
        EntityAttributeInstance range = player.getAttributeInstance(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE);
        if (range != null) {
            range.removeModifier(REACH_ID);
        }
    }

    public static boolean active() {
        return encounter != null;
    }

    /** Whether the creature takes part in the running combat (active or not). */
    public static boolean contains(UUID id) {
        return encounter != null && containsId(id.toString());
    }

    public static Optional<Encounter> encounter() {
        return Optional.ofNullable(encounter);
    }

    public static Optional<String> currentId() {
        return encounter == null ? Optional.empty() : encounter.current().map(Combatant::id);
    }

    public static TurnState turn() {
        return encounter == null ? new TurnState() : encounter.turn();
    }

    public static MovementBudget budget() {
        return budget;
    }

    /** Clears all state without messages, e.g. when the server stops. */
    public static void reset() {
        encounter = null;
        server = null;
        DEFEATED.clear();
        budget = new MovementBudget(FALLBACK_SPEED);
        Enforcement.onCombatEnded();
    }

    // ---- candidates ------------------------------------------------------------------------------------------

    /** Characters (non-GM) and living NPCs near the GM, for the combat screen. */
    public static List<OpenCombatScreenPayload.Candidate> candidates(ServerPlayerEntity gm) {
        List<OpenCombatScreenPayload.Candidate> result = new ArrayList<>();
        if (CharacterStore.get(gm).isPresent()) {
            // The requesting GM's own character may be ticked in the window (opt-in).
            result.add(new OpenCombatScreenPayload.Candidate(gm.getUuid(), clip(playerName(gm)), false));
        }
        for (Entity e : nearby(gm.getServerWorld(), gm.getPos())) {
            if (e instanceof ServerPlayerEntity p) {
                result.add(new OpenCombatScreenPayload.Candidate(p.getUuid(), clip(playerName(p)), false));
            } else if (e instanceof NpcEntity npc) {
                result.add(new OpenCombatScreenPayload.Candidate(npc.getUuid(), clip(npc.stats().displayName()),
                        true));
            }
            if (result.size() >= OpenCombatScreenPayload.MAX_CANDIDATES) {
                break;
            }
        }
        return result;
    }

    /** UUIDs of everything {@link #candidates} would list around {@code center}. */
    public static List<UUID> nearbyIds(ServerWorld world, Vec3d center) {
        return nearby(world, center).stream().map(Entity::getUuid).limit(OpenCombatScreenPayload.MAX_CANDIDATES)
                .toList();
    }

    private static List<Entity> nearby(ServerWorld world, Vec3d center) {
        List<Entity> result = new ArrayList<>();
        double max = RADIUS * RADIUS;
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (!GmRoles.isGm(p) && p.squaredDistanceTo(center) <= max && CharacterStore.get(p).isPresent()) {
                result.add(p);
            }
        }
        result.addAll(world.getEntitiesByClass(NpcEntity.class,
                Box.of(center, RADIUS * 2, RADIUS * 2, RADIUS * 2),
                e -> e.stats() != null && !e.stats().defeated() && e.squaredDistanceTo(center) <= max));
        return result;
    }

    // ---- starting and ending ---------------------------------------------------------------------------------

    public static void startFromGm(ServerPlayerEntity gm, Collection<UUID> participantIds) {
        if (!GmRoles.isGm(gm)) {
            CombatMessages.error(gm, CombatMessages.notGm());
            return;
        }
        if (encounter != null) {
            // A running combat: the list means "add these"; those already taking part are ignored.
            for (UUID id : new LinkedHashSet<>(participantIds)) {
                add(gm, id);
            }
            return;
        }
        start(gm.getServer(), gm, participantIds);
    }

    /** Candidates near the GM that do not yet take part in the running combat. */
    public static List<OpenCombatScreenPayload.Candidate> newCandidates(ServerPlayerEntity gm) {
        List<OpenCombatScreenPayload.Candidate> result = new ArrayList<>();
        for (OpenCombatScreenPayload.Candidate c : candidates(gm)) {
            if (encounter == null || !containsId(c.id().toString())) {
                result.add(c);
            }
        }
        return result;
    }

    public static void removeFromGm(ServerPlayerEntity gm, UUID id) {
        if (!GmRoles.isGm(gm)) {
            CombatMessages.error(gm, CombatMessages.notGm());
            return;
        }
        if (encounter == null) {
            CombatMessages.error(gm, CombatMessages.notRunning());
            return;
        }
        remove(id);
    }

    /** Command path ({@code /pf2 combat remove}, permission checked by the command); false if not taking part. */
    public static boolean removeFromCommand(UUID id) {
        if (encounter == null || !containsId(id.toString())) {
            return false;
        }
        remove(id);
        return true;
    }

    /** {@code /pf2 combat add <players>}: adds the named players (a GM only with a character), permission by command. */
    public static int addPlayers(ServerPlayerEntity actor, Collection<ServerPlayerEntity> players) {
        if (encounter == null) {
            CombatMessages.error(actor, CombatMessages.notRunning());
            return 0;
        }
        int before = encounter.order().size();
        for (ServerPlayerEntity p : players) {
            add(actor, p.getUuid());
        }
        return encounter == null ? 0 : encounter.order().size() - before;
    }

    /** Takes a participant out of the running order; the turn moves on if it was the current one. */
    public static void remove(UUID uuid) {
        String id = uuid.toString();
        if (encounter == null || !containsId(id)) {
            return;
        }
        boolean wasCurrent = currentId().filter(id::equals).isPresent();
        String name = encounter.order().stream().filter(e -> e.combatant().id().equals(id))
                .map(e -> e.combatant().name()).findFirst().orElse(id);
        List<ServerPlayerEntity> audience = recipients(null);
        ServerPlayerEntity removedPlayer = server.getPlayerManager().getPlayer(uuid);
        encounter.remove(id);
        DEFEATED.remove(id);
        Enforcement.onLeft(uuid);
        for (ServerPlayerEntity r : audience) {
            r.sendMessage(CombatMessages.removed(name), false);
        }
        if (wasCurrent) {
            nextTurn(null);
        } else if (!encounter.hasActive(true) && !encounter.hasActive(false)) {
            finish(null);
        }
        if (encounter != null) {
            sync(null);
        }
        if (removedPlayer != null && !GmRoles.isGm(removedPlayer)) {
            removeReach(removedPlayer);
        }
        if (removedPlayer != null && !GmRoles.isGm(removedPlayer)
                && ServerPlayNetworking.canSend(removedPlayer, CombatSyncPayload.ID)) {
            ServerPlayNetworking.send(removedPlayer, CombatSyncPayload.inactive());
        }
    }

    /** Fallback for /pf2 combat add: everything near the GM that is not yet taking part. */
    public static int addNearby(ServerPlayerEntity gm) {
        // The command checks "game master or permission level 2"; no role check here.
        if (encounter == null) {
            CombatMessages.error(gm, CombatMessages.notRunning());
            return 0;
        }
        int before = encounter.order().size();
        for (UUID id : nearbyIds(gm.getServerWorld(), gm.getPos())) {
            add(gm, id);
        }
        return encounter == null ? 0 : encounter.order().size() - before;
    }

    /** @return whether a combat was started */
    public static boolean start(MinecraftServer srv, @Nullable ServerPlayerEntity gm,
                                Collection<UUID> participantIds) {
        if (encounter != null) {
            if (gm != null) {
                CombatMessages.error(gm, CombatMessages.alreadyRunning());
            }
            return false;
        }
        Encounter fresh = new Encounter();
        for (UUID id : new LinkedHashSet<>(participantIds)) {
            Combatant c = resolve(srv, id);
            if (c != null) {
                fresh.add(c, rollInitiative(c));
            }
        }
        fresh.start();
        Optional<Combatant> first = fresh.current();
        if (first.isEmpty()) {
            if (gm != null) {
                CombatMessages.error(gm, CombatMessages.noParticipants());
            }
            return false;
        }
        encounter = fresh;
        server = srv;
        DEFEATED.clear();
        budget = new MovementBudget(speedOf(first.get().id()));
        announcedRound = 1;
        Pf2Craft.LOGGER.info("Combat started with {} participants", fresh.order().size());

        for (ServerPlayerEntity r : recipients(null)) {
            applyReach(r);
            boolean viewerGm = GmRoles.isGm(r);
            r.sendMessage(CombatMessages.started(), false);
            r.sendMessage(CombatMessages.round(1), false);
            List<Encounter.Entry> order = fresh.order();
            for (int i = 0; i < order.size(); i++) {
                Encounter.Entry e = order.get(i);
                r.sendMessage(CombatMessages.orderLine(i + 1, e.combatant().name(), e.initiative(),
                        viewerGm || !e.combatant().npc(), e.combatant().id().equals(first.get().id()),
                        e.active()), false);
            }
        }
        announceTurn(first.get(), null);
        sync(null);
        return true;
    }

    /** Late joiner: rolls initiative and inserts the creature into the running order. */
    public static void add(ServerPlayerEntity gm, UUID id) {
        if (encounter == null) {
            return;
        }
        Combatant c = resolve(gm.getServer(), id);
        if (c == null || containsId(c.id())) {
            return;
        }
        int init = rollInitiative(c);
        encounter.add(c, init);
        ServerPlayerEntity joined = gm.getServer().getPlayerManager().getPlayer(id);
        if (joined != null) {
            applyReach(joined);
        }
        for (ServerPlayerEntity r : recipients(null)) {
            r.sendMessage(c.npc() && !GmRoles.isGm(r)
                    ? CombatMessages.lateJoinHidden(c.name()) : CombatMessages.lateJoin(c.name(), init), false);
        }
        sync(null);
    }

    public static void control(ServerPlayerEntity player, String action) {
        if (!GmRoles.isGm(player)) {
            CombatMessages.error(player, CombatMessages.notGm());
            return;
        }
        switch (action) {
            case "skip" -> skip(player);
            case "end" -> end(player);
            default -> { }
        }
    }

    public static void skip(ServerPlayerEntity gm) {
        if (encounter == null) {
            CombatMessages.error(gm, CombatMessages.notRunning());
            return;
        }
        nextTurn(null);
    }

    public static void end(ServerPlayerEntity gm) {
        if (encounter == null) {
            CombatMessages.error(gm, CombatMessages.notRunning());
            return;
        }
        finish(null);
    }

    public static void endTurn(ServerPlayerEntity actor) {
        if (encounter == null) {
            CombatMessages.error(actor, CombatMessages.notRunning());
            return;
        }
        Optional<String> current = currentId();
        boolean allowed = current.isPresent()
                && (GmRoles.isGm(actor) || current.get().equals(actor.getUuid().toString()));
        if (!allowed) {
            CombatMessages.error(actor, CombatMessages.notYourTurn());
            return;
        }
        nextTurn(null);
    }

    // ---- connection and defeat -------------------------------------------------------------------------------

    public static void onDisconnect(ServerPlayerEntity player) {
        if (encounter == null || !containsId(player.getUuid().toString())) {
            return;
        }
        deactivate(player.getUuid().toString(), player);
    }

    public static void onJoin(ServerPlayerEntity player) {
        if (encounter == null) {
            return;
        }
        String id = player.getUuid().toString();
        if (containsId(id) && !DEFEATED.contains(id)) {
            Optional<CharacterSheet> sheet = CharacterStore.get(player);
            if (sheet.isPresent() && sheet.get().currentHp() > 0) {
                encounter.setActive(id, true);
            }
        }
        if (GmRoles.isGm(player) || containsId(id)) {
            applyReach(player);
        }
        sync(null);
    }

    /** A creature dropped out (0 HP): it no longer gets turns; the combat ends when nobody is left. */
    public static void markDefeated(String id) {
        if (encounter == null || !containsId(id)) {
            return;
        }
        DEFEATED.add(id);
        deactivate(id, null);
    }

    /**
     * A creature that was out at 0 HP is up again (HP raised): it takes part once more, the current turn pointer
     * stays where it is. Does nothing for creatures that are active or not in the combat.
     */
    public static void reviveIfNeeded(String id) {
        if (encounter == null || !containsId(id)) {
            return;
        }
        DEFEATED.remove(id);
        for (Encounter.Entry e : encounter.order()) {
            if (e.combatant().id().equals(id) && !e.active()) {
                encounter.setActive(id, true);
                sync(null);
                return;
            }
        }
    }

    private static void deactivate(String id, @Nullable ServerPlayerEntity leaving) {
        boolean wasCurrent = currentId().filter(id::equals).isPresent();
        encounter.setActive(id, false);
        if (wasCurrent) {
            nextTurn(leaving);
            if (encounter == null) {
                return;
            }
        }
        if (!encounter.hasActive(true) || !encounter.hasActive(false)) {
            // Only the GMs are told; the combat goes on until nobody at all is active.
            Text hint = encounter.hasActive(true) ? CombatMessages.canEndPlayers() : CombatMessages.canEnd();
            for (ServerPlayerEntity gm : recipients(leaving)) {
                if (GmRoles.isGm(gm)) {
                    gm.sendMessage(hint, false);
                }
            }
        }
        sync(leaving);
    }

    // ---- turn handling ---------------------------------------------------------------------------------------

    /**
     * Removes NPC combatants whose entity is gone (removed or not loaded) from the order. Deactivating the
     * current one advances the turn; inactive entries are skipped, so this cannot recurse endlessly.
     */
    public static void purgeGone() {
        if (encounter == null || server == null) {
            return;
        }
        for (Encounter.Entry e : List.copyOf(encounter.order())) {
            if (encounter == null) {
                return;
            }
            Combatant c = e.combatant();
            if (c.npc() && e.active() && findNpc(server, UUID.fromString(c.id())) == null) {
                for (ServerPlayerEntity gm : recipients(null)) {
                    if (GmRoles.isGm(gm)) {
                        gm.sendMessage(CombatMessages.npcGone(c.name()), false);
                    }
                }
                markDefeated(c.id());
            }
        }
    }

    private static void nextTurn(@Nullable ServerPlayerEntity exclude) {
        Optional<String> before = currentId();
        purgeGone();
        if (encounter == null || !currentId().equals(before)) {
            return; // the purge already moved on (or ended the combat)
        }
        Optional<Combatant> next = encounter.advance();
        if (next.isEmpty()) {
            finish(exclude);
            return;
        }
        budget = new MovementBudget(speedOf(next.get().id()));
        if (encounter.round() != announcedRound) {
            announcedRound = encounter.round();
            for (ServerPlayerEntity r : recipients(exclude)) {
                r.sendMessage(CombatMessages.round(announcedRound), false);
            }
        }
        announceTurn(next.get(), exclude);
        sync(exclude);
    }

    private static void announceTurn(Combatant c, @Nullable ServerPlayerEntity exclude) {
        for (ServerPlayerEntity r : recipients(exclude)) {
            r.sendMessage(CombatMessages.turn(c.name()), false);
            if (r.getUuid().toString().equals(c.id())) {
                r.sendMessage(CombatMessages.yourTurn(), true);
            }
        }
    }

    private static void finish(@Nullable ServerPlayerEntity exclude) {
        List<ServerPlayerEntity> audience = recipients(exclude);
        encounter = null;
        DEFEATED.clear();
        Enforcement.onCombatEnded();
        Pf2Craft.LOGGER.info("Combat ended");
        if (server != null) {
            server.getPlayerManager().getPlayerList().forEach(CombatService::removeReach);
        }
        for (ServerPlayerEntity r : audience) {
            r.sendMessage(CombatMessages.ended(), false);
            if (ServerPlayNetworking.canSend(r, CombatSyncPayload.ID)) {
                ServerPlayNetworking.send(r, CombatSyncPayload.inactive());
            }
        }
    }

    // ---- status ----------------------------------------------------------------------------------------------

    /** Chat lines for {@code /pf2 combat status}; empty if no combat runs. */
    public static List<Text> statusLines(boolean viewerGm) {
        List<Text> lines = new ArrayList<>();
        if (encounter == null) {
            return lines;
        }
        Combatant current = encounter.current().orElse(null);
        lines.add(CombatMessages.statusHead(encounter.round(), current == null ? "-" : current.name(),
                encounter.turn().actionsLeft(), encounter.turn().reactionAvailable()));
        List<Encounter.Entry> order = encounter.order();
        for (int i = 0; i < order.size(); i++) {
            Encounter.Entry e = order.get(i);
            lines.add(CombatMessages.orderLine(i + 1, e.combatant().name(), e.initiative(),
                    viewerGm || !e.combatant().npc(), current != null && current.id().equals(e.combatant().id()),
                    e.active()));
        }
        return lines;
    }

    // ---- sync ------------------------------------------------------------------------------------------------

    /** Sends the current state, built per recipient, to every online combatant player and every GM. */
    public static void sync(@Nullable ServerPlayerEntity exclude) {
        purgeGone();
        if (encounter == null || server == null) {
            return;
        }
        String currentId = currentId().orElse("");
        for (ServerPlayerEntity r : recipients(exclude)) {
            if (!ServerPlayNetworking.canSend(r, CombatSyncPayload.ID)) {
                continue;
            }
            boolean gm = GmRoles.isGm(r);
            List<CombatSyncPayload.Entry> entries = new ArrayList<>();
            for (Encounter.Entry e : encounter.order()) {
                Combatant c = e.combatant();
                UUID uuid = UUID.fromString(c.id());
                if (c.npc()) {
                    NpcEntity npc = findNpc(server, uuid);
                    NpcStats stats = npc == null ? null : npc.stats();
                    byte label = (byte) (stats == null ? HealthLabel.UNHURT.ordinal()
                            : HealthLabel.of(stats.currentHp(), stats.base().hp()).ordinal());
                    entries.add(new CombatSyncPayload.Entry(uuid, clip(c.name()), true,
                            gm ? e.initiative() : -1, label,
                            gm && stats != null ? stats.currentHp() : -1,
                            gm && stats != null ? stats.base().hp() : -1,
                            e.active(), c.id().equals(currentId)));
                } else {
                    entries.add(new CombatSyncPayload.Entry(uuid, clip(c.name()), false, e.initiative(),
                            CombatSyncPayload.Entry.PLAYER_LABEL, -1, -1, e.active(), c.id().equals(currentId)));
                }
            }
            TurnState t = encounter.turn();
            int stride = Math.max(0, (int) Math.ceil(budget.remainingInStride()));
            ServerPlayNetworking.send(r, new CombatSyncPayload(true, encounter.round(), entries,
                    t.actionsLeft(), t.reactionAvailable(), stride));
        }
    }

    /** Online players that are combatants plus all GMs. */
    private static List<ServerPlayerEntity> recipients(@Nullable ServerPlayerEntity exclude) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        if (server == null || encounter == null) {
            return result;
        }
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p != exclude && (GmRoles.isGm(p) || containsId(p.getUuid().toString()))) {
                result.add(p);
            }
        }
        return result;
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private static boolean containsId(String id) {
        for (Encounter.Entry e : encounter.order()) {
            if (e.combatant().id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Combatant for a player with a character or a loaded NPC; null for anything else (GMs included). */
    @Nullable
    private static Combatant resolve(MinecraftServer srv, UUID id) {
        ServerPlayerEntity player = srv.getPlayerManager().getPlayer(id);
        if (player != null) {
            // A game master takes part only when listed explicitly and with a character.
            return CharacterStore.get(player)
                    .map(s -> new Combatant(id.toString(), playerName(player, s), false,
                            CharacterMath.perception(s)))
                    .orElse(null);
        }
        NpcEntity npc = findNpc(srv, id);
        if (npc != null && !npc.stats().defeated()) {
            return new Combatant(id.toString(), npc.stats().displayName(), true, npc.stats().base().perception());
        }
        return null;
    }

    private static int rollInitiative(Combatant c) {
        try {
            return RollService.rollQuiet("1d20").total() + c.perception();
        } catch (DiceParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int speedOf(String id) {
        UUID uuid = UUID.fromString(id);
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return CharacterStore.get(player).map(CharacterSheet::speed).orElse(FALLBACK_SPEED);
        }
        NpcEntity npc = findNpc(server, uuid);
        return npc != null ? npc.stats().base().speed() : FALLBACK_SPEED;
    }

    @Nullable
    private static NpcEntity findNpc(MinecraftServer srv, UUID id) {
        for (ServerWorld world : srv.getWorlds()) {
            if (world.getEntity(id) instanceof NpcEntity npc && !npc.isRemoved() && npc.stats() != null) {
                return npc;
            }
        }
        return null;
    }

    private static String playerName(ServerPlayerEntity player) {
        return CharacterStore.get(player).map(s -> playerName(player, s)).orElse(player.getName().getString());
    }

    private static String playerName(ServerPlayerEntity player, CharacterSheet sheet) {
        return sheet.name() == null || sheet.name().isBlank() ? player.getName().getString() : sheet.name();
    }

    private static String clip(String s) {
        return s.length() <= 64 ? s : s.substring(0, 64);
    }
}
