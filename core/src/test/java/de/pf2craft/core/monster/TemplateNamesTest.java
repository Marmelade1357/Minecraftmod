package de.pf2craft.core.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TemplateNamesTest {
    @Test
    void templateFileNameIsSanitised() {
        assertEquals("evil.json", TemplateNames.fileName("../../evil"));
        assertEquals("kobold-warrior.json", TemplateNames.fileName("Kobold/Warrior"));
        assertEquals("template.json", TemplateNames.fileName("   "));
        assertTrue(TemplateNames.fileName("a".repeat(200)).length() <= 69);
    }
}
