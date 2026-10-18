package de.pf2craft.roll;

import de.pf2craft.GmRoles;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.dice.DiceExpression;
import de.pf2craft.core.dice.DiceParseException;
import de.pf2craft.core.dice.DieRoller;
import de.pf2craft.core.dice.RollResult;
import de.pf2craft.network.RollResultPayload;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/** Performs rolls on the server and distributes the results. */
public final class RollService {
    private static final DieRoller ROLLER = DieRoller.random(new SecureRandom());
    private static final long COOLDOWN_MS = 300;
    private static final int MAX_LABEL_LENGTH = 32;
    private static final Map<UUID, Long> LAST_ROLL = new HashMap<>();

    private RollService() {
    }

    /** Guards against packet spam. All calls happen on the server thread. */
    public static boolean checkCooldown(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        Long last = LAST_ROLL.get(player.getUuid());
        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }
        LAST_ROLL.put(player.getUuid(), now);
        return true;
    }

    /** Rolls with the server's secure roller and tells nobody, e.g. for initiative. */
    public static RollResult rollQuiet(String expression) throws DiceParseException {
        return DiceExpression.parse(expression).roll(ROLLER);
    }

    /**
     * Rolls {@code expression}. Public rolls go to everyone; secret rolls only to the roller
     * and the GMs, everyone else just learns that a secret roll happened.
     */
    public static void roll(MinecraftServer server, @Nullable ServerPlayerEntity roller, String rollerName,
                            String expression, String label, boolean secret) throws DiceParseException {
        RollResult result = DiceExpression.parse(expression).roll(ROLLER);

        int flags = secret ? RollResultPayload.FLAG_SECRET : 0;
        OptionalInt natural = result.naturalD20();
        if (natural.isPresent() && natural.getAsInt() == 20) {
            flags |= RollResultPayload.FLAG_NATURAL_20;
        } else if (natural.isPresent() && natural.getAsInt() == 1) {
            flags |= RollResultPayload.FLAG_NATURAL_1;
        }
        RollResultPayload payload = new RollResultPayload(rollerName, sanitizeLabel(label),
                result.expression().toNotation(), result.breakdown(), result.total(), flags);

        Pf2Craft.LOGGER.info("{}{} rolled {} = {}", secret ? "[secret] " : "", rollerName,
                result.breakdown(), result.total());

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (secret && player != roller && !GmRoles.isGm(player)) {
                player.sendMessage(Text.translatableWithFallback("pf2craft.roll.secret_notice",
                        "%s rolls secretly.", rollerName).formatted(Formatting.DARK_GRAY), false);
            } else if (ServerPlayNetworking.canSend(player, RollResultPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            } else {
                player.sendMessage(RollMessages.chatLine(payload), false);
            }
        }
    }

    private static String sanitizeLabel(String label) {
        String clean = label.replace("§", "").strip();
        return clean.length() > MAX_LABEL_LENGTH ? clean.substring(0, MAX_LABEL_LENGTH) : clean;
    }
}
