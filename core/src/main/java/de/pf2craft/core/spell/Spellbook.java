package de.pf2craft.core.spell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A character's spellcasting entries plus the mutable state: used slots and focus points. */
public final class Spellbook {
    private final List<SpellcastingEntry> entries;
    private final int[][] used;
    private final int focusMax;
    private int focusCurrent;

    public Spellbook(List<SpellcastingEntry> entries, int focusMax) {
        this(entries, focusMax, focusMax, null);
    }

    /** Full state constructor used by deserialization; {@code used} may be null (nothing used). */
    public Spellbook(List<SpellcastingEntry> entries, int focusMax, int focusCurrent, int[][] used) {
        this.entries = List.copyOf(entries);
        this.focusMax = Math.max(0, focusMax);
        this.focusCurrent = Math.max(0, Math.min(this.focusMax, focusCurrent));
        this.used = new int[this.entries.size()][SpellcastingEntry.RANKS];
        if (used != null) {
            for (int i = 0; i < this.used.length && i < used.length; i++) {
                if (used[i] == null) {
                    continue;
                }
                for (int r = 0; r < SpellcastingEntry.RANKS && r < used[i].length; r++) {
                    this.used[i][r] = Math.max(0, used[i][r]);
                }
            }
        }
    }

    public static Spellbook empty() {
        return new Spellbook(List.of(), 0);
    }

    public List<SpellcastingEntry> entries() {
        return entries;
    }

    public int focusMax() {
        return focusMax;
    }

    public int focusCurrent() {
        return focusCurrent;
    }

    /** Slots spent so far for an entry and rank (0 for invalid indices). */
    public int slotsUsed(int entry, int rank) {
        return valid(entry, rank) ? used[entry][rank] : 0;
    }

    /** Remaining slots; cantrips (rank 0) are free and report {@link Integer#MAX_VALUE}. */
    public int slotsLeft(int entry, int rank) {
        if (!valid(entry, rank)) {
            return 0;
        }
        if (rank == 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, entries.get(entry).perDay()[rank] - used[entry][rank]);
    }

    /** Spends one slot of a prepared/spontaneous entry; focus and innate entries are handled elsewhere. */
    public boolean spendSlot(int entry, int rank) {
        if (!valid(entry, rank)) {
            return false;
        }
        CastingType type = entries.get(entry).type();
        if (type == CastingType.FOCUS || type == CastingType.INNATE) {
            return false;
        }
        if (rank == 0) {
            return true;
        }
        if (slotsLeft(entry, rank) <= 0) {
            return false;
        }
        used[entry][rank]++;
        return true;
    }

    public boolean spendFocus() {
        if (focusCurrent <= 0) {
            return false;
        }
        focusCurrent--;
        return true;
    }

    public void rest() {
        for (int[] row : used) {
            Arrays.fill(row, 0);
        }
        focusCurrent = focusMax;
    }

    public void resetSlots(int entry) {
        if (entry >= 0 && entry < used.length) {
            Arrays.fill(used[entry], 0);
        }
    }

    public Spellbook copy() {
        int[][] u = new int[used.length][];
        for (int i = 0; i < used.length; i++) {
            u[i] = used[i].clone();
        }
        return new Spellbook(new ArrayList<>(entries), focusMax, focusCurrent, u);
    }

    private boolean valid(int entry, int rank) {
        return entry >= 0 && entry < entries.size() && rank >= 0 && rank < SpellcastingEntry.RANKS;
    }
}
