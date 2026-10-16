package de.pf2craft.core.check;

import java.util.OptionalInt;

/**
 * Pathfinder 2e degrees of success: beating the DC by 10 or more is a critical success,
 * missing it by 10 or more a critical failure. A natural 20 improves the result by one
 * step, a natural 1 worsens it by one step.
 */
public enum DegreeOfSuccess {
    CRITICAL_FAILURE,
    FAILURE,
    SUCCESS,
    CRITICAL_SUCCESS;

    public static DegreeOfSuccess determine(int total, int dc, OptionalInt naturalD20) {
        DegreeOfSuccess degree;
        if (total >= dc + 10) {
            degree = CRITICAL_SUCCESS;
        } else if (total >= dc) {
            degree = SUCCESS;
        } else if (total <= dc - 10) {
            degree = CRITICAL_FAILURE;
        } else {
            degree = FAILURE;
        }
        if (naturalD20.isPresent()) {
            if (naturalD20.getAsInt() == 20) {
                degree = degree.upgrade();
            } else if (naturalD20.getAsInt() == 1) {
                degree = degree.downgrade();
            }
        }
        return degree;
    }

    public DegreeOfSuccess upgrade() {
        return this == CRITICAL_SUCCESS ? this : values()[ordinal() + 1];
    }

    public DegreeOfSuccess downgrade() {
        return this == CRITICAL_FAILURE ? this : values()[ordinal() - 1];
    }

    public boolean isSuccess() {
        return this == SUCCESS || this == CRITICAL_SUCCESS;
    }
}
