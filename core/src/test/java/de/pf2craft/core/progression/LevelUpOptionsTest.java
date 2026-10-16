package de.pf2craft.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelUpOptionsTest {
    @Test
    void jsonRoundTrip() {
        LevelUpOptions o = new LevelUpOptions(7, true, 0, List.of("Class Feat", "General Feat"),
                List.of("Weapon Specialization", "Battlefield Surveyor"), 13);
        assertEquals(o, LevelUpOptions.fromJson(o.toJson()));
    }

    @Test
    void jsonRoundTripEmptyLists() {
        LevelUpOptions o = new LevelUpOptions(10, false, 4, List.of(), List.of(), 9);
        assertEquals(o, LevelUpOptions.fromJson(o.toJson()));
    }

    @Test
    void invalidJsonThrows() {
        assertThrows(IllegalArgumentException.class, () -> LevelUpOptions.fromJson("{}"));
        assertThrows(IllegalArgumentException.class, () -> LevelUpOptions.fromJson("not json"));
    }
}
