package de.pf2craft.roll;

import de.pf2craft.network.RollResultPayload;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Builds the chat line for a roll. Shared by the client and the vanilla-client fallback. */
public final class RollMessages {
    private RollMessages() {
    }

    public static MutableText total(RollResultPayload result) {
        Formatting color = result.natural20() ? Formatting.GOLD
                : result.natural1() ? Formatting.RED : Formatting.WHITE;
        return Text.literal(String.valueOf(result.total())).formatted(Formatting.BOLD, color);
    }

    public static MutableText chatLine(RollResultPayload result) {
        MutableText line = Text.empty();
        if (result.secret()) {
            line.append(Text.translatableWithFallback("pf2craft.roll.secret_prefix", "[Secret] ")
                    .formatted(Formatting.DARK_PURPLE));
        }
        Text roller = Text.literal(result.roller()).formatted(Formatting.AQUA);
        Text breakdown = Text.literal(result.breakdown()).formatted(Formatting.GRAY);
        if (result.label().isEmpty()) {
            line.append(Text.translatableWithFallback("pf2craft.roll.chat", "%s rolls %s = %s",
                    roller, breakdown, total(result)));
        } else {
            line.append(Text.translatableWithFallback("pf2craft.roll.chat_labeled", "%s rolls %s: %s = %s",
                    roller, Text.literal(result.label()).formatted(Formatting.YELLOW), breakdown, total(result)));
        }
        return line;
    }
}
