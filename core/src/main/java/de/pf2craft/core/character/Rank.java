package de.pf2craft.core.character;

/** Proficiency ranks. Untrained adds nothing, every other rank adds level + rank bonus. */
public enum Rank {
    UNTRAINED(0),
    TRAINED(2),
    EXPERT(4),
    MASTER(6),
    LEGENDARY(8);

    private final int rankBonus;

    Rank(int rankBonus) {
        this.rankBonus = rankBonus;
    }

    public int bonus(int level) {
        return this == UNTRAINED ? 0 : level + rankBonus;
    }

    /** Converts Pathbuilder's proficiency number (0/2/4/6/8). */
    public static Rank fromPathbuilder(int value) {
        for (Rank rank : values()) {
            if (rank.rankBonus == value) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Unknown proficiency value: " + value);
    }

    /** The next higher rank, or this one if already legendary. */
    public Rank next() {
        return this == LEGENDARY ? this : values()[ordinal() + 1];
    }
}
