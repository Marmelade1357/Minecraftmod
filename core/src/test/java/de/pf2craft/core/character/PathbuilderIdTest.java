package de.pf2craft.core.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PathbuilderIdTest {
    @Test
    void acceptsDigits() {
        assertEquals(Optional.of("422753"), PathbuilderId.parse("422753"));
    }

    @Test
    void trims() {
        assertEquals(Optional.of("422753"), PathbuilderId.parse(" 422753 "));
    }

    @Test
    void rejectsInvalid() {
        for (String s : new String[] {"", "abc", "12 34", "12345678901", "-5", "１２３"}) {
            assertTrue(PathbuilderId.parse(s).isEmpty(), s);
        }
        assertTrue(PathbuilderId.parse(null).isEmpty());
    }

    @Test
    void acceptsTenDigits() {
        assertEquals(Optional.of("1234567890"), PathbuilderId.parse("1234567890"));
    }
}
