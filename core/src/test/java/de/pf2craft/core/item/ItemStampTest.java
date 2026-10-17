package de.pf2craft.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.ArmorClass;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.HitPoints;
import de.pf2craft.core.character.Strike;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ItemStampTest {
    private static Strike scythe(int dice) {
        return new Strike("Lion Scythe", "martial", "d6", "S", dice, Ability.STR, 0, 0,
                "", List.of(), List.of(), 0);
    }

    private static CharacterSheet sheet(int level, List<Strike> strikes, int hp) {
        return new CharacterSheet("August", "Fighter", "Kholo", "", "", level, Ability.STR,
                Map.of(Ability.STR, 18), Map.of(), List.of(), new HitPoints(8, 10, 2, 0), 25,
                new ArmorClass("heavy", OptionalInt.of(0), 6), strikes, List.of(), List.of(), hp,
                false, Optional.empty());
    }

    @Test
    void sameSheetGivesSameStamp() {
        assertEquals(ItemStamp.of(sheet(5, List.of(scythe(1)), 75)),
                ItemStamp.of(sheet(5, List.of(scythe(1)), 75)));
    }

    @Test
    void changedStrikeGivesDifferentStamp() {
        assertNotEquals(ItemStamp.of(sheet(5, List.of(scythe(1)), 75)),
                ItemStamp.of(sheet(5, List.of(scythe(2)), 75)));
        assertNotEquals(ItemStamp.of(sheet(5, List.of(scythe(1)), 75)),
                ItemStamp.of(sheet(5, List.of(), 75)));
    }

    @Test
    void changedLevelGivesDifferentStamp() {
        assertNotEquals(ItemStamp.of(sheet(5, List.of(scythe(1)), 75)),
                ItemStamp.of(sheet(6, List.of(scythe(1)), 75)));
    }

    @Test
    void hpChangeKeepsStamp() {
        CharacterSheet s = sheet(5, List.of(scythe(1)), 75);
        assertEquals(ItemStamp.of(s), ItemStamp.of(s.withCurrentHp(12)));
    }
}
