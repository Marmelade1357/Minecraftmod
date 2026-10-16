package de.pf2craft.character;

import com.mojang.serialization.Codec;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.character.CharacterImportException;
import de.pf2craft.core.character.CharacterJson;
import de.pf2craft.core.character.CharacterSheet;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;

/** Persists one character sheet per player as a JSON string attached to the player entity. */
@SuppressWarnings("UnstableApiUsage")
public final class CharacterStore {
    private static AttachmentType<String> attachment;

    private CharacterStore() {
    }

    public static void register() {
        attachment = AttachmentRegistry.<String>builder()
                .persistent(Codec.STRING)
                .copyOnDeath()
                .buildAndRegister(Pf2Craft.id("character"));
    }

    public static Optional<CharacterSheet> get(ServerPlayerEntity player) {
        String json = player.getAttached(attachment);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(CharacterJson.read(json));
        } catch (CharacterImportException e) {
            Pf2Craft.LOGGER.warn("Ignoring corrupt character data of {}: {}",
                    player.getName().getString(), e.getMessage());
            return Optional.empty();
        }
    }

    public static void set(ServerPlayerEntity player, CharacterSheet sheet) {
        player.setAttached(attachment, CharacterJson.write(sheet));
    }

    public static void remove(ServerPlayerEntity player) {
        player.removeAttached(attachment);
    }
}
