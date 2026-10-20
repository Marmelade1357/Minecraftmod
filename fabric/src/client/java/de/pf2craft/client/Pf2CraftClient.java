package de.pf2craft.client;

import de.pf2craft.network.ActionRequestPayload;
import de.pf2craft.network.CombatSyncPayload;
import de.pf2craft.network.EndTurnPayload;
import de.pf2craft.network.NpcSearchResultPayload;
import de.pf2craft.network.NpcSheetPayload;
import de.pf2craft.network.OpenAttackScreenPayload;
import de.pf2craft.network.OpenCombatScreenPayload;
import de.pf2craft.network.OpenImportPayload;
import de.pf2craft.network.OpenNpcScreenPayload;
import de.pf2craft.network.PendingActionPayload;
import de.pf2craft.network.RollResultPayload;
import de.pf2craft.network.SheetRequestPayload;
import de.pf2craft.network.SheetSyncPayload;
import de.pf2craft.npc.Pf2Entities;
import de.pf2craft.roll.RollMessages;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class Pf2CraftClient implements ClientModInitializer {
    public static final String KEY_CATEGORY = "key.categories.pf2craft";
    /** How long the C key waits for the own sheet before it assumes there is no character. */
    private static final int SHEET_WAIT_TICKS = 30;

    private static KeyBinding openDiceKey;
    private static KeyBinding openSheetKey;
    private static KeyBinding endTurnKey;
    private static KeyBinding actionMenuKey;
    private static KeyBinding strideKey;
    private static KeyBinding rangeToggleKey;
    /** Ticks left to wait for the own sheet after a C press, or -1. */
    private static int waitingForOwnSheet = -1;
    /** A screen the server asked for while another (e.g. the terrain loading) screen was open. */
    private static Supplier<Screen> pendingScreen;

    @Override
    public void onInitializeClient() {
        openDiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.dice", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY));
        openSheetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.sheet", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY));

        endTurnKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.end_turn", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, KEY_CATEGORY));

        actionMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.action_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY));

        strideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.stride", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY));
        rangeToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.range_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, KEY_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(Pf2CraftClient::tick);
        RangeOverlay.register();

        ClientPlayNetworking.registerGlobalReceiver(RollResultPayload.ID, (payload, context) -> {
            context.client().inGameHud.getChatHud().addMessage(RollMessages.chatLine(payload));
            RollHud.show(payload);
        });
        ClientPlayNetworking.registerGlobalReceiver(SheetSyncPayload.ID,
                (payload, context) -> onSheetSync(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(OpenImportPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            // The server only asks for an import when there is no character, so a cached one is stale.
            if (client.player != null) {
                ClientSheets.remove(client.player.getUuid());
            }
            waitingForOwnSheet = -1;
            show(client, ImportScreen::new);
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenNpcScreenPayload.ID, (payload, context) -> {
            if ("spawn".equals(payload.screen())) {
                show(context.client(), NpcSpawnScreen::new);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(NpcSearchResultPayload.ID, (payload, context) -> {
            if (context.client().currentScreen instanceof NpcSpawnScreen screen) {
                screen.onResults(payload.entries());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(NpcSheetPayload.ID, (payload, context) -> {
            if (context.client().currentScreen instanceof NpcSheetScreen screen
                    && screen.entityId() == payload.entityId()) {
                screen.update(payload.json());
            } else {
                show(context.client(), () -> new NpcSheetScreen(payload.entityId(), payload.json()));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(CombatSyncPayload.ID,
                (payload, context) -> {
                    ClientCombat.set(payload);
                    if (context.client().currentScreen instanceof ConditionEditScreen screen) {
                        screen.refresh();
                    }
                });
        ClientPlayNetworking.registerGlobalReceiver(PendingActionPayload.ID,
                (payload, context) -> ClientActions.setPending(payload.actionId()));
        ClientPlayNetworking.registerGlobalReceiver(OpenCombatScreenPayload.ID,
                (payload, context) -> show(context.client(), () -> new CombatSetupScreen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(OpenAttackScreenPayload.ID,
                (payload, context) -> show(context.client(), () -> new AttackScreen(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientSheets.clear();
            ClientCombat.clear();
            ClientActions.setPending("");
            waitingForOwnSheet = -1;
            pendingScreen = null;
        }));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                return;
            }
            Screen current = MinecraftClient.getInstance().currentScreen;
            if (current instanceof ImportScreen screen) {
                screen.onServerMessage(message);
            } else if (current instanceof LevelUpScreen screen) {
                screen.onServerMessage(message);
            } else if (current instanceof NpcSheetScreen screen) {
                screen.onServerMessage(message);
            } else if (current instanceof NpcSpawnScreen screen) {
                screen.onServerMessage(message);
            }
        });

        HudRenderCallback.EVENT.register(RollHud::render);
        HudRenderCallback.EVENT.register(CombatHud::render);
        EntityRendererRegistry.register(Pf2Entities.NPC, VillagerEntityRenderer::new);
    }

    private static void tick(MinecraftClient client) {
        while (openDiceKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                client.setScreen(new DiceScreen());
            }
        }
        while (openSheetKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                openOwnSheet(client);
            }
        }
        while (endTurnKey.wasPressed()) {
            // A GM also ends an NPC's turn (only GMs receive the exact hit points of NPC entries).
            boolean gmNpcTurn = ClientCombat.current().map(e -> e.npc() && e.hp() >= 0).orElse(false);
            if (client.currentScreen == null && client.player != null
                    && (ClientCombat.isTurnOf(client.player.getUuid()) || gmNpcTurn)
                    && ClientPlayNetworking.canSend(EndTurnPayload.ID)) {
                ClientPlayNetworking.send(new EndTurnPayload());
            }
        }
        while (actionMenuKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                onActionKey(client);
            }
        }
        while (strideKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                onStrideKey(client);
            }
        }
        while (rangeToggleKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                boolean on = !ClientActions.rangeDisplay();
                ClientActions.setRangeDisplay(on);
                client.player.sendMessage(Text.translatable(on ? "hud.pf2craft.range.on" : "hud.pf2craft.range.off")
                        .formatted(Formatting.YELLOW), true);
            }
        }
        if (waitingForOwnSheet > 0 && --waitingForOwnSheet == 0) {
            waitingForOwnSheet = -1;
            if (client.currentScreen == null && client.player != null) {
                client.setScreen(new ImportScreen());
            }
        }
        if (pendingScreen != null && client.currentScreen == null && client.player != null) {
            Supplier<Screen> screen = pendingScreen;
            pendingScreen = null;
            client.setScreen(screen.get());
        }
    }

    /** G: asks the server for a Stride; the server checks turn, actions and speed. */
    private static void onStrideKey(MinecraftClient client) {
        Optional<UUID> actor = ActionMenuScreen.actor(client);
        if (actor.isEmpty()) {
            client.player.sendMessage(Text.translatable("hud.pf2craft.action.not_your_turn")
                    .formatted(Formatting.RED), true);
            return;
        }
        if (ClientPlayNetworking.canSend(ActionRequestPayload.ID)) {
            ClientPlayNetworking.send(new ActionRequestPayload(actor.get(), "stride", -1));
        }
    }

    /** V: cancels a pending action, otherwise opens the action menu when someone the player controls is up. */
    private static void onActionKey(MinecraftClient client) {
        if (!ClientActions.pending().isEmpty()) {
            Optional<UUID> actor = ActionMenuScreen.actor(client);
            ClientActions.setPending("");
            if (ClientPlayNetworking.canSend(ActionRequestPayload.ID)) {
                // The server ignores the actor for a cancel; the own id is always valid.
                ClientPlayNetworking.send(new ActionRequestPayload(actor.orElse(client.player.getUuid()),
                        ActionRequestPayload.CANCEL, -1));
            }
            return;
        }
        if (ActionMenuScreen.actor(client).isEmpty()) {
            client.player.sendMessage(Text.translatable("hud.pf2craft.action.not_your_turn")
                    .formatted(Formatting.RED), true);
            return;
        }
        client.setScreen(new ActionMenuScreen());
    }

    private static void openOwnSheet(MinecraftClient client) {
        UUID own = client.player.getUuid();
        if (ClientSheets.get(own).isPresent()) {
            client.setScreen(new CharacterScreen(own));
            return;
        }
        if (!ClientPlayNetworking.canSend(SheetRequestPayload.ID)) {
            client.player.sendMessage(Text.translatable("screen.pf2craft.dice.server_missing")
                    .formatted(Formatting.RED), false);
            return;
        }
        // The server answers with an opening sync, or not at all if there is no character yet.
        ClientPlayNetworking.send(new SheetRequestPayload(own));
        waitingForOwnSheet = SHEET_WAIT_TICKS;
    }

    private static void onSheetSync(MinecraftClient client, SheetSyncPayload payload) {
        ClientSheets.put(payload);
        UUID owner = payload.owner();
        boolean own = client.player != null && owner.equals(client.player.getUuid());
        if (own) {
            waitingForOwnSheet = -1;
        }
        if (own && client.currentScreen instanceof LevelUpScreen screen) {
            screen.onSync();
        } else if (client.currentScreen instanceof CharacterScreen screen && screen.owner().equals(owner)) {
            screen.refresh();
        } else if (own && client.currentScreen instanceof ImportScreen) {
            // Import finished: replace the import screen with the new sheet.
            client.setScreen(new CharacterScreen(owner));
        } else if (payload.open()) {
            show(client, () -> new CharacterScreen(owner));
        }
    }

    /** Opens the screen now if nothing important is shown, otherwise as soon as the current one closes. */
    private static void show(MinecraftClient client, Supplier<Screen> screen) {
        Screen current = client.currentScreen;
        if (client.player != null && (current == null || current instanceof ChatScreen
                || current instanceof ImportScreen || current instanceof CharacterScreen
                || current instanceof DiceScreen || current instanceof NpcSpawnScreen
                || current instanceof NpcSheetScreen || current instanceof CombatSetupScreen
                || current instanceof AttackScreen)) {
            client.setScreen(screen.get());
        } else {
            pendingScreen = screen;
        }
    }
}
