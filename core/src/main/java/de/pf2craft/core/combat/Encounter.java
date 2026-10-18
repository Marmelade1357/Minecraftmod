package de.pf2craft.core.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Initiative order, current turn and round counter of one encounter.
 * Not thread-safe; used from the server thread only.
 */
public final class Encounter {
    public record Entry(Combatant combatant, int initiative, boolean active) {
    }

    private static final Comparator<Entry> ORDER = Comparator
            .comparingInt(Entry::initiative).reversed()
            .thenComparing((Entry e) -> !e.combatant().npc())
            .thenComparing(Comparator.comparingInt((Entry e) -> e.combatant().perception()).reversed())
            .thenComparing(e -> e.combatant().name());

    private final List<Entry> entries = new ArrayList<>();
    private boolean started;
    /** Index of the current entry; if {@code currentGone}, the index of the entry that would come next. */
    private int cursor;
    private boolean currentGone;
    private int round = 1;
    private TurnState turn = new TurnState();

    public void add(Combatant c, int initiativeTotal) {
        if (indexOf(c.id()) >= 0) {
            throw new IllegalArgumentException("duplicate combatant " + c.id());
        }
        Entry entry = new Entry(c, initiativeTotal, true);
        int idx = 0;
        while (idx < entries.size() && ORDER.compare(entries.get(idx), entry) <= 0) {
            idx++;
        }
        entries.add(idx, entry);
        if (started && (currentGone ? idx < cursor : idx <= cursor)) {
            cursor++;
        }
    }

    /** Fixes the pointer on the highest-initiative active entry; later adds do not move it. */
    public void start() {
        if (started) {
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).active()) {
                started = true;
                cursor = i;
                currentGone = false;
                return;
            }
        }
    }

    public List<Entry> order() {
        return List.copyOf(entries);
    }

    public Optional<Combatant> current() {
        start();
        if (!started || currentGone || cursor >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(cursor).combatant());
    }

    public int round() {
        return round;
    }

    public TurnState turn() {
        return turn;
    }

    public void setActive(String id, boolean active) {
        int idx = indexOf(id);
        if (idx >= 0) {
            Entry e = entries.get(idx);
            entries.set(idx, new Entry(e.combatant(), e.initiative(), active));
        }
    }

    /** Removes a combatant; if it was the current one, the caller should {@link #advance()} afterwards. */
    public void remove(String id) {
        int idx = indexOf(id);
        if (idx < 0) {
            return;
        }
        entries.remove(idx);
        if (!started) {
            return;
        }
        if (idx < cursor) {
            cursor--;
        } else if (idx == cursor && !currentGone) {
            currentGone = true;
        }
    }

    /** Moves to the next active combatant (wrapping starts a new round) with a fresh turn state. */
    public Optional<Combatant> advance() {
        if (!hasActiveAny()) {
            return Optional.empty();
        }
        start();
        int n = entries.size();
        int from = currentGone ? cursor : cursor + 1;
        for (int i = 0; i < n; i++) {
            int pos = from + i;
            int idx = pos % n;
            if (entries.get(idx).active()) {
                if (pos >= n) {
                    round++;
                }
                cursor = idx;
                currentGone = false;
                turn = new TurnState();
                return Optional.of(entries.get(idx).combatant());
            }
        }
        return Optional.empty();
    }

    public boolean hasActive(boolean npc) {
        for (Entry e : entries) {
            if (e.active() && e.combatant().npc() == npc) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveAny() {
        return hasActive(true) || hasActive(false);
    }

    private int indexOf(String id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).combatant().id().equals(id)) {
                return i;
            }
        }
        return -1;
    }
}
