package de.pf2craft.core.dice;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/** The outcome of rolling a {@link DiceExpression}, keeping every single die for display. */
public record RollResult(DiceExpression expression, List<TermResult> terms) {

    public sealed interface TermResult permits DiceRoll, ConstantValue {
        int total();
    }

    public record DiceRoll(DiceExpression.Dice dice, List<Integer> values) implements TermResult {
        public DiceRoll {
            values = List.copyOf(values);
        }

        @Override
        public int total() {
            int sum = 0;
            for (int value : values) {
                sum += value;
            }
            return dice.sign() * sum;
        }
    }

    public record ConstantValue(int value) implements TermResult {
        @Override
        public int total() {
            return value;
        }
    }

    public RollResult {
        terms = List.copyOf(terms);
    }

    public int total() {
        int sum = 0;
        for (TermResult term : terms) {
            sum += term.total();
        }
        return sum;
    }

    /**
     * The natural d20 result if this roll is a check, i.e. it contains exactly one d20
     * that is added. Pathfinder 2e uses it to upgrade (natural 20) or downgrade
     * (natural 1) the degree of success.
     */
    public OptionalInt naturalD20() {
        Integer natural = null;
        for (TermResult term : terms) {
            if (term instanceof DiceRoll roll && roll.dice().sides() == 20) {
                if (natural != null || roll.dice().count() != 1 || roll.dice().sign() < 0) {
                    return OptionalInt.empty();
                }
                natural = roll.values().get(0);
            }
        }
        return natural == null ? OptionalInt.empty() : OptionalInt.of(natural);
    }

    /** Human readable breakdown, e.g. {@code 1d20 (17) + 2d6 (3, 5) + 4}. */
    public String breakdown() {
        StringBuilder sb = new StringBuilder();
        for (TermResult term : terms) {
            boolean negative = term instanceof DiceRoll roll ? roll.dice().sign() < 0
                    : ((ConstantValue) term).value() < 0;
            if (sb.isEmpty()) {
                if (negative) {
                    sb.append('-');
                }
            } else {
                sb.append(negative ? " - " : " + ");
            }
            if (term instanceof DiceRoll roll) {
                sb.append(roll.dice().count()).append('d').append(roll.dice().sides())
                        .append(" (")
                        .append(roll.values().stream().map(String::valueOf).collect(Collectors.joining(", ")))
                        .append(')');
            } else {
                sb.append(Math.abs(((ConstantValue) term).value()));
            }
        }
        return sb.toString();
    }
}
