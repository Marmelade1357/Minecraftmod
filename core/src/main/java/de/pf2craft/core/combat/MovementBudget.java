package de.pf2craft.core.combat;

/**
 * Tracks movement within Strides: 1 block = 5 ft, each new Stride costs one action.
 * The service creates a budget (or calls {@link #reset()}) for every turn.
 */
public final class MovementBudget {
    public static final double EPSILON = 1e-6;

    private final int blocksPerStride;
    private double remainingInStride;
    private boolean inStep;

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
        inStep = false;
    }

    /**
     * Another action (an attack) interrupts the Stride: leftover distance is lost and the next move starts a new
     * Stride, which costs an action.
     */
    public void interrupt() {
        remainingInStride = 0;
        inStep = false;
    }

    /**
     * A Step: exactly one block, no action spent here (the caller pays it). Moving those blocks does not charge a
     * Stride. A larger running Stride remainder is never shrunk.
     */
    public void grantStep() {
        if (remainingInStride > 1 + EPSILON) {
            return;
        }
        remainingInStride = 1;
        inStep = true;
    }

    public boolean strideActive() {
        return remainingInStride > EPSILON;
    }

    /** Explicitly starts a Stride: spends one action and fills the budget. Refused (nothing changed) if one is active. */
    public boolean startStride(TurnState turn) {
        if (blocksPerStride == 0 || strideActive() || !turn.spendActions(1)) {
            return false;
        }
        inStep = false;
        remainingInStride = blocksPerStride;
        return true;
    }

    /**
     * Moves within the active Stride (or granted Step). Refused unchanged if none is active or {@code blocks}
     * exceeds the remainder by more than one block; a final step may overshoot, the remainder is clamped at 0.
     */
    public boolean moveWithinStride(double blocks) {
        if (!strideActive() || blocks > remainingInStride + 1 + EPSILON) {
            return false;
        }
        remainingInStride = Math.max(0, remainingInStride - blocks);
        return true;
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
            inStep = false; // the step block is used up
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
