package de.pf2craft.core.check;

import static de.pf2craft.core.check.DegreeOfSuccess.CRITICAL_FAILURE;
import static de.pf2craft.core.check.DegreeOfSuccess.CRITICAL_SUCCESS;
import static de.pf2craft.core.check.DegreeOfSuccess.FAILURE;
import static de.pf2craft.core.check.DegreeOfSuccess.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class DegreeOfSuccessTest {

    private static DegreeOfSuccess check(int total, int dc, int natural) {
        return DegreeOfSuccess.determine(total, dc, OptionalInt.of(natural));
    }

    @Test
    void comparesAgainstDc() {
        assertEquals(CRITICAL_SUCCESS, check(30, 20, 10));
        assertEquals(SUCCESS, check(29, 20, 10));
        assertEquals(SUCCESS, check(20, 20, 10));
        assertEquals(FAILURE, check(19, 20, 10));
        assertEquals(FAILURE, check(11, 20, 10));
        assertEquals(CRITICAL_FAILURE, check(10, 20, 10));
    }

    @Test
    void naturalTwentyAndOneShiftOneStep() {
        assertEquals(SUCCESS, check(19, 20, 20));
        assertEquals(CRITICAL_SUCCESS, check(35, 20, 20));
        assertEquals(FAILURE, check(25, 20, 1));
        assertEquals(CRITICAL_FAILURE, check(5, 20, 1));
    }

    @Test
    void withoutNaturalNoShift() {
        assertEquals(FAILURE, DegreeOfSuccess.determine(19, 20, OptionalInt.empty()));
    }
}
