package de.pf2craft.npc;

import de.pf2craft.GmReach;
import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.NpcEdit;
import de.pf2craft.core.monster.NpcJson;
import de.pf2craft.core.monster.NpcStats;
import de.pf2craft.core.monster.TemplateNames;
import de.pf2craft.core.progression.InvalidChoiceException;
import de.pf2craft.network.NpcActionPayload;
import de.pf2craft.network.NpcEditPayload;
import de.pf2craft.network.NpcSearchPayload;
import de.pf2craft.network.NpcSearchResultPayload;
import de.pf2craft.network.NpcSheetPayload;
import de.pf2craft.network.NpcSpawnPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Server side of the NPC screens: every request is checked for the GM role and for a loaded NPC nearby. */
public final class NpcNetwork {
    /** An NPC further away than this cannot be edited from a screen. */
    public static final double MAX_DISTANCE = GmReach.GM_SCAN;
    private static final long SEARCH_COOLDOWN_MS = 200;
    private static final Map<UUID, Long> LAST_SEARCH = new ConcurrentHashMap<>();

    private NpcNetwork() {
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        LAST_SEARCH.remove(player.getUuid());
        NpcService.forgetPlayer(player.getUuid());
        GmSelection.clear(player);
    }

    public static void notGm(ServerPlayerEntity player) {
        player.sendMessage(Text.translatableWithFallback("pf2craft.npc.not_gm",
                "Only a game master can do that.").formatted(Formatting.RED), false);
    }

    public static void sendSheet(ServerPlayerEntity gm, NpcEntity npc) {
        ServerPlayNetworking.send(gm, new NpcSheetPayload(npc.getId(), NpcJson.write(npc.stats())));
    }

    public static void search(ServerPlayerEntity gm, NpcSearchPayload payload) {
        if (!GmRoles.isGm(gm)) {
            notGm(gm);
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_SEARCH.put(gm.getUuid(), now);
        if (last != null && now - last < SEARCH_COOLDOWN_MS) {
            return;
        }
        List<NpcSearchResultPayload.Entry> entries = MonsterDataLoader.catalog()
                .search(payload.text(), NpcSearchResultPayload.MAX_RESULTS).stream()
                .filter(m -> m.id().length() <= NpcSearchResultPayload.MAX_ID)
                .map(m -> new NpcSearchResultPayload.Entry(m.id(), clip(m.name()), m.level()))
                .toList();
        ServerPlayNetworking.send(gm, new NpcSearchResultPayload(entries));
    }

    public static void spawn(ServerPlayerEntity gm, NpcSpawnPayload payload) {
        if (!GmRoles.isGm(gm)) {
            notGm(gm);
            return;
        }
        Optional<MonsterData> data = MonsterDataLoader.catalog().byId(payload.monsterId());
        if (data.isEmpty()) {
            gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.unknown", "Unknown monster: %s",
                    payload.monsterId()).formatted(Formatting.RED), false);
            return;
        }
        NpcService.SpawnResult result = NpcService.spawn(gm, data.get());
        if (result.npc() == null) {
            if (result.error() != null) {
                gm.sendMessage(result.error().copy().formatted(Formatting.RED), false);
            }
            return;
        }
        gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.spawned", "Spawned %s.",
                result.npc().stats().displayName()), false);
    }

    public static void edit(ServerPlayerEntity gm, NpcEditPayload payload) {
        NpcEntity npc = resolve(gm, payload.entityId());
        if (npc == null) {
            return;
        }
        try {
            NpcService.apply(npc, NpcEdit.apply(npc.stats(), payload.field(), payload.value()));
        } catch (InvalidChoiceException e) {
            gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.edit_failed", "Invalid value: %s",
                    e.getMessage()).formatted(Formatting.RED), false);
        }
        sendSheet(gm, npc);
    }

    public static void action(ServerPlayerEntity gm, NpcActionPayload payload) {
        NpcEntity npc = resolve(gm, payload.entityId());
        if (npc == null) {
            return;
        }
        switch (payload.action()) {
            case "remove" -> {
                String name = npc.stats().displayName();
                NpcService.remove(npc);
                gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.removed", "Removed %s.", name), false);
            }
            case "save_template" -> saveTemplate(gm, npc);
            default -> gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.edit_failed",
                    "Invalid value: %s", payload.action()).formatted(Formatting.RED), false);
        }
    }

    /**
     * Writes the NPC's current stat block as a template; the id (and file name) is the sanitised display
     * name, so the template shows up in the catalog under that name from now on.
     */
    private static void saveTemplate(ServerPlayerEntity gm, NpcEntity npc) {
        NpcStats stats = npc.stats();
        MonsterData b = stats.base();
        String id = TemplateNames.freeId(stats.displayName(),
                candidate -> MonsterDataLoader.catalog().byId(candidate).isPresent());
        String fileName = id + ".json";
        MonsterData template = new MonsterData(id, stats.displayName(), b.level(), b.size(), b.traits(),
                b.perception(), b.ac(), b.fortitude(), b.reflex(), b.will(), b.hp(), b.speed(), b.strikes(),
                b.resistances(), b.weaknesses(), b.immunities(), b.skills(), b.abilities());
        Path dir = gm.getServer().getRunDirectory().resolve("config").resolve("pf2craft").resolve("monsters");
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName), NpcJson.writeTemplate(template), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Pf2Craft.LOGGER.error("Cannot save template {}: {}", fileName, e.getMessage());
            gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.template_failed",
                    "Could not save the template: %s", e.getMessage()).formatted(Formatting.RED), false);
            return;
        }
        MonsterDataLoader.add(template);
        gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.template_saved",
                "Template saved: %s (id %s)", fileName, id), false);
    }

    /** The NPC with this entity id if the sender is a GM and the NPC is loaded and near; else tells the GM. */
    private static NpcEntity resolve(ServerPlayerEntity gm, int entityId) {
        if (!GmRoles.isGm(gm)) {
            notGm(gm);
            return null;
        }
        Entity entity = gm.getServerWorld().getEntityById(entityId);
        if (entity instanceof NpcEntity npc && !npc.isRemoved() && npc.stats() != null
                && npc.squaredDistanceTo(gm) <= MAX_DISTANCE * MAX_DISTANCE) {
            return npc;
        }
        gm.sendMessage(Text.translatableWithFallback("pf2craft.npc.not_found",
                "That NPC is not loaded or too far away (within 128 blocks).").formatted(Formatting.RED), false);
        return null;
    }

    private static String clip(String s) {
        return s.length() <= NpcSearchResultPayload.MAX_NAME ? s : s.substring(0, NpcSearchResultPayload.MAX_NAME);
    }
}
