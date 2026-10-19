package de.pf2craft.client;

import de.pf2craft.core.combat.Condition;
import de.pf2craft.network.CombatSyncPayload;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Client-side lookups and texts for the conditions that came with the combat sync. */
public final class ClientConditions {
    private ClientConditions() {
    }

    /** The conditions this client may see for the combatant; empty if it is not in the combat. */
    public static Map<Condition, Integer> of(UUID id) {
        return entry(id).map(CombatSyncPayload.Entry::conditionMap).orElse(Map.of());
    }

    public static Optional<CombatSyncPayload.Entry> entry(UUID id) {
        return ClientCombat.get().flatMap(s -> s.entries().stream().filter(e -> e.id().equals(id)).findFirst());
    }

    /** "Frightened 1" or "Prone". */
    public static MutableText label(Condition c, int value) {
        MutableText name = Text.translatable("pf2craft.condition." + c.id());
        return c.valued() ? name.append(" " + value) : name;
    }

    /** All conditions as one line, e.g. "Frightened 1, Prone"; empty if there are none. */
    public static MutableText line(Map<Condition, Integer> conditions) {
        MutableText line = Text.empty();
        boolean first = true;
        for (Map.Entry<Condition, Integer> e : conditions.entrySet()) {
            if (!first) {
                line.append(", ");
            }
            line.append(label(e.getKey(), e.getValue()));
            first = false;
        }
        return line;
    }
}
