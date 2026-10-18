package de.pf2craft.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderImporter;
import de.pf2craft.core.character.Rank;
import de.pf2craft.core.item.ItemStamp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LevelUpTest {
    private static ClassRegistry registry() throws IOException {
        return new ClassRegistry(List.of(ClassProgressionTest.fighter()));
    }

    private static CharacterSheet fighter5() throws Exception {
        String json = Files.readString(Paths.get(LevelUpTest.class
                .getResource("/pathbuilder/422753-fighter5.json").toURI()), StandardCharsets.UTF_8);
        return PathbuilderImporter.parse(json).withLevelUpGranted(true);
    }

    private static CharacterSheet rebuilt(CharacterSheet s, String className, int level) {
        return new CharacterSheet(s.name(), className, s.ancestry(), s.heritage(), s.background(),
                level, s.keyAbility(), s.abilityScores(), s.proficiencies(), s.lores(),
                s.hitPoints(), s.speed(), s.armorClass(), s.strikes(), s.feats(), s.specials(),
                s.currentHp(), s.levelUpGranted(), s.shield());
    }

    private static LevelUpChoices feats(String... names) {
        return new LevelUpChoices(Optional.empty(), Set.of(), List.of(names));
    }

    @Test
    void optionsForLevel6() throws Exception {
        LevelUpOptions o = LevelUp.options(fighter5(), registry());
        assertEquals(6, o.newLevel());
        assertFalse(o.skillIncrease());
        assertEquals(0, o.abilityBoosts());
        assertEquals(List.of("Class Feat", "Skill Feat"), o.featSlots());
        assertEquals(13, o.hpGain());
    }

    @Test
    void levelUpKeepsItemStamp() throws Exception {
        CharacterSheet s5 = fighter5();
        CharacterSheet s6 = LevelUp.apply(s5, registry(), feats("A", "B"));
        assertEquals(6, s6.level());
        assertEquals(ItemStamp.of(s5), ItemStamp.of(s6));
    }

    @Test
    void levelSevenRaisesPerception() throws Exception {
        CharacterSheet s6 = LevelUp.apply(fighter5(), registry(), feats("A", "B"));
        assertEquals(6, s6.level());
        assertFalse(s6.levelUpGranted());
        CharacterSheet s7 = LevelUp.apply(s6.withLevelUpGranted(true), registry(),
                new LevelUpChoices(Optional.of("athletics"), Set.of(), List.of("C")));
        assertEquals(7, s7.level());
        assertEquals(Rank.MASTER, s7.rank("perception"));
        assertEquals(Rank.MASTER, s7.rank("athletics"));
        assertEquals(101, CharacterMath.maxHp(s7));
        assertEquals(101, s7.currentHp());
        assertTrue(s7.specials().contains("Weapon Specialization"));
        assertEquals("General Feat", s7.feats().get(s7.feats().size() - 1).type());
    }

    @Test
    void masterSkillNeedsLevel7() throws Exception {
        CharacterSheet s = rebuilt(fighter5(), "Fighter", 2).withRank("athletics", Rank.EXPERT);
        assertThrows(InvalidChoiceException.class, () -> LevelUp.apply(s, registry(),
                new LevelUpChoices(Optional.of("athletics"), Set.of(), List.of("A"))));
        CharacterSheet ok = LevelUp.apply(s.withRank("athletics", Rank.TRAINED), registry(),
                new LevelUpChoices(Optional.of("athletics"), Set.of(), List.of("A")));
        assertEquals(Rank.EXPERT, ok.rank("athletics"));
    }

    @Test
    void boostsAtLevel10() throws Exception {
        CharacterSheet s = rebuilt(fighter5(), "Fighter", 9);
        LevelUpOptions o = LevelUp.options(s, registry());
        assertEquals(4, o.abilityBoosts());
        List<String> names = o.featSlots().stream().map(x -> "F").toList();
        LevelUpChoices ok = new LevelUpChoices(Optional.empty(),
                Set.of(Ability.STR, Ability.CON, Ability.DEX, Ability.WIS), names);
        CharacterSheet r = LevelUp.apply(s, registry(), ok);
        assertEquals(19, r.abilityScores().get(Ability.STR));
        assertEquals(18, r.abilityScores().get(Ability.CON));
        assertThrows(InvalidChoiceException.class, () -> LevelUp.apply(s, registry(),
                new LevelUpChoices(Optional.empty(),
                        Set.of(Ability.STR, Ability.CON, Ability.DEX), names)));
        assertThrows(InvalidChoiceException.class, () -> LevelUp.apply(s, registry(),
                new LevelUpChoices(Optional.empty(),
                        Set.of(Ability.STR, Ability.CON, Ability.DEX, Ability.WIS, Ability.INT),
                        names)));
    }

    @Test
    void conBoostAddsRetroactiveHp() throws Exception {
        CharacterSheet base = rebuilt(fighter5(), "Fighter", 9).withAbilityScore(Ability.CON, 16);
        CharacterSheet full = base.withCurrentHp(CharacterMath.maxHp(base));
        LevelUpChoices choices = new LevelUpChoices(Optional.empty(),
                Set.of(Ability.CON, Ability.STR, Ability.DEX, Ability.WIS), List.of("A", "B"));
        CharacterSheet r = LevelUp.apply(full, registry(), choices);
        assertEquals(18, r.abilityScores().get(Ability.CON));
        assertEquals(full.currentHp() + CharacterMath.maxHp(r) - CharacterMath.maxHp(full),
                r.currentHp());
        assertEquals(CharacterMath.maxHp(r), r.currentHp());
        CharacterSheet hurt = base.withCurrentHp(10);
        assertEquals(10 + CharacterMath.maxHp(LevelUp.apply(hurt, registry(), choices))
                - CharacterMath.maxHp(hurt), LevelUp.apply(hurt, registry(), choices).currentHp());
    }

    @Test
    void requiresGrant() throws Exception {
        CharacterSheet s = fighter5().withLevelUpGranted(false);
        assertThrows(InvalidChoiceException.class,
                () -> LevelUp.apply(s, registry(), feats("A", "B")));
    }

    @Test
    void unknownClassAndLevel20() throws Exception {
        assertThrows(UnsupportedClassException.class,
                () -> LevelUp.options(rebuilt(fighter5(), "Homebrew", 5), registry()));
        assertThrows(UnsupportedClassException.class,
                () -> LevelUp.options(rebuilt(fighter5(), "Fighter", 20), registry()));
    }
    @Test
    void skillIncreaseTargetsRespectRankCaps() throws Exception {
        CharacterSheet s = fighter5().withRank("athletics", Rank.EXPERT)
                .withRank("acrobatics", Rank.TRAINED).withRank("stealth", Rank.MASTER)
                .withRank("arcana", Rank.LEGENDARY);
        List<String> at6 = LevelUp.skillIncreaseTargets(s, 6);
        assertTrue(at6.contains("acrobatics"));
        assertTrue(at6.contains("crafting"));
        assertFalse(at6.contains("athletics"));
        assertFalse(at6.contains("stealth"));
        assertFalse(at6.contains("arcana"));
        List<String> at7 = LevelUp.skillIncreaseTargets(s, 7);
        assertTrue(at7.contains("athletics"));
        assertFalse(at7.contains("stealth"));
        List<String> at15 = LevelUp.skillIncreaseTargets(s, 15);
        assertTrue(at15.contains("stealth"));
        assertFalse(at15.contains("arcana"));
        for (int i = 0; i < s.lores().size(); i++) {
            assertEquals(s.lores().get(i).rank() != Rank.LEGENDARY,
                    LevelUp.skillIncreaseTargets(s, 20).contains("lore:" + i));
        }
    }
}
