package de.pf2craft.core.character;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable character sheet. Proficiency keys: perception, fortitude, reflex, will, class_dc,
 * unarmored, light, medium, heavy, simple, martial, advanced, unarmed and every
 * {@link Skill#id()}; a missing key means untrained.
 */
public record CharacterSheet(String name, String className, String ancestry, String heritage,
                             String background, int level, Ability keyAbility,
                             Map<Ability, Integer> abilityScores, Map<String, Rank> proficiencies,
                             List<Lore> lores, HitPoints hitPoints, int speed, ArmorClass armorClass,
                             List<Strike> strikes, List<Feat> feats, List<String> specials,
                             int currentHp, boolean levelUpGranted,
                             Optional<Shield> shield) {

    public CharacterSheet {
        abilityScores = Collections.unmodifiableMap(copyScores(abilityScores));
        proficiencies = Map.copyOf(proficiencies);
        lores = List.copyOf(lores);
        strikes = List.copyOf(strikes);
        feats = List.copyOf(feats);
        specials = List.copyOf(specials);
    }

    private static EnumMap<Ability, Integer> copyScores(Map<Ability, Integer> scores) {
        EnumMap<Ability, Integer> copy = new EnumMap<>(Ability.class);
        copy.putAll(scores);
        return copy;
    }

    public Rank rank(String key) {
        return proficiencies.getOrDefault(key, Rank.UNTRAINED);
    }

    public int abilityMod(Ability ability) {
        return Ability.modifier(abilityScores.getOrDefault(ability, 10));
    }

    public CharacterSheet withCurrentHp(int hp) {
        return new CharacterSheet(name, className, ancestry, heritage, background, level, keyAbility,
                abilityScores, proficiencies, lores, hitPoints, speed, armorClass, strikes, feats,
                specials, hp, levelUpGranted, shield);
    }

    public CharacterSheet withAbilityScore(Ability ability, int score) {
        Map<Ability, Integer> scores = copyScores(abilityScores);
        scores.put(ability, score);
        return new CharacterSheet(name, className, ancestry, heritage, background, level, keyAbility,
                scores, proficiencies, lores, hitPoints, speed, armorClass, strikes, feats,
                specials, currentHp, levelUpGranted, shield);
    }

    public CharacterSheet withRank(String key, Rank rank) {
        Map<String, Rank> prof = new HashMap<>(proficiencies);
        prof.put(key, rank);
        return new CharacterSheet(name, className, ancestry, heritage, background, level, keyAbility,
                abilityScores, prof, lores, hitPoints, speed, armorClass, strikes, feats,
                specials, currentHp, levelUpGranted, shield);
    }

    public CharacterSheet withLevelUpGranted(boolean granted) {
        return new CharacterSheet(name, className, ancestry, heritage, background, level, keyAbility,
                abilityScores, proficiencies, lores, hitPoints, speed, armorClass, strikes, feats,
                specials, currentHp, granted, shield);
    }
}
