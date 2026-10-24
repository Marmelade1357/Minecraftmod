package de.pf2craft.core.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpellCatalogParserTest {
    private static final String VALID = "{\"id\":\"a\",\"name\":\"Alpha\",\"rank\":1,\"actions\":2,\"rangeFeet\":30,"
            + "\"maxTargets\":1,\"kind\":\"SAVE\",\"save\":\"reflex\",\"basicSave\":true,\"damageDice\":\"2d6\","
            + "\"damageType\":\"fire\",\"heightenDice\":\"2d6\",\"heightenStep\":1}";

    private static SpellCatalogParser.ParseResult parse(String... entries) {
        return SpellCatalogParser.parse("[" + String.join(",", entries) + "]");
    }

    @Test
    void acceptsValidEntry() {
        var r = parse(VALID);
        assertEquals(List.of(), r.errors());
        SpellDefinition d = r.catalog().byName("ALPHA").orElseThrow();
        assertEquals("2d6", d.damageDice());
        assertEquals(SpellKind.SAVE, d.kind());
        assertTrue(r.catalog().byName("a").isPresent());
    }

    @Test
    void rejectsBadEntriesButKeepsGoodOnes() {
        var r = parse(VALID,
                "{\"name\":\"NoId\"}",
                "{\"id\":\"b\"}",
                "{\"id\":\"c\",\"name\":\"C\",\"actions\":4}",
                "{\"id\":\"d\",\"name\":\"D\",\"actions\":0}",
                "{\"id\":\"e\",\"name\":\"E\",\"damageDice\":\"two dice\",\"damageType\":\"fire\"}",
                "{\"id\":\"f\",\"name\":\"F\",\"damageDice\":\"1d6\",\"damageType\":\"nonsense\"}",
                "{\"id\":\"g\",\"name\":\"G\",\"save\":\"luck\"}");
        assertEquals(1, r.catalog().all().size());
        assertEquals(7, r.errors().size());
    }

    @Test
    void invalidJsonGivesError() {
        assertEquals(1, SpellCatalogParser.parse("not json").errors().size());
    }

    @Test
    void mergedLaterWinsById() {
        var base = parse(VALID).catalog();
        var over = parse(VALID.replace("Alpha", "Beta"), VALID.replace("\"a\"", "\"z\"").replace("Alpha", "Zed")).catalog();
        var m = base.merged(over);
        assertEquals(2, m.all().size());
        assertEquals("Beta", m.byName("a").orElseThrow().name());
    }
}
