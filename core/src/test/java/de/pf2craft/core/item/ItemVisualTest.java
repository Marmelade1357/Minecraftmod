package de.pf2craft.core.item;

import static de.pf2craft.core.item.ItemVisual.Material.DIAMOND;
import static de.pf2craft.core.item.ItemVisual.Material.GOLD;
import static de.pf2craft.core.item.ItemVisual.Material.NETHERITE;
import static de.pf2craft.core.item.ItemVisual.Material.WOOD;
import static de.pf2craft.core.item.ItemVisual.Shape.AXE;
import static de.pf2craft.core.item.ItemVisual.Shape.BOW;
import static de.pf2craft.core.item.ItemVisual.Shape.STICK;
import static de.pf2craft.core.item.ItemVisual.Shape.SWORD;
import static de.pf2craft.core.item.ItemVisual.Shape.TRIDENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ItemVisualTest {
    @Test
    void mapsShapeAndMaterial() {
        assertEquals(new ItemVisual(SWORD, DIAMOND), ItemVisual.forWeapon("d8", "sword", 0));
        assertEquals(new ItemVisual(SWORD, WOOD), ItemVisual.forWeapon("d4", "knife", 0));
        assertEquals(new ItemVisual(AXE, NETHERITE), ItemVisual.forWeapon("d12", "axe", 0));
        assertEquals(new ItemVisual(BOW, GOLD), ItemVisual.forWeapon("d6", "bow", 60));
        assertEquals(new ItemVisual(TRIDENT, NETHERITE), ItemVisual.forWeapon("d10", "polearm", 0));
        assertEquals(new ItemVisual(STICK, GOLD), ItemVisual.forWeapon("d6", "club", 0));
        assertEquals(new ItemVisual(BOW, WOOD), ItemVisual.forWeapon("d4", "dart", 20));
        assertEquals(new ItemVisual(SWORD, WOOD), ItemVisual.forWeapon("d7", "whatever", 0));
    }

    @Test
    void strikingWords() {
        assertEquals("", RuneNames.strikingWord(1));
        assertEquals("Striking", RuneNames.strikingWord(2));
        assertEquals("Greater Striking", RuneNames.strikingWord(3));
        assertEquals("Major Striking", RuneNames.strikingWord(4));
    }
}
