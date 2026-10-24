package de.pf2craft.spell;

import com.mojang.serialization.Codec;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.spell.Spellbook;
import de.pf2craft.core.spell.SpellbookJson;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;

/** Persists one spellbook per player as a JSON string attached to the player entity. */
@SuppressWarnings("UnstableApiUsage")
public final class SpellbookStore {
    private static AttachmentType<String> attachment;

    private SpellbookStore() {
    }

    public static void register() {
        attachment = AttachmentRegistry.<String>create(Pf2Craft.id("spellbook"),
                builder -> builder.persistent(Codec.STRING).copyOnDeath());
    }

    /** The player's spellbook, or an empty one if there is none (or it is unreadable). */
    public static Spellbook get(ServerPlayerEntity player) {
        String json = player.getAttached(attachment);
        if (json == null) {
            return Spellbook.empty();
        }
        try {
            return SpellbookJson.read(json);
        } catch (RuntimeException e) {
            Pf2Craft.LOGGER.warn("Ignoring corrupt spellbook data of {}: {}",
                    player.getName().getString(), e.getMessage());
            return Spellbook.empty();
        }
    }

    public static void set(ServerPlayerEntity player, Spellbook book) {
        player.setAttached(attachment, SpellbookJson.write(book));
    }

    public static void remove(ServerPlayerEntity player) {
        player.removeAttached(attachment);
    }
}
