package de.pf2craft;

import de.pf2craft.character.CharacterService;
import de.pf2craft.character.CharacterStore;
import de.pf2craft.character.ClassData;
import de.pf2craft.character.SheetService;
import de.pf2craft.command.Pf2Commands;
import de.pf2craft.item.ItemData;
import de.pf2craft.item.Pf2Components;
import de.pf2craft.network.Pf2Networking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pf2Craft implements ModInitializer {
    public static final String MOD_ID = "pf2craft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Pf2Components.register();
        CharacterStore.register();
        ServerLifecycleEvents.SERVER_STARTING.register(ClassData::load);
        ServerLifecycleEvents.SERVER_STARTING.register(ItemData::load);
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> CharacterService.onJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> SheetService.onDisconnect(handler.player));
        Pf2Networking.registerPayloads();
        Pf2Networking.registerServerReceivers();
        CommandRegistrationCallback.EVENT.register(Pf2Commands::register);
    }
}
