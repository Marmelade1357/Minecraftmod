package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class EncounterTest {

    private static Combatant player(String id, int perception) {
        return new Combatant(id, id, false, perception);
    }

    private static Combatant npc(String id, int perception) {
        return new Combatant(id, id, true, perception);
    }

    private static List<String> ids(Encounter e) {
        return e.order().stream().map(x -> x.combatant().id()).toList();
    }

    private static Encounter three() {
        Encounter e = new Encounter();
        e.add(player("a", 0), 20);
        e.add(player("b", 0), 15);
        e.add(player("c", 0), 10);
        e.start();
        return e;
    }

    @Test
    void ordersByInitiativeThenNpcThenPerception() {
        Encounter e = new Encounter();
        e.add(player("A", 5), 15);
        e.add(npc("B", 3), 15);
        e.add(player("C", 8), 15);
        e.add(player("D", 0), 20);
        assertEquals(List.of("D", "B", "C", "A"), ids(e));
    }

    @Test
    void nameBreaksFinalTie() {
        Encounter e = new Encounter();
        e.add(player("z", 5), 10);
        e.add(player("m", 5), 10);
        assertEquals(List.of("m", "z"), ids(e));
    }

    @Test
    void advanceWrapsAndCountsRounds() {
        Encounter e = three();
        assertEquals("a", e.current().orElseThrow().id());
        assertEquals(1, e.round());
        assertEquals("b", e.advance().orElseThrow().id());
        assertEquals("c", e.advance().orElseThrow().id());
        assertEquals(1, e.round());
        assertEquals("a", e.advance().orElseThrow().id());
        assertEquals(2, e.round());
    }

    @Test
    void advanceSkipsInactive() {
        Encounter e = three();
        e.setActive("b", false);
        assertEquals("c", e.advance().orElseThrow().id());
    }

    @Test
    void removeCurrentAdvances() {
        Encounter e = three();
        e.advance();
        e.remove("b");
        assertEquals("c", e.advance().orElseThrow().id());
        assertEquals(List.of("a", "c"), ids(e));
        assertEquals(1, e.round());
    }

    @Test
    void removeLastCurrentWrapsToFirst() {
        Encounter e = three();
        e.advance();
        e.advance();
        e.remove("c");
        assertEquals("a", e.advance().orElseThrow().id());
        assertEquals(2, e.round());
    }

    @Test
    void allInactiveEndsEncounter() {
        Encounter e = three();
        e.setActive("a", false);
        e.setActive("b", false);
        e.setActive("c", false);
        assertTrue(e.advance().isEmpty());
        assertFalse(e.hasActive(false));
    }

    @Test
    void lateAddKeepsCurrent() {
        Encounter e = three();
        e.advance();
        e.add(player("late", 0), 30);
        assertEquals("b", e.current().orElseThrow().id());
        assertEquals("late", ids(e).get(0));
        assertEquals("c", e.advance().orElseThrow().id());
    }

    @Test
    void advanceResetsTurnState() {
        Encounter e = three();
        e.turn().spendActions(2);
        e.turn().recordAttack();
        e.advance();
        assertEquals(3, e.turn().actionsLeft());
        assertEquals(0, e.turn().attacksMade());
        assertTrue(e.turn().reactionAvailable());
    }

    @Test
    void hasActiveDistinguishesSides() {
        Encounter e = new Encounter();
        e.add(player("p", 0), 10);
        e.add(npc("n", 0), 5);
        assertTrue(e.hasActive(true));
        e.setActive("n", false);
        assertFalse(e.hasActive(true));
        assertTrue(e.hasActive(false));
    }

    @Test
    void advanceBeforeStartReturnsFirst() {
        Encounter e = new Encounter();
        e.add(player("a", 0), 20);
        e.add(player("b", 0), 10);
        assertEquals("a", e.advance().orElseThrow().id());
        assertEquals(1, e.round());
        assertEquals(3, e.turn().actionsLeft());
        assertEquals("b", e.advance().orElseThrow().id());
    }

    @Test
    void currentIsEmptyBeforeStart() {
        Encounter e = new Encounter();
        e.add(player("a", 0), 20);
        assertTrue(e.current().isEmpty());
    }

    @Test
    void addsBeforeStartDoNotFreezePointer() {
        Encounter e = new Encounter();
        e.add(player("A", 0), 10);
        assertTrue(e.current().isEmpty());
        e.add(player("B", 0), 20);
        e.start();
        assertEquals("B", e.current().orElseThrow().id());
        e.start();
        assertEquals("B", e.current().orElseThrow().id());
    }

    @Test
    void advanceOnEmptyEncounterIsEmpty() {
        assertTrue(new Encounter().advance().isEmpty());
    }

    @Test
    void duplicateIdThrows() {
        Encounter e = new Encounter();
        e.add(player("a", 0), 10);
        assertThrows(IllegalArgumentException.class, () -> e.add(player("a", 0), 5));
    }

    @Test
    void deactivatedCurrentIsSkippedAndReactivatedPickedUpNextPass() {
        Encounter e = three();
        e.setActive("a", false);
        assertEquals("b", e.advance().orElseThrow().id());
        assertEquals("c", e.advance().orElseThrow().id());
        e.setActive("a", true);
        assertEquals("a", e.advance().orElseThrow().id());
        assertEquals(2, e.round());
    }

    @Test
    void deactivateCurrentThenAdvance() {
        Encounter e = three();
        e.setActive("a", false);
        assertEquals("b", e.advance().orElseThrow().id());
        assertEquals(1, e.round());
    }
}
