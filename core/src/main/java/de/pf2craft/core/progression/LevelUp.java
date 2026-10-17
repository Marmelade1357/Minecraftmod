package de.pf2craft.core.progression;

import de.pf2craft.core.character.Ability;
import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Feat;
import de.pf2craft.core.character.Lore;
import de.pf2craft.core.character.Rank;
import de.pf2craft.core.character.Skill;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Rules for advancing a character by one level. */
public final class LevelUp {
    public static final int MAX_LEVEL = 20;
    private static final int MAX_FEAT_NAME = 64;
    private static final int BOOSTS_PER_LEVEL = 4;

    private LevelUp() {
    }

    public static LevelUpOptions options(CharacterSheet sheet, ClassRegistry registry)
            throws UnsupportedClassException {
        ClassProgression progression = registry.find(sheet.className()).orElseThrow(() ->
                new UnsupportedClassException("Class not supported: " + sheet.className()));
        if (sheet.level() >= MAX_LEVEL) {
            throw new UnsupportedClassException("Maximum level reached.");
        }
        int newLevel = sheet.level() + 1;
        ClassProgression.LevelEntry entry = progression.levels().get(newLevel);
        if (entry == null) {
            throw new UnsupportedClassException("No data for level " + newLevel + ".");
        }
        List<String> slots = new ArrayList<>(entry.featSlots());
        if (ClassProgression.ANCESTRY_FEAT_LEVELS.contains(newLevel)) {
            slots.add("Ancestry Feat");
        }
        if (ClassProgression.GENERAL_FEAT_LEVELS.contains(newLevel)) {
            slots.add("General Feat");
        }
        int boosts = ClassProgression.ABILITY_BOOST_LEVELS.contains(newLevel) ? BOOSTS_PER_LEVEL : 0;
        return new LevelUpOptions(newLevel, entry.skillIncrease(), boosts, slots, entry.features(),
                hpGain(sheet));
    }

    public static CharacterSheet apply(CharacterSheet sheet, ClassRegistry registry,
                                       LevelUpChoices choices)
            throws UnsupportedClassException, InvalidChoiceException {
        LevelUpOptions options = options(sheet, registry);
        if (!sheet.levelUpGranted()) {
            throw new InvalidChoiceException("The GM has not granted a level-up.");
        }
        int newLevel = options.newLevel();
        ClassProgression.LevelEntry entry = registry.find(sheet.className()).orElseThrow()
                .levels().get(newLevel);

        if (options.skillIncrease() != choices.skillIncrease().isPresent()) {
            throw new InvalidChoiceException(options.skillIncrease()
                    ? "A skill increase must be chosen." : "No skill increase at this level.");
        }
        if (choices.boosts().size() != options.abilityBoosts()) {
            throw new InvalidChoiceException("Choose exactly " + options.abilityBoosts()
                    + " different ability boosts.");
        }
        if (choices.featNames().size() != options.featSlots().size()) {
            throw new InvalidChoiceException("Choose exactly " + options.featSlots().size()
                    + " feats.");
        }
        for (String name : choices.featNames()) {
            if (name == null || name.isBlank() || name.length() > MAX_FEAT_NAME) {
                throw new InvalidChoiceException("Feat names must be 1-" + MAX_FEAT_NAME
                        + " characters long.");
            }
        }

        CharacterSheet result = sheet;
        for (Map.Entry<String, Rank> fixed : entry.proficiencies().entrySet()) {
            if (fixed.getValue().compareTo(result.rank(fixed.getKey())) > 0) {
                result = result.withRank(fixed.getKey(), fixed.getValue());
            }
        }
        if (options.skillIncrease()) {
            result = increaseSkill(result, choices.skillIncrease().get(), newLevel);
        }
        for (Ability ability : choices.boosts()) {
            int score = result.abilityScores().getOrDefault(ability, 10);
            result = result.withAbilityScore(ability, score + (score < 18 ? 2 : 1));
        }

        List<Feat> feats = new ArrayList<>(result.feats());
        for (int i = 0; i < options.featSlots().size(); i++) {
            feats.add(new Feat(choices.featNames().get(i).strip(), options.featSlots().get(i),
                    newLevel));
        }
        List<String> specials = new ArrayList<>(result.specials());
        specials.addAll(options.features());

        CharacterSheet leveled = new CharacterSheet(result.name(), result.className(),
                result.ancestry(), result.heritage(), result.background(), newLevel,
                result.keyAbility(), result.abilityScores(), result.proficiencies(), result.lores(),
                result.hitPoints(), result.speed(), result.armorClass(), result.strikes(), feats,
                specials, result.currentHp(), false, result.shield());
        // Max HP growth includes retroactive HP from a CON boost.
        return leveled.withCurrentHp(sheet.currentHp()
                + CharacterMath.maxHp(leveled) - CharacterMath.maxHp(sheet));
    }

    /**
     * Skill ids and "lore:&lt;n&gt;" entries that may receive a skill increase at
     * {@code newLevel} (master needs level 7, legendary level 15).
     */
    public static List<String> skillIncreaseTargets(CharacterSheet sheet, int newLevel) {
        List<String> out = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            if (canRaise(sheet.rank(skill.id()), newLevel)) {
                out.add(skill.id());
            }
        }
        for (int i = 0; i < sheet.lores().size(); i++) {
            if (canRaise(sheet.lores().get(i).rank(), newLevel)) {
                out.add("lore:" + i);
            }
        }
        return out;
    }

    private static boolean canRaise(Rank current, int newLevel) {
        Rank next = current.next();
        return current != Rank.LEGENDARY
                && (next != Rank.MASTER || newLevel >= 7)
                && (next != Rank.LEGENDARY || newLevel >= 15);
    }

    private static int hpGain(CharacterSheet s) {
        return s.hitPoints().classHpPerLevel() + s.abilityMod(Ability.CON)
                + s.hitPoints().bonusHpPerLevel();
    }

    private static CharacterSheet increaseSkill(CharacterSheet sheet, String target, int newLevel)
            throws InvalidChoiceException {
        if (target.startsWith("lore:")) {
            int index;
            try {
                index = Integer.parseInt(target.substring(5));
            } catch (NumberFormatException e) {
                throw new InvalidChoiceException("Unknown lore: " + target);
            }
            if (index < 0 || index >= sheet.lores().size()) {
                throw new InvalidChoiceException("Unknown lore: " + target);
            }
            Lore lore = sheet.lores().get(index);
            Rank raised = checkedNext(lore.rank(), newLevel);
            List<Lore> lores = new ArrayList<>(sheet.lores());
            lores.set(index, new Lore(lore.name(), raised));
            return new CharacterSheet(sheet.name(), sheet.className(), sheet.ancestry(),
                    sheet.heritage(), sheet.background(), sheet.level(), sheet.keyAbility(),
                    sheet.abilityScores(), sheet.proficiencies(), lores, sheet.hitPoints(),
                    sheet.speed(), sheet.armorClass(), sheet.strikes(), sheet.feats(),
                    sheet.specials(), sheet.currentHp(), sheet.levelUpGranted(), sheet.shield());
        }
        for (Skill skill : Skill.values()) {
            if (skill.id().equals(target)) {
                return sheet.withRank(target, checkedNext(sheet.rank(target), newLevel));
            }
        }
        throw new InvalidChoiceException("Unknown skill: " + target);
    }

    private static Rank checkedNext(Rank current, int newLevel) throws InvalidChoiceException {
        if (current == Rank.LEGENDARY) {
            throw new InvalidChoiceException("That skill is already legendary.");
        }
        Rank next = current.next();
        if (next == Rank.MASTER && newLevel < 7) {
            throw new InvalidChoiceException("Master requires level 7.");
        }
        if (next == Rank.LEGENDARY && newLevel < 15) {
            throw new InvalidChoiceException("Legendary requires level 15.");
        }
        return next;
    }
}
