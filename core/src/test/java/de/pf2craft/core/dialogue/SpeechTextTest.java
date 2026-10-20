package de.pf2craft.core.dialogue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpeechTextTest {
    @Test
    void stripsFormattingCodes() {
        assertEquals("Rot und fett", SpeechText.sanitize("\u00a7cRot und \u00a7lfett"));
    }

    @Test
    void collapsesWhitespaceAndControlCharacters() {
        assertEquals("a b c", SpeechText.sanitize("  a \t\n  b\u0007 c  "));
    }

    @Test
    void emptyAfterCleaningStaysEmpty() {
        assertEquals("", SpeechText.sanitize("\u00a7c"));
        assertEquals("", SpeechText.sanitize("   "));
        assertEquals("", SpeechText.sanitize(null));
    }

    @Test
    void capsTheLength() {
        String out = SpeechText.sanitize("x".repeat(300));
        assertEquals(SpeechText.MAX_LENGTH, out.length());
    }

    @Test
    void trailingSectionSignIsDropped() {
        assertEquals("hi", SpeechText.sanitize("hi\u00a7"));
    }
}
