package de.pf2craft.spell;

import de.pf2craft.core.check.DegreeOfSuccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Chat texts of spell casting (hints and errors in red/yellow, results public, numbers for GMs only). */
final class SpellMessages {
    private SpellMessages() {
    }

    private static MutableText t(String key, String english, Object... args) {
        return Text.translatableWithFallback("pf2craft.spell." + key, english, args);
    }

    // ---- errors ----------------------------------------------------------------------------------------------

    static MutableText noCharacter() {
        return t("error.no_character", "You have no character.");
    }

    static MutableText noEntry() {
        return t("error.no_entry", "You have no such spellcasting entry.");
    }

    static MutableText notInCatalog() {
        return t("error.not_in_catalog", "That spell is not in the spell catalog. Ask the GM.");
    }

    static MutableText notInEntry() {
        return t("error.not_in_entry", "That spell is not in this spellcasting entry.");
    }

    static MutableText badRank() {
        return t("error.bad_rank", "You cannot cast that spell at this rank.");
    }

    static MutableText noSlot() {
        return t("error.no_slot", "No spell slot of that rank left.");
    }

    static MutableText noFocus() {
        return t("error.no_focus", "No focus points left.");
    }

    static MutableText innateUnsupported() {
        return t("error.innate", "Innate spells cannot be cast yet.");
    }

    static MutableText badTarget() {
        return t("error.bad_target", "That is not a valid target for this spell.");
    }

    static MutableText selfNotAllowed() {
        return t("error.self_target", "You cannot target yourself with this spell.");
    }

    static MutableText duplicateTarget() {
        return t("error.duplicate_target", "That creature is already targeted.");
    }

    static MutableText confirmNone() {
        return t("error.confirm_none", "Choose at least one target first.");
    }

    static MutableText noPending() {
        return t("error.no_pending", "No spell is waiting for targets.");
    }

    static MutableText failed(String detail) {
        return t("error.failed", "The spell could not be resolved: %s", detail);
    }

    // ---- pending ---------------------------------------------------------------------------------------------

    static MutableText pendingSingle(String spell) {
        return t("pending.single", "%s: click the target.", spell).formatted(Formatting.YELLOW);
    }

    static MutableText pendingMulti(String spell, int chosen, int max) {
        return t("pending.multi", "%s: click targets (%s/%s), then confirm in the spell menu.", spell, chosen, max)
                .formatted(Formatting.YELLOW);
    }

    // ---- results ---------------------------------------------------------------------------------------------

    static MutableText say(String caster, String spell, int rank) {
        return rank > 0
                ? t("say.ranked", "%s casts %s (rank %s).", caster, spell, rank).formatted(Formatting.LIGHT_PURPLE)
                : t("say", "%s casts %s.", caster, spell).formatted(Formatting.LIGHT_PURPLE);
    }

    /** "Target: outcome - N damage"; damage &lt; 0 means no damage line at all. */
    static MutableText targetLine(String target, MutableText outcome, int damage) {
        MutableText line = t("target", "  %s: %s", target, outcome);
        if (damage >= 0) {
            line.append(damage == 0
                    ? Text.translatableWithFallback("pf2craft.combat.attack.no_damage", " - no damage")
                    : Text.translatableWithFallback("pf2craft.combat.attack.damage", " - %s damage", damage));
        }
        return line;
    }

    static MutableText attackOutcome(DegreeOfSuccess degree) {
        return switch (degree) {
            case CRITICAL_SUCCESS -> Text.translatableWithFallback("pf2craft.combat.attack.crit_hit",
                    "Hit! (critical)").formatted(Formatting.GREEN);
            case SUCCESS -> Text.translatableWithFallback("pf2craft.combat.attack.hit", "Hit!")
                    .formatted(Formatting.GREEN);
            case FAILURE -> Text.translatableWithFallback("pf2craft.combat.attack.miss", "Miss!")
                    .formatted(Formatting.RED);
            case CRITICAL_FAILURE -> Text.translatableWithFallback("pf2craft.combat.attack.crit_miss",
                    "Critical miss!").formatted(Formatting.RED);
        };
    }

    static MutableText autoOutcome() {
        return Text.translatableWithFallback("pf2craft.combat.attack.hit", "Hit!").formatted(Formatting.GREEN);
    }

    static MutableText saveOutcome(DegreeOfSuccess degree) {
        return switch (degree) {
            case CRITICAL_SUCCESS -> t("save.crit_success", "critical success on the save");
            case SUCCESS -> t("save.success", "success on the save");
            case FAILURE -> t("save.failure", "failed the save");
            case CRITICAL_FAILURE -> t("save.crit_failure", "critically failed the save");
        };
    }

    static MutableText healLine(String target, int amount) {
        return t("heal.line", "  %s recovers %s hit points.", target, amount).formatted(Formatting.GREEN);
    }

    static MutableText healLineHidden(String target) {
        return t("heal.line_hidden", "  %s is healed.", target).formatted(Formatting.GREEN);
    }

    static MutableText utilityLine() {
        return t("utility", "  The spell takes effect (the GM decides what happens).")
                .formatted(Formatting.GRAY);
    }

    // ---- GM details ------------------------------------------------------------------------------------------

    static MutableText gmAttack(String target, int natural, int total, int ac, int map) {
        return t("gm.attack", "[GM] %s: spell attack d20 %s, total %s against AC %s (multiple attack penalty %s)",
                target, natural, total, ac, map).formatted(Formatting.DARK_AQUA);
    }

    static MutableText gmSave(String target, String save, int natural, int total, int dc) {
        return t("gm.save", "[GM] %s: %s save d20 %s, total %s against DC %s", target, save, natural, total, dc)
                .formatted(Formatting.DARK_AQUA);
    }

    static MutableText gmDamage(String parts) {
        return t("gm.damage", "[GM] Damage: %s", parts).formatted(Formatting.DARK_AQUA);
    }

    static MutableText gmHp(String target, int hp, int max) {
        return t("gm.hp", "[GM] %s: %s/%s hit points", target, hp, max).formatted(Formatting.DARK_AQUA);
    }

    static MutableText gmHeal(String target, int amount, int hp, int max) {
        return t("gm.heal", "[GM] %s heals %s (%s/%s hit points)", target, amount, hp, max)
                .formatted(Formatting.DARK_AQUA);
    }

    static MutableText gmNumbers(int attack, int dc) {
        return t("gm.numbers", "[GM] Spell attack %s, spell DC %s", attack, dc).formatted(Formatting.DARK_AQUA);
    }
}
