package de.pf2craft.core.combat;

/**
 * Tracks movement within Strides: 1 block = 5 ft, each new Stride costs one action.
 * The service creates a budget (or calls {@link #reset()}) for every turn.
 */
public final class MovementBudget {
    private static final double EPSILON = 1e-6;

    private final int blocksPerStride;
    private double remainingInStride;

    public MovementBudget(int speedFeet) {
        this.blocksPerStride = speedFeet > 0 ? Math.max(1, speedFeet / 5) : 0;
    }

    public int blocksPerStride() {
        return blocksPerStride;
    }

    public double remainingInStride() {
        return remainingInStride;
    }

    /** Forgets the current Stride; the next move costs an action again. */
    public void reset() {
        remainingInStride = 0;
    }

    /** Moves {@code blocks}; starts a new Stride (one action) when the current one is used up. */
    public boolean tryMove(double blocks, TurnState turn) {
        if (blocks <= 0) {
            return true;
        }
        if (blocksPerStride == 0) {
            return false;
        }
        if (remainingInStride <= EPSILON) {
            if (!turn.spendActions(1)) {
                return false;
            }
            remainingInStride = blocksPerStride;
        }
        remainingInStride -= blocks; // overshooting by one step is allowed
        return true;
    }

    /**
     * Pays for a whole walk of {@code blocks} up front (used for NPCs, whose path is known): spends the Strides
     * needed beyond what is left of the current one and keeps the unused rest of the last Stride. Returns false
     * and changes nothing if the actions do not suffice.
     */
    public boolean payWalk(double blocks, TurnState turn) {
        if (blocks <= 0) {
            return true;
        }
        int strides = stridesNeededFor(Math.max(0, blocks - remainingInStride));
        if (strides == Integer.MAX_VALUE || !turn.spendActions(strides)) {
            return false;
        }
        remainingInStride = Math.max(0, remainingInStride + (double) strides * blocksPerStride - blocks);
        return true;
    }

    public int stridesNeededFor(double blocks) {
        if (blocks <= 0) {
            return 0;
        }
        if (blocksPerStride == 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(blocks / blocksPerStride);
    }
}
