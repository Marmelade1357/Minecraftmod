package de.pf2craft.client;

import de.pf2craft.network.ControlActionPayload;
import de.pf2craft.network.ControlInputPayload;
import de.pf2craft.network.ControlStatePayload;
import de.pf2craft.network.TakeOverPayload;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client side of the NPC take-over: the camera follows the controlled NPC, the vanilla movement keys are sent
 * to the server as the NPC's input and the GM's own body is locked (see the client mixins).
 */
public final class ClientControl {
    /** Ticks to wait for the NPC entity to show up in the client world. */
    private static final int LOOKUP_INTERVAL = 20;

    private static UUID controlled;
    private static int lookupWait;
    private static boolean cameraSet;

    private ClientControl() {
    }

    /** The NPC entity the GM controls, if any. */
    public static Optional<UUID> controlled() {
        return Optional.ofNullable(controlled);
    }

    public static boolean isControlling() {
        return controlled != null;
    }

    public static void onState(MinecraftClient client, ControlStatePayload payload) {
        Optional<UUID> npc = payload.npcEntityUuid();
        if (npc.isPresent()) {
            controlled = npc.get();
            lookupWait = LOOKUP_INTERVAL;
            cameraSet = false;
            tryAttachCamera(client);
        } else {
            release(client);
        }
    }

    /** Restores the camera and forgets the controlled NPC (release message or disconnect). */
    public static void release(MinecraftClient client) {
        controlled = null;
        lookupWait = 0;
        cameraSet = false;
        if (client != null && client.player != null) {
            client.setCameraEntity(client.player);
        }
    }

    public static void toggle(MinecraftClient client) {
        if (ClientPlayNetworking.canSend(TakeOverPayload.ID)) {
            ClientPlayNetworking.send(new TakeOverPayload(!isControlling()));
        } else {
            client.player.sendMessage(Text.translatable("screen.pf2craft.dice.server_missing")
                    .formatted(Formatting.RED), false);
        }
    }

    /** Left click while controlling: the NPC strikes what it looks at. */
    public static void sendAttack() {
        if (ClientPlayNetworking.canSend(ControlActionPayload.ID)) {
            ClientPlayNetworking.send(new ControlActionPayload("attack"));
        }
    }

    private static Entity findEntity(MinecraftClient client) {
        if (client.world == null || controlled == null) {
            return null;
        }
        for (Entity e : client.world.getEntities()) {
            if (e.getUuid().equals(controlled)) {
                return e;
            }
        }
        return null;
    }

    private static void tryAttachCamera(MinecraftClient client) {
        Entity npc = findEntity(client);
        if (npc == null || client.player == null) {
            return;
        }
        // Start looking where the NPC looks so the view does not jump.
        client.player.setYaw(npc.getYaw());
        client.player.setPitch(npc.getPitch());
        client.setCameraEntity(npc);
        cameraSet = true;
    }

    /** Display name of the controlled NPC for the HUD. */
    public static Text name(MinecraftClient client) {
        if (controlled == null) {
            return Text.empty();
        }
        Entity e = findEntity(client);
        return e != null ? e.getDisplayName() : Text.literal("NPC");
    }

    public static void tick(MinecraftClient client) {
        if (controlled == null) {
            return;
        }
        if (client.player == null || client.world == null) {
            release(client);
            return;
        }
        if (!cameraSet) {
            // Keep trying (every 20 ticks) for as long as control is active; the server releases if it never works.
            if (--lookupWait <= 0) {
                lookupWait = LOOKUP_INTERVAL;
                tryAttachCamera(client);
            }
        } else if (client.getCameraEntity() != findEntity(client)) {
            // The NPC entity was reloaded or the camera was reset: attach again.
            cameraSet = false;
            lookupWait = LOOKUP_INTERVAL;
            tryAttachCamera(client);
        }
        if (!ClientPlayNetworking.canSend(ControlInputPayload.ID)) {
            return;
        }
        boolean free = client.currentScreen == null;
        float forward = 0;
        float sideways = 0;
        boolean jump = false;
        boolean sprint = false;
        if (free) {
            forward = (client.options.forwardKey.isPressed() ? 1 : 0) - (client.options.backKey.isPressed() ? 1 : 0);
            // Vanilla convention: positive is to the left.
            sideways = (client.options.leftKey.isPressed() ? 1 : 0) - (client.options.rightKey.isPressed() ? 1 : 0);
            jump = client.options.jumpKey.isPressed();
            sprint = client.options.sprintKey.isPressed();
        }
        ClientPlayNetworking.send(new ControlInputPayload(forward, sideways, jump, sprint,
                client.player.getYaw(), client.player.getPitch()));
    }
}
