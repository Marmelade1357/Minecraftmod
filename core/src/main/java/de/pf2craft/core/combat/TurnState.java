package de.pf2craft.core.combat;

/** Actions, reaction and attacks of the combatant whose turn it currently is. */
public final class TurnState {
    public static final int ACTIONS_PER_TURN = 3;

    private int actionsLeft = ACTIONS_PER_TURN;
    private boolean reactionAvailable = true;
    private int attacksMade;

    public TurnState() {
    }

    public int actionsLeft() {
        return actionsLeft;
    }

    public boolean reactionAvailable() {
        return reactionAvailable;
    }

    public int attacksMade() {
        return attacksMade;
    }

    /** Spends {@code n} actions; returns false and changes nothing if not enough are left. */
    public boolean spendActions(int n) {
        if (n < 0 || n > actionsLeft) {
            return false;
        }
        actionsLeft -= n;
        return true;
    }

    public void spendReaction() {
        reactionAvailable = false;
    }

    /** Number of the next attack this turn (1 for the first), used for the multiple attack penalty. */
    public int nextAttackNumber() {
        return attacksMade + 1;
    }

    public void recordAttack() {
        attacksMade++;
    }
}
