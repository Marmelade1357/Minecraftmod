package de.pf2craft.core.combat;

import de.pf2craft.core.character.CharacterMath;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.Strike;
import de.pf2craft.core.monster.DamagePart;
import de.pf2craft.core.monster.MonsterStrike;
import java.util.ArrayList;
import java.util.List;

/** Builds the damage components of player and monster strikes. */
public final class StrikeDamage {
    private StrikeDamage() {
    }

    public static List<DamageDice> forStrike(CharacterSheet sheet, int strikeIndex) {
        Strike strike = sheet.strikes().get(strikeIndex);
        List<DamageDice> out = new ArrayList<>();
        out.add(new DamageDice(strike.damageDice() + strike.damageDie(),
                CharacterMath.strikeDamageBonus(sheet, strikeIndex), typeOf(strike.damageType())));
        out.addAll(RuneDamage.parts(strike.propertyRunes()));
        return out;
    }

    public static List<DamageDice> forMonster(MonsterStrike strike) {
        List<DamageDice> out = new ArrayList<>();
        for (DamagePart p : strike.damage()) {
            out.add(new DamageDice(p.dice(), p.bonus(), p.type()));
        }
        return out;
    }

    private static String typeOf(String letter) {
        return switch (letter == null ? "" : letter.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "P" -> "piercing";
            case "S" -> "slashing";
            default -> "bludgeoning";
        };
    }
}
