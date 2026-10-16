package de.pf2craft.core.dice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A parsed dice expression like {@code 1d20+2d6-1}. Accepts {@code d} as well as the
 * German {@code W} ("Würfel") as die marker, so {@code 2W6+3} works too.
 */
public record DiceExpression(List<Term> terms) {
    public static final int MAX_LENGTH = 100;
    public static final int MAX_TERMS = 20;
    public static final int MAX_DICE = 100;
    public static final int MAX_SIDES = 1000;
    public static final int MAX_CONSTANT = 10_000;

    public sealed interface Term permits Dice, Constant {
    }

    /** {@code count} dice with {@code sides} sides, added ({@code sign = 1}) or subtracted ({@code sign = -1}). */
    public record Dice(int sign, int count, int sides) implements Term {
    }

    /** A flat, signed modifier. */
    public record Constant(int value) implements Term {
    }

    public DiceExpression {
        terms = List.copyOf(terms);
    }

    public static DiceExpression parse(String input) throws DiceParseException {
        if (input == null) {
            throw new DiceParseException("Empty dice expression");
        }
        if (input.matches("(?s).*[0-9dDwW]\\s+[0-9].*")) {
            // "1d20 3" must not silently become "1d203".
            throw new DiceParseException("Unexpected space inside a number");
        }
        String s = input.replaceAll("\\s+", "").toLowerCase(Locale.ROOT).replace('w', 'd');
        if (s.isEmpty()) {
            throw new DiceParseException("Empty dice expression");
        }
        if (s.length() > MAX_LENGTH) {
            throw new DiceParseException("Dice expression is too long (max " + MAX_LENGTH + " characters)");
        }

        List<Term> terms = new ArrayList<>();
        int totalDice = 0;
        int pos = 0;
        while (pos < s.length()) {
            int sign = 1;
            char c = s.charAt(pos);
            if (c == '+' || c == '-') {
                sign = c == '-' ? -1 : 1;
                pos++;
            } else if (!terms.isEmpty()) {
                throw new DiceParseException("Expected '+' or '-' at position " + (pos + 1));
            }

            int start = pos;
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            String number = s.substring(start, pos);

            if (pos < s.length() && s.charAt(pos) == 'd') {
                pos++;
                int sidesStart = pos;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
                if (sidesStart == pos) {
                    throw new DiceParseException("Missing number of sides after 'd' at position " + pos);
                }
                int count = number.isEmpty() ? 1 : parseBounded(number, MAX_DICE, "Too many dice");
                int sides = parseBounded(s.substring(sidesStart, pos), MAX_SIDES, "Die has too many sides");
                if (count < 1) {
                    throw new DiceParseException("Number of dice must be at least 1");
                }
                if (sides < 2) {
                    throw new DiceParseException("A die needs at least 2 sides");
                }
                totalDice += count;
                if (totalDice > MAX_DICE) {
                    throw new DiceParseException("Too many dice (max " + MAX_DICE + ")");
                }
                terms.add(new Dice(sign, count, sides));
            } else {
                if (number.isEmpty()) {
                    throw new DiceParseException("Expected a number or die at position " + (pos + 1));
                }
                terms.add(new Constant(sign * parseBounded(number, MAX_CONSTANT, "Modifier is too large")));
            }

            if (terms.size() > MAX_TERMS) {
                throw new DiceParseException("Too many terms (max " + MAX_TERMS + ")");
            }
        }
        return new DiceExpression(terms);
    }

    private static int parseBounded(String digits, int max, String error) throws DiceParseException {
        // Strip leading zeros first so long zero-padded input can't overflow.
        String trimmed = digits.replaceFirst("^0+(?=\\d)", "");
        if (trimmed.length() > 9 || Integer.parseInt(trimmed) > max) {
            throw new DiceParseException(error + " (max " + max + ")");
        }
        return Integer.parseInt(trimmed);
    }

    public RollResult roll(DieRoller roller) {
        List<RollResult.TermResult> results = new ArrayList<>(terms.size());
        for (Term term : terms) {
            if (term instanceof Dice dice) {
                List<Integer> values = new ArrayList<>(dice.count());
                for (int i = 0; i < dice.count(); i++) {
                    values.add(roller.roll(dice.sides()));
                }
                results.add(new RollResult.DiceRoll(dice, values));
            } else if (term instanceof Constant constant) {
                results.add(new RollResult.ConstantValue(constant.value()));
            }
        }
        return new RollResult(this, results);
    }

    /** Canonical notation, e.g. {@code 1d20+2d6-1}. */
    public String toNotation() {
        StringBuilder sb = new StringBuilder();
        for (Term term : terms) {
            if (term instanceof Dice dice) {
                if (dice.sign() < 0) {
                    sb.append('-');
                } else if (!sb.isEmpty()) {
                    sb.append('+');
                }
                sb.append(dice.count()).append('d').append(dice.sides());
            } else if (term instanceof Constant constant) {
                if (constant.value() < 0) {
                    sb.append(constant.value());
                } else {
                    if (!sb.isEmpty()) {
                        sb.append('+');
                    }
                    sb.append(constant.value());
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toNotation();
    }
}
