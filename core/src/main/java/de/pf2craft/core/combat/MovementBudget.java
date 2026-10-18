package de.pf2craft.core.combat;

/** Tracks movement within Strides: 1 block = 5 ft, each new Stride costs one action. */
public final class MovementBudget {
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

    /** Moves {@code blocks}; starts a new Stride (one action) when the current one is used up. */
    public boolean tryMove(double blocks, TurnState turn) {
        if (blocks <= 0) {
            return true;
        }
        if (blocksPerStride == 0) {
            return false;
        }
        if (remainingInStride <= 0) {
            if (!turn.spendActions(1)) {
                return false;
            }
            remainingInStride = blocksPerStride;
        }
        remainingInStride -= blocks; // overshooting by one step is allowed
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
