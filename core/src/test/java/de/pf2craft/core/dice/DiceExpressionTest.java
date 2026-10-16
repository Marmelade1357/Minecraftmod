package de.pf2craft.core.dice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiceExpressionTest {

    @Test
    void parsesDiceAndModifiers() throws DiceParseException {
        DiceExpression expr = DiceExpression.parse("1d20 + 2d6 - 1d4 + 3 - 1");
        assertEquals(List.of(
                new DiceExpression.Dice(1, 1, 20),
                new DiceExpression.Dice(1, 2, 6),
                new DiceExpression.Dice(-1, 1, 4),
                new DiceExpression.Constant(3),
                new DiceExpression.Constant(-1)), expr.terms());
        assertEquals("1d20+2d6-1d4+3-1", expr.toNotation());
    }

    @Test
    void acceptsImplicitCountAndGermanW() throws DiceParseException {
        assertEquals("1d20+2d6+5", DiceExpression.parse("d20+2W6+5").toNotation());
        assertEquals("-1d4+2", DiceExpression.parse("-d4+2").toNotation());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "d", "2d", "1d20+", "1d20 3", "abc", "1d1", "0d6",
            "101d6", "1d1001", "1d20+10001", "60d6+60d6", "99999999999999d6", "1d20++2"})
    void rejectsInvalidExpressions(String input) {
        assertThrows(DiceParseException.class, () -> DiceExpression.parse(input));
    }

    @Test
    void rollsWithGivenRoller() throws DiceParseException {
        RollResult result = DiceExpression.parse("1d20+2d6-1d4+3").roll(sides -> sides / 2);
        // 10 + (3 + 3) - 2 + 3
        assertEquals(17, result.total());
        assertEquals("1d20 (10) + 2d6 (3, 3) - 1d4 (2) + 3", result.breakdown());
        assertEquals(OptionalInt.of(10), result.naturalD20());
    }

    @Test
    void naturalD20OnlyForSingleAddedD20() throws DiceParseException {
        DieRoller twenty = sides -> 20;
        assertTrue(DiceExpression.parse("2d20").roll(twenty).naturalD20().isEmpty());
        assertTrue(DiceExpression.parse("1d20+1d20").roll(twenty).naturalD20().isEmpty());
        assertTrue(DiceExpression.parse("3-1d20").roll(twenty).naturalD20().isEmpty());
        assertTrue(DiceExpression.parse("2d6").roll(twenty).naturalD20().isEmpty());
    }

    @Test
    void randomRollsStayInRange() throws DiceParseException {
        DieRoller roller = DieRoller.random(new SplittableRandom(42));
        DiceExpression expr = DiceExpression.parse("3d6");
        for (int i = 0; i < 1000; i++) {
            int total = expr.roll(roller).total();
            assertTrue(total >= 3 && total <= 18, "out of range: " + total);
        }
    }
}
