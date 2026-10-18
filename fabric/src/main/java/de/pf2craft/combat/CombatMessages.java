package de.pf2craft.combat;

import de.pf2craft.core.check.DegreeOfSuccess;
import de.pf2craft.core.combat.AppliedPart;
import java.util.List;
import java.util.stream.Collectors;
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

    static MutableText canEndPlayers() {
        return Text.translatableWithFallback("pf2craft.combat.can_end_players",
                "All players are unable to fight. The combat can be ended.").formatted(Formatting.GOLD);
    }

    static MutableText attackCooldown() {
        return Text.translatableWithFallback("pf2craft.combat.attack_cooldown", "Not so fast!");
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

    // ---- NPC turns -------------------------------------------------------------------------------------------

    static MutableText npcNotCurrent() {
        return Text.translatableWithFallback("pf2craft.combat.npc_not_current",
                "The selected NPC is not up in the combat.");
    }

    static MutableText noPath() {
        return Text.translatableWithFallback("pf2craft.combat.no_path", "The NPC cannot find a path there.");
    }

    static MutableText notEnoughActions() {
        return Text.translatableWithFallback("pf2craft.combat.not_enough_actions",
                "Not enough actions left to walk that far.");
    }

    static MutableText pathPartial() {
        return Text.translatableWithFallback("pf2craft.combat.path_partial",
                "The NPC cannot get all the way there and will stop short.").formatted(Formatting.YELLOW);
    }

    static MutableText noStrikes() {
        return Text.translatableWithFallback("pf2craft.combat.no_strikes", "This NPC has no strikes.");
    }

    static MutableText npcNoActions() {
        return Text.translatableWithFallback("pf2craft.combat.attack.npc_no_actions",
                "The NPC has no actions left to attack.");
    }

    static MutableText badNpcTarget() {
        return Text.translatableWithFallback("pf2craft.combat.attack.bad_npc_target",
                "The target must be a fighting character or a standing NPC.");
    }

    static MutableText badStrike() {
        return Text.translatableWithFallback("pf2craft.combat.attack.bad_strike", "The NPC has no such strike.");
    }

    static MutableText noLookTarget() {
        return Text.translatableWithFallback("pf2craft.combat.attack.no_look_target",
                "No character or NPC in view (within 16 blocks).");
    }

    // ---- attacks ---------------------------------------------------------------------------------------------

    static MutableText noWeapon() {
        return Text.translatableWithFallback("pf2craft.combat.attack.no_weapon",
                "Hold your character's weapon in your main hand.");
    }

    static MutableText noAttackActions() {
        return Text.translatableWithFallback("pf2craft.combat.attack.no_actions",
                "You have no actions left to attack.");
    }

    static MutableText badTarget() {
        return Text.translatableWithFallback("pf2craft.combat.attack.bad_target",
                "You can only attack NPCs that are still standing.");
    }

    static MutableText tooFar() {
        return Text.translatableWithFallback("pf2craft.combat.attack.too_far", "The target is too far away.");
    }

    static MutableText noTarget() {
        return Text.translatableWithFallback("pf2craft.combat.attack.no_target",
                "No NPC in view (within 16 blocks).");
    }

    /** The line everyone sees: no AC, no roll total, no hit points. {@code damage < 0} means a miss. */
    static MutableText attackLine(String attacker, String target, int number, DegreeOfSuccess degree, int damage) {
        MutableText outcome = switch (degree) {
            case CRITICAL_SUCCESS -> Text.translatableWithFallback("pf2craft.combat.attack.crit_hit",
                    "Hit! (critical)");
            case SUCCESS -> Text.translatableWithFallback("pf2craft.combat.attack.hit", "Hit!");
            case FAILURE -> Text.translatableWithFallback("pf2craft.combat.attack.miss", "Miss!");
            case CRITICAL_FAILURE -> Text.translatableWithFallback("pf2craft.combat.attack.crit_miss",
                    "Critical miss!");
        };
        if (damage >= 0) {
            outcome.append(damage == 0
                    ? Text.translatableWithFallback("pf2craft.combat.attack.no_damage", " - no damage")
                    : Text.translatableWithFallback("pf2craft.combat.attack.damage", " - %s damage", damage));
        }
        Formatting color = switch (degree) {
            case CRITICAL_SUCCESS -> Formatting.GOLD;
            case SUCCESS -> Formatting.GREEN;
            case FAILURE -> Formatting.GRAY;
            case CRITICAL_FAILURE -> Formatting.RED;
        };
        return Text.translatableWithFallback("pf2craft.combat.attack.line", "%s attacks %s (attack %s): %s",
                attacker, target, number, outcome).formatted(color);
    }

    /** GM-only detail lines: the numbers the players must not see. */
    static List<Text> attackDetail(int natural, int total, int ac, int rangePenalty, List<AppliedPart> parts,
                                   List<String> unmatched, int hpLeft, int hpMax) {
        List<Text> lines = new java.util.ArrayList<>();
        lines.add(Text.translatableWithFallback("pf2craft.combat.attack.gm_roll",
                "[GM] d20 %s, total %s against AC %s (range penalty %s)", natural, total, ac, rangePenalty)
                .formatted(Formatting.DARK_AQUA));
        if (!parts.isEmpty()) {
            String list = parts.stream().map(p -> p.type() + " " + p.rolled() + " -> " + p.applied())
                    .collect(Collectors.joining(", "));
            lines.add(Text.translatableWithFallback("pf2craft.combat.attack.gm_damage", "[GM] Damage: %s", list)
                    .formatted(Formatting.DARK_AQUA));
        }
        if (!unmatched.isEmpty()) {
            lines.add(Text.translatableWithFallback("pf2craft.combat.attack.gm_unmatched",
                    "[GM] Note: %s not taken into account", String.join(", ", unmatched))
                    .formatted(Formatting.DARK_AQUA));
        }
        lines.add(Text.translatableWithFallback("pf2craft.combat.attack.gm_hp", "[GM] Target has %s/%s HP left",
                hpLeft, hpMax).formatted(Formatting.DARK_AQUA));
        return lines;
    }

    static MutableText attackNumberLow() {
        return Text.translatableWithFallback("pf2craft.combat.attack_number_low",
                "You cannot choose a lower attack number than the attacks you have already made.");
    }

    static MutableText attackError(String detail) {
        return Text.translatableWithFallback("pf2craft.combat.attack_error",
                "The attack could not be resolved: %s", detail);
    }
}
