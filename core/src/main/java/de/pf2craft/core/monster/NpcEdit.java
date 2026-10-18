package de.pf2craft.core.monster;

import de.pf2craft.core.progression.InvalidChoiceException;
import java.util.ArrayList;
import java.util.List;

/** Validated GM edits of an NPC's stats. Values arrive as text and are parsed here. */
public final class NpcEdit {
    private static final int NAME_MAX = 32;

    private NpcEdit() {
    }

    public static NpcStats apply(NpcStats stats, String field, String value) throws InvalidChoiceException {
        MonsterData b = stats.base();
        switch (field) {
            case "hp":
                return new NpcStats(b, stats.displayName(),
                        Math.max(0, Math.min(b.hp(), number(value, Integer.MIN_VALUE, Integer.MAX_VALUE))));
            case "maxhp": {
                int max = number(value, 1, 9999);
                return new NpcStats(with(b, b.level(), b.perception(), b.ac(), b.fortitude(), b.reflex(),
                        b.will(), max, b.speed(), b.strikes()), stats.displayName(),
                        Math.min(stats.currentHp(), max));
            }
            case "ac":
                return edited(stats, with(b, b.level(), b.perception(), number(value, 0, 99), b.fortitude(),
                        b.reflex(), b.will(), b.hp(), b.speed(), b.strikes()));
            case "fortitude":
                return edited(stats, with(b, b.level(), b.perception(), b.ac(), number(value, -10, 99),
                        b.reflex(), b.will(), b.hp(), b.speed(), b.strikes()));
            case "reflex":
                return edited(stats, with(b, b.level(), b.perception(), b.ac(), b.fortitude(),
                        number(value, -10, 99), b.will(), b.hp(), b.speed(), b.strikes()));
            case "will":
                return edited(stats, with(b, b.level(), b.perception(), b.ac(), b.fortitude(), b.reflex(),
                        number(value, -10, 99), b.hp(), b.speed(), b.strikes()));
            case "perception":
                return edited(stats, with(b, b.level(), number(value, -10, 99), b.ac(), b.fortitude(),
                        b.reflex(), b.will(), b.hp(), b.speed(), b.strikes()));
            case "speed": {
                int speed = number(value, 0, 300);
                if (speed % 5 != 0) {
                    throw new InvalidChoiceException("Speed must be a multiple of 5.");
                }
                return edited(stats, with(b, b.level(), b.perception(), b.ac(), b.fortitude(), b.reflex(),
                        b.will(), b.hp(), speed, b.strikes()));
            }
            case "level":
                return edited(stats, with(b, number(value, -1, 25), b.perception(), b.ac(), b.fortitude(),
                        b.reflex(), b.will(), b.hp(), b.speed(), b.strikes()));
            case "name":
                if (value.isEmpty() || value.length() > NAME_MAX) {
                    throw new InvalidChoiceException("Name must be 1 to " + NAME_MAX + " characters.");
                }
                if (value.indexOf('\u00a7') >= 0) {
                    throw new InvalidChoiceException("Name must not contain formatting codes.");
                }
                return new NpcStats(b, value, stats.currentHp());
            default:
                if (field.startsWith("strike:") && field.endsWith(":bonus")) {
                    return strikeBonus(stats, field, value);
                }
                throw new InvalidChoiceException("Unknown field: " + field);
        }
    }

    private static NpcStats strikeBonus(NpcStats stats, String field, String value)
            throws InvalidChoiceException {
        String indexText = field.substring("strike:".length(), field.length() - ":bonus".length());
        int index;
        try {
            index = Integer.parseInt(indexText);
        } catch (NumberFormatException e) {
            throw new InvalidChoiceException("Invalid strike index: " + indexText);
        }
        MonsterData b = stats.base();
        if (index < 0 || index >= b.strikes().size()) {
            throw new InvalidChoiceException("No strike with index " + index + ".");
        }
        int bonus = number(value, -10, 99);
        List<MonsterStrike> strikes = new ArrayList<>(b.strikes());
        MonsterStrike old = strikes.get(index);
        strikes.set(index, new MonsterStrike(old.name(), bonus, old.damage(), old.traits(), old.ranged()));
        return edited(stats, with(b, b.level(), b.perception(), b.ac(), b.fortitude(), b.reflex(), b.will(),
                b.hp(), b.speed(), List.copyOf(strikes)));
    }

    private static NpcStats edited(NpcStats stats, MonsterData base) {
        return new NpcStats(base, stats.displayName(), stats.currentHp());
    }

    private static MonsterData with(MonsterData b, int level, int perception, int ac, int fortitude,
            int reflex, int will, int hp, int speed, List<MonsterStrike> strikes) {
        return new MonsterData(b.id(), b.name(), level, b.size(), b.traits(), perception, ac, fortitude,
                reflex, will, hp, speed, strikes, b.resistances(), b.weaknesses(), b.immunities(),
                b.skills(), b.abilities());
    }

    private static int number(String value, int min, int max) throws InvalidChoiceException {
        int n;
        try {
            n = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidChoiceException("Not a number: " + value);
        }
        if (n < min || n > max) {
            throw new InvalidChoiceException("Value must be between " + min + " and " + max + ".");
        }
        return n;
    }
}
