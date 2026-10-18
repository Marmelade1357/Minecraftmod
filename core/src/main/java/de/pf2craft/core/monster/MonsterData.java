package de.pf2craft.core.monster;

import java.util.List;
import java.util.Map;

/** Stat block of a monster or NPC. {@code speed} is the land speed in feet, {@code abilities} are names only. */
public record MonsterData(String id, String name, int level, String size, List<String> traits,
        int perception, int ac, int fortitude, int reflex, int will, int hp, int speed,
        List<MonsterStrike> strikes, List<Resistance> resistances, List<Resistance> weaknesses,
        List<String> immunities, Map<String, Integer> skills, List<String> abilities) {
}
