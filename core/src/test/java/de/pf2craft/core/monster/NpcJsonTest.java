package de.pf2craft.core.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.pf2craft.core.progression.InvalidChoiceException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NpcJsonTest {
    @Test
    void editedStatsRoundTrip() throws InvalidChoiceException {
        NpcStats s = NpcEdit.apply(NpcStats.fresh(NpcTestData.kobold()), "name", "Kobold 2");
        s = NpcEdit.apply(s, "hp", "3");
        s = NpcEdit.apply(s, "strike:0:bonus", "11");
        assertEquals(s, NpcJson.read(NpcJson.write(s)));
    }

    @Test
    void templateIsReadableByMonsterParser() {
        MonsterData base = NpcTestData.kobold();
        assertEquals(List.of(base), MonsterParser.parse(NpcJson.writeTemplate(base)));
    }

    @Test
    void invalidJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> NpcJson.read("{}"));
        assertThrows(IllegalArgumentException.class, () -> NpcJson.read("not json"));
    }
}
