package de.pf2craft.core.combat;

import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Save;
import de.pf2craft.core.character.Skill;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.MonsterStrike;
import java.util.Locale;
import java.util.OptionalInt;

/** Skill bonuses and target DCs for basic actions. */
public final class ActionDcs {
    private ActionDcs() {
    }

    public static int skillBonus(CharacterSheet sheet, String skill) {
        if (skill.equals("perception")) {
            return CharacterMath.perception(sheet);
        }
        for (Skill s : Skill.values()) {
            if (s.id().equals(skill)) {
                return CharacterMath.skill(sheet, s);
            }
        }
        return 0;
    }

    /**
     * Monster skill bonus. The catalog often lists no skills, so we fall back (plan decision):
     * perception, athletics = best strike bonus - 2 (level + 4 without strikes), intimidation = perception,
     * anything else = level + 4.
     */
    public static int skillBonus(MonsterData m, String skill) {
        Integer listed = m.skills().get(skill);
        if (listed != null) {
            return listed;
        }
        switch (skill) {
            case "perception":
            case "intimidation":
                return m.perception();
            case "athletics":
                return m.strikes().stream().mapToInt(MonsterStrike::attackBonus).max()
                        .orElse(m.level() + 6) - 2;
            default:
                return m.level() + 4;
        }
    }

    public static OptionalInt defenseDc(CharacterSheet target, String defense) {
        return switch (defense) {
            case "fortitude" -> OptionalInt.of(10 + CharacterMath.save(target, Save.FORTITUDE));
            case "reflex" -> OptionalInt.of(10 + CharacterMath.save(target, Save.REFLEX));
            case "will" -> OptionalInt.of(10 + CharacterMath.save(target, Save.WILL));
            case "perception_dc" -> OptionalInt.of(10 + CharacterMath.perception(target));
            default -> OptionalInt.empty();
        };
    }

    public static OptionalInt defenseDc(MonsterData target, String defense) {
        return switch (defense) {
            case "fortitude" -> OptionalInt.of(10 + target.fortitude());
            case "reflex" -> OptionalInt.of(10 + target.reflex());
            case "will" -> OptionalInt.of(10 + target.will());
            case "perception_dc" -> OptionalInt.of(10 + target.perception());
            default -> OptionalInt.empty();
        };
    }

    /** tiny 0 ... gargantuan 5; unknown sizes count as medium. */
    public static int sizeRank(String size) {
        return switch (size.toLowerCase(Locale.ROOT)) {
            case "tiny" -> 0;
            case "small" -> 1;
            case "large" -> 3;
            case "huge" -> 4;
            case "gargantuan" -> 5;
            default -> 2;
        };
    }
}
