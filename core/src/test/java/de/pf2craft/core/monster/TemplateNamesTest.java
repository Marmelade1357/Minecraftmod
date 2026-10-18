package de.pf2craft.core.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TemplateNamesTest {
    @Test
    void templateFileNameIsSanitised() {
        assertEquals("evil.json", TemplateNames.fileName("../../evil"));
        assertEquals("kobold-warrior.json", TemplateNames.fileName("Kobold/Warrior"));
        assertEquals("template.json", TemplateNames.fileName("   "));
        assertTrue(TemplateNames.fileName("a".repeat(200)).length() <= 69);
    }

    @Test
    void windowsReservedNamesAreRenamed() {
        assertEquals("template-con.json", TemplateNames.fileName("Con"));
        assertEquals("template-nul.json", TemplateNames.fileName("nul"));
        assertEquals("template-com1.json", TemplateNames.fileName("COM1"));
        assertEquals("template-lpt9.json", TemplateNames.fileName("lpt9"));
        assertEquals("console.json", TemplateNames.fileName("console"));
        assertEquals("com10.json", TemplateNames.fileName("com10"));
    }

    @Test
    void freeIdKeepsFreeBase() {
        assertEquals("goblin-boss", TemplateNames.freeId("Goblin Boss", id -> false));
    }

    @Test
    void freeIdAppendsCustomOnCollision() {
        assertEquals("goblin-warrior-custom", TemplateNames.freeId("Goblin Warrior", Set.of("goblin-warrior")::contains));
        assertEquals("goblin-warrior-custom-2", TemplateNames.freeId("Goblin Warrior",
                Set.of("goblin-warrior", "goblin-warrior-custom")::contains));
        assertEquals("goblin-warrior-custom-3", TemplateNames.freeId("Goblin Warrior",
                Set.of("goblin-warrior", "goblin-warrior-custom", "goblin-warrior-custom-2")::contains));
    }

    @Test
    void freeIdRespectsLengthCap() {
        String base = "a".repeat(64);
        String id = TemplateNames.freeId(base, Set.of(base)::contains);
        assertTrue(id.length() <= 64);
        assertTrue(id.endsWith("-custom"));
        String second = TemplateNames.freeId(base, Set.of(base, id)::contains);
        assertTrue(second.length() <= 64);
        assertTrue(second.endsWith("-custom-2"));
    }
}
