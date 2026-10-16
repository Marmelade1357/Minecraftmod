package de.pf2craft.client;

import de.pf2craft.network.RollResultPayload;
import de.pf2craft.roll.RollMessages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Pf2CraftClient implements ClientModInitializer {
    public static final String KEY_CATEGORY = "key.categories.pf2craft";

    private static KeyBinding openDiceKey;

    @Override
    public void onInitializeClient() {
        openDiceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pf2craft.dice", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openDiceKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new DiceScreen());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(RollResultPayload.ID, (payload, context) -> {
            context.client().inGameHud.getChatHud().addMessage(RollMessages.chatLine(payload));
            RollHud.show(payload);
        });

        HudRenderCallback.EVENT.register(RollHud::render);
    }
}
