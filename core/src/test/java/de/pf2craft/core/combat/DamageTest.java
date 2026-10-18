package de.pf2craft.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderImporter;
import de.pf2craft.core.character.Strike;
import de.pf2craft.core.dice.DieRoller;
import de.pf2craft.core.item.TestCatalog;
import de.pf2craft.core.monster.DamagePart;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.MonsterParser;
import de.pf2craft.core.monster.Resistance;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DamageTest {
    private static Defenses def(List<Resistance> res, List<Resistance> weak, List<String> imm) {
        return new Defenses(res, weak, imm);
    }

    private static AppliedPart part(DamageOutcome o, String type) {
        return o.parts().stream().filter(p -> p.type().equals(type)).findFirst().orElseThrow();
    }

    @Test
    void fireResistanceOnlyReducesFire() {
        DamageOutcome o = DamageResolver.resolve(
                List.of(new DamageInstance("slashing", 6), new DamageInstance("fire", 4)),
                def(List.of(new Resistance("fire", 5)), List.of(), List.of()));
        assertEquals(6, o.total());
        assertEquals(0, part(o, "fire").applied());
        assertEquals(4, part(o, "fire").rolled());
    }

    @Test
    void weaknessAddsThenResistanceSubtracts() {
        Defenses d = def(List.of(new Resistance("fire", 3)), List.of(new Resistance("fire", 5)), List.of());
        assertEquals(6, DamageResolver.resolve(List.of(new DamageInstance("fire", 4)), d).total());
        assertEquals(0, DamageResolver.resolve(List.of(new DamageInstance("fire", 0)), d).total());
    }

    @Test
    void immunityZeroes() {
        DamageOutcome o = DamageResolver.resolve(List.of(new DamageInstance("fire", 7)),
                def(List.of(), List.of(), List.of("fire")));
        assertEquals(0, o.total());
    }

    @Test
    void physicalAndAllDamageMatch() {
        assertEquals(3, DamageResolver.resolve(List.of(new DamageInstance("slashing", 8)),
                def(List.of(new Resistance("physical", 5)), List.of(), List.of())).total());
        assertEquals(0, DamageResolver.resolve(List.of(new DamageInstance("piercing", 1)),
                def(List.of(new Resistance("all-damage", 2)), List.of(), List.of())).total());
        assertEquals(5, DamageResolver.resolve(List.of(new DamageInstance("cold", 2)),
                def(List.of(), List.of(new Resistance("energy", 3)), List.of())).total());
    }

    @Test
    void unmatchedTypesAreReported() {
        DamageOutcome o = DamageResolver.resolve(List.of(new DamageInstance("slashing", 5)),
                def(List.of(new Resistance("cold-iron", 5)), List.of(), List.of("critical-hits")));
        assertEquals(5, o.total());
        assertEquals(List.of("cold-iron", "critical-hits"), o.unmatched());
    }

    @Test
    void criticalDoublesEachInstance() {
        DieRoller three = sides -> 3;
        List<DamageDice> parts = List.of(new DamageDice("1d6", 2, "slashing"));
        assertEquals(10, DamageRoller.roll(parts, true, three).get(0).amount());
        assertEquals(5, DamageRoller.roll(parts, false, three).get(0).amount());
        assertEquals("slashing", DamageRoller.roll(parts, false, three).get(0).type());
    }

    @Test
    void strikeDamageFromSheet() throws Exception {
        CharacterSheet fighter = PathbuilderImporter.parse(fixture("422753-fighter5.json"));
        DamageDice main = StrikeDamage.forStrike(fighter, 0).get(0);
        assertEquals("1d6", main.dice());
        assertEquals(4, main.bonus());
        assertEquals("slashing", main.type());

        CharacterSheet magus = PathbuilderImporter.parse(fixture("315329-magus9.json"), TestCatalog.shipped());
        DamageDice rapier = StrikeDamage.forStrike(magus, 0).get(0);
        assertEquals("2d6", rapier.dice());
        assertEquals("piercing", rapier.type());

        Strike s = fighter.strikes().get(0);
        Strike flaming = new Strike(s.name(), s.category(), s.damageDie(), s.damageType(), s.damageDice(),
                s.attackAbility(), s.otherAttackBonus(), s.otherDamageBonus(), s.group(), s.traits(),
                List.of("flaming (greater)"), s.potency());
        CharacterSheet withRune = new CharacterSheet(fighter.name(), fighter.className(), fighter.ancestry(),
                fighter.heritage(), fighter.background(), fighter.level(), fighter.keyAbility(),
                fighter.abilityScores(), fighter.proficiencies(), fighter.lores(), fighter.hitPoints(),
                fighter.speed(), fighter.armorClass(), List.of(flaming), fighter.feats(), fighter.specials(),
                fighter.currentHp(), fighter.levelUpGranted(), fighter.shield());
        List<DamageDice> parts = StrikeDamage.forStrike(withRune, 0);
        assertEquals(2, parts.size());
        assertEquals(new DamageDice("1d6", 0, "fire"), parts.get(1));
    }

    @Test
    void monsterStrikeDamage() throws Exception {
        List<MonsterData> all = MonsterParser.parse(new String(
                DamageTest.class.getResourceAsStream("/pf2craft/monsters/monster-core.json").readAllBytes(),
                StandardCharsets.UTF_8));
        MonsterData kobold = all.stream().filter(m -> m.id().equals("kobold-warrior")).findFirst().orElseThrow();
        List<DamagePart> expected = kobold.strikes().get(0).damage();
        List<DamageDice> actual = StrikeDamage.forMonster(kobold.strikes().get(0));
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(new DamageDice(expected.get(i).dice(), expected.get(i).bonus(), expected.get(i).type()),
                    actual.get(i));
        }
    }

    private static String fixture(String name) throws Exception {
        try (InputStream in = DamageTest.class.getResourceAsStream("/pathbuilder/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
