package de.pf2craft.core.combat;

import java.util.List;
import java.util.Optional;

/** The basic actions available in the action menu. */
public final class BasicActions {
    public static final List<BasicAction> ALL = List.of(
            new BasicAction("stride", 1, ActionKind.SELF, "", "", false, false, 0),
            new BasicAction("step", 1, ActionKind.SELF, "", "", false, false, 0),
            new BasicAction("raise_shield", 1, ActionKind.SELF, "", "", false, true, 0),
            new BasicAction("take_cover", 1, ActionKind.SELF, "", "", false, false, 0),
            new BasicAction("seek", 1, ActionKind.SELF, "perception", "", false, false, 0),
            new BasicAction("demoralize", 1, ActionKind.TARGET_RANGE, "intimidation", "will", false, false, 6),
            new BasicAction("trip", 1, ActionKind.TARGET_MELEE, "athletics", "reflex", true, false, 0),
            new BasicAction("grapple", 1, ActionKind.TARGET_MELEE, "athletics", "fortitude", true, false, 0),
            new BasicAction("stand_up", 1, ActionKind.SELF, "", "", false, false, 0),
            // The server rolls the better of athletics and acrobatics against the grabber's Fortitude DC.
            new BasicAction("escape", 1, ActionKind.SELF, "athletics", "", false, false, 0));

    private BasicActions() {
    }

    public static Optional<BasicAction> byId(String id) {
        return ALL.stream().filter(a -> a.id().equals(id)).findFirst();
    }
}
