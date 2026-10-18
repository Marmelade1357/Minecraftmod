package de.pf2craft.core.item;

import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Strike;
import java.nio.charset.StandardCharsets;

/**
 * Fingerprint of the parts of a sheet that items are made from. Items carry it so that weapons
 * from an older import can be recognised as stale. Hit point changes and level-ups do not alter it,
 * so a GM-granted level-up keeps the weapons valid.
 */
public final class ItemStamp {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private ItemStamp() {
    }

    public static long of(CharacterSheet sheet) {
        StringBuilder sb = new StringBuilder();
        sb.append(sheet.name()).append('\u0000').append(sheet.className());
        for (Strike s : sheet.strikes()) {
            sb.append('\u0000').append(s);
        }
        long hash = FNV_OFFSET;
        for (byte b : sb.toString().getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xff;
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
