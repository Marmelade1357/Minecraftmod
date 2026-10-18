package de.pf2craft.core.monster;

/** A live NPC: its (possibly GM-edited) stat block, display name and current hit points. */
public record NpcStats(MonsterData base, String displayName, int currentHp) {
    public static NpcStats fresh(MonsterData base) {
        return new NpcStats(base, base.name(), base.hp());
    }

    public boolean defeated() {
        return currentHp <= 0;
    }
}
