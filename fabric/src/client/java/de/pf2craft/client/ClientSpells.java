package de.pf2craft.client;

import de.pf2craft.core.spell.CastingType;
import de.pf2craft.core.spell.SpellcastingEntry;
import de.pf2craft.core.spell.Spellbook;
import de.pf2craft.network.ActionRequestPayload;
import de.pf2craft.network.CastSpellPayload;
import de.pf2craft.network.SpellTargetPayload;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Client side of spellcasting: the checks that grey out buttons (the server repeats all of them), sending the
 * cast request and the long raycast that picks a target further away than the vanilla reach.
 */
public final class ClientSpells {
    /** The id of the pending action the server reports while a spell waits for its targets. */
    public static final String PENDING_ID = "cast";
    /** The server checks the real range; this only bounds the client raycast. */
    private static final double TARGET_RANGE = 64.0;

    private ClientSpells() {
    }

    /** The own spellbook, if the player has one with at least one entry. */
    public static Optional<Spellbook> own(MinecraftClient client) {
        if (client.player == null) {
            return Optional.empty();
        }
        return ClientSpellbooks.get(client.player.getUuid()).filter(b -> !b.entries().isEmpty());
    }

    /**
     * Why the spell cannot be cast at this rank right now, empty if it can. The action cost of the spell is
     * only known to the server, so at least one action is required in a combat.
     */
    public static Optional<Text> unavailable(MinecraftClient client, Spellbook book, int entry, int rank) {
        SpellcastingEntry e = book.entries().get(entry);
        if (rank > 0) {
            switch (e.type()) {
                case INNATE -> {
                    return Optional.of(Text.translatable("screen.pf2craft.spells.innate"));
                }
                case FOCUS -> {
                    if (book.focusCurrent() <= 0) {
                        return Optional.of(Text.translatable("screen.pf2craft.spells.no_focus"));
                    }
                }
                default -> {
                    if (book.slotsLeft(entry, rank) <= 0) {
                        return Optional.of(Text.translatable("screen.pf2craft.spells.no_slot"));
                    }
                }
            }
        }
        var combat = ClientCombat.get();
        if (combat.isPresent() && client.player != null) {
            UUID me = client.player.getUuid();
            if (combat.get().entries().stream().anyMatch(en -> en.id().equals(me))) {
                if (!ClientCombat.isTurnOf(me)) {
                    return Optional.of(Text.translatable("screen.pf2craft.action_menu.not_your_turn"));
                }
                if (combat.get().actionsLeft() < 1) {
                    return Optional.of(Text.translatable("screen.pf2craft.action_menu.no_actions"));
                }
            }
        }
        return Optional.empty();
    }

    /** Sends the cast wish; the server answers with the pending target prompt or a refusal in chat. */
    public static boolean cast(int entry, String spell, int rank) {
        if (!ClientPlayNetworking.canSend(CastSpellPayload.ID)) {
            return false;
        }
        ClientPlayNetworking.send(new CastSpellPayload(entry, spell, rank));
        return true;
    }

    /** Confirms a multi-target spell with the targets chosen so far (key B). */
    public static void confirm(MinecraftClient client) {
        if (client.player != null && ClientActions.pending().equals(PENDING_ID)
                && ClientPlayNetworking.canSend(ActionRequestPayload.ID)) {
            ClientPlayNetworking.send(new ActionRequestPayload(client.player.getUuid(),
                    ActionRequestPayload.CONFIRM, -1));
        }
    }

    /**
     * While a spell waits for a target, a left click picks the creature under the crosshair with a long raycast
     * (stopped by blocks). Returns true if a target was sent; the vanilla attack is then skipped.
     */
    public static boolean tryTarget(MinecraftClient client) {
        if (client.player == null || !ClientActions.pending().equals(PENDING_ID)
                || !ClientPlayNetworking.canSend(SpellTargetPayload.ID)) {
            return false;
        }
        Entity camera = client.getCameraEntity() == null ? client.player : client.getCameraEntity();
        Vec3d start = camera.getCameraPosVec(1.0f);
        Vec3d direction = camera.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TARGET_RANGE));
        HitResult block = client.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE,
                camera));
        double limit = block.getType() == HitResult.Type.MISS ? TARGET_RANGE : block.getPos().distanceTo(start);
        Vec3d reach = start.add(direction.multiply(limit));
        Box box = camera.getBoundingBox().stretch(direction.multiply(limit)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(camera, start, reach, box,
                e -> !e.isSpectator() && e.canHit() && e != client.player, limit * limit);
        if (hit == null) {
            return false;
        }
        ClientPlayNetworking.send(new SpellTargetPayload(hit.getEntity().getId()));
        return true;
    }

    /** Translated tradition name; unknown traditions are shown capitalized as they are. */
    public static Text tradition(String tradition) {
        String key = "screen.pf2craft.spells.tradition." + tradition.toLowerCase(Locale.ROOT);
        if (Language.getInstance().hasTranslation(key)) {
            return Text.translatable(key);
        }
        return Text.literal(tradition.isEmpty() ? tradition
                : Character.toUpperCase(tradition.charAt(0)) + tradition.substring(1));
    }

    public static Text type(CastingType type) {
        return Text.translatable("screen.pf2craft.spells.type." + type.name().toLowerCase(Locale.ROOT));
    }
}
