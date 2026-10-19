package de.pf2craft.client;

import de.pf2craft.network.CombatSyncPayload;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;

/** Client copy of the running combat as the server last sent it; empty when no combat is running. */
public final class ClientCombat {
    private static CombatSyncPayload state;

    private ClientCombat() {
    }

    public static void set(CombatSyncPayload payload) {
        boolean wasActive = state != null;
        state = payload.active() ? payload : null;
        rewrapChat(wasActive);
    }

    public static void clear() {
        boolean wasActive = state != null;
        state = null;
        rewrapChat(wasActive);
    }

    /** The combat chat scale changes how lines wrap, so rebuild the visible chat when a combat starts or ends. */
    private static void rewrapChat(boolean wasActive) {
        if (wasActive != (state != null)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.inGameHud != null) {
                client.inGameHud.getChatHud().reset();
            }
        }
    }

    public static Optional<CombatSyncPayload> get() {
        return Optional.ofNullable(state);
    }

    /** The entry whose turn it is, if a combat is running. */
    public static Optional<CombatSyncPayload.Entry> current() {
        if (state == null) {
            return Optional.empty();
        }
        return state.entries().stream().filter(CombatSyncPayload.Entry::current).findFirst();
    }

    public static boolean isTurnOf(UUID id) {
        return current().map(e -> e.id().equals(id)).orElse(false);
    }
}
