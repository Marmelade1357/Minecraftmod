package de.pf2craft.combat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Chat texts of the combat; every key exists in both language files. */
final class CombatMessages {
    private CombatMessages() {
    }

    static MutableText started() {
        return Text.translatableWithFallback("pf2craft.combat.started", "Combat begins!")
                .formatted(Formatting.GOLD, Formatting.BOLD);
    }

    static MutableText ended() {
        return Text.translatableWithFallback("pf2craft.combat.ended", "Combat ended.")
                .formatted(Formatting.GOLD);
    }

    static MutableText orderLine(int position, String name, int initiative, boolean showInitiative, boolean current,
                                 boolean active) {
        MutableText line = showInitiative
                ? Text.translatableWithFallback("pf2craft.combat.order_line", "%s. %s (initiative %s)",
                        position, name, initiative)
                : Text.translatableWithFallback("pf2craft.combat.order_line_hidden", "%s. %s", position, name);
        if (!active) {
            line.append(Text.literal(" ")).append(Text.translatableWithFallback("pf2craft.combat.out", "(out)"));
            return line.formatted(Formatting.DARK_GRAY);
        }
        return current ? Text.literal("> ").append(line).formatted(Formatting.YELLOW) : line.formatted(Formatting.GRAY);
    }

    static MutableText round(int round) {
        return Text.translatableWithFallback("pf2craft.combat.round", "Round %s", round)
                .formatted(Formatting.GOLD);
    }

    static MutableText turn(String name) {
        return Text.translatableWithFallback("pf2craft.combat.turn", "%s is up.", name)
                .formatted(Formatting.YELLOW);
    }

    static MutableText yourTurn() {
        return Text.translatableWithFallback("pf2craft.combat.your_turn", "It is your turn!")
                .formatted(Formatting.GREEN, Formatting.BOLD);
    }

    static MutableText lateJoin(String name, int initiative) {
        return Text.translatableWithFallback("pf2craft.combat.late_join", "%s joins the combat (initiative %s).",
                name, initiative).formatted(Formatting.GOLD);
    }

    static MutableText lateJoinHidden(String name) {
        return Text.translatableWithFallback("pf2craft.combat.late_join_hidden", "%s joins the combat.", name)
                .formatted(Formatting.GOLD);
    }

    static MutableText npcGone(String name) {
        return Text.translatableWithFallback("pf2craft.combat.npc_gone",
                "%s is gone and was removed from the order.", name).formatted(Formatting.GOLD);
    }

    static MutableText removed(String name) {
        return Text.translatableWithFallback("pf2craft.combat.removed",
                "%s was removed from the combat.", name).formatted(Formatting.GOLD);
    }

    static MutableText canEnd() {
        return Text.translatableWithFallback("pf2craft.combat.can_end", "The combat can be ended.")
                .formatted(Formatting.GOLD);
    }

    static MutableText statusHead(int round, String current, int actionsLeft, boolean reaction) {
        return Text.translatableWithFallback("pf2craft.combat.status_head",
                "Round %s, %s's turn. Actions left: %s, reaction: %s", round, current, actionsLeft,
                Text.translatableWithFallback(reaction ? "pf2craft.combat.yes" : "pf2craft.combat.no",
                        reaction ? "yes" : "no")).formatted(Formatting.GOLD);
    }

    static MutableText notRunning() {
        return Text.translatableWithFallback("pf2craft.combat.not_running", "No combat is running.")
                .formatted(Formatting.RED);
    }

    static void error(ServerPlayerEntity player, MutableText text) {
        player.sendMessage(text.formatted(Formatting.RED), false);
    }

    static MutableText notGm() {
        return Text.translatableWithFallback("pf2craft.combat.not_gm", "Only a game master can do that.");
    }

    static MutableText notYourTurn() {
        return Text.translatableWithFallback("pf2craft.combat.not_your_turn", "It is not your turn.");
    }

    static MutableText noActions() {
        return Text.translatableWithFallback("pf2craft.combat.no_actions", "You have no actions left to move.");
    }

    static MutableText alreadyRunning() {
        return Text.translatableWithFallback("pf2craft.combat.already_running", "A combat is already running.");
    }

    static MutableText noParticipants() {
        return Text.translatableWithFallback("pf2craft.combat.no_participants",
                "Nobody can fight: no characters or NPCs were found.");
    }
}
