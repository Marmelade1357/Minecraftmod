package de.pf2craft.core.character;

import java.util.OptionalInt;

/** Derived values of a character sheet. */
public final class CharacterMath {
    private CharacterMath() {
    }

    public static int maxHp(CharacterSheet s) {
        HitPoints hp = s.hitPoints();
        return hp.ancestryHp() + hp.bonusHp()
                + s.level() * (hp.classHpPerLevel() + s.abilityMod(Ability.CON) + hp.bonusHpPerLevel());
    }

    public static int armorClass(CharacterSheet s) {
        ArmorClass ac = s.armorClass();
        int dex = s.abilityMod(Ability.DEX);
        if (ac.dexCap().isPresent()) {
            dex = Math.min(dex, ac.dexCap().getAsInt());
        }
        return 10 + dex + s.rank(ac.armorCategory()).bonus(s.level()) + ac.itemBonus();
    }

    public static int save(CharacterSheet s, Save save) {
        return s.abilityMod(save.ability()) + s.rank(save.id()).bonus(s.level());
    }

    public static int perception(CharacterSheet s) {
        return s.abilityMod(Ability.WIS) + s.rank("perception").bonus(s.level());
    }

    public static int skill(CharacterSheet s, Skill skill) {
        return s.abilityMod(skill.ability()) + s.rank(skill.id()).bonus(s.level());
    }

    public static int lore(CharacterSheet s, int index) {
        return s.abilityMod(Ability.INT) + s.lores().get(index).rank().bonus(s.level());
    }

    public static int classDc(CharacterSheet s) {
        return 10 + s.abilityMod(s.keyAbility()) + s.rank("class_dc").bonus(s.level());
    }

    public static int strikeAttack(CharacterSheet s, int index) {
        Strike st = s.strikes().get(index);
        return s.abilityMod(st.attackAbility()) + s.rank(st.category()).bonus(s.level())
                + st.otherAttackBonus();
    }

    public static int strikeDamageBonus(CharacterSheet s, int index) {
        Strike st = s.strikes().get(index);
        return st.otherDamageBonus()
                + (st.attackAbility() == Ability.STR ? s.abilityMod(Ability.STR) : 0);
    }

    /** Speed in blocks (1 block = 5 ft). */
    public static int speedBlocks(CharacterSheet s) {
        return s.speed() / 5;
    }

    /** Bonus for a check, or empty if the check refers to something the sheet does not have. */
    public static OptionalInt checkBonus(CharacterSheet s, CheckId id) {
        switch (id.kind()) {
            case SKILL:
                for (Skill skill : Skill.values()) {
                    if (skill.id().equals(id.key())) {
                        return OptionalInt.of(skill(s, skill));
                    }
                }
                return OptionalInt.empty();
            case SAVE:
                for (Save save : Save.values()) {
                    if (save.id().equals(id.key())) {
                        return OptionalInt.of(save(s, save));
                    }
                }
                return OptionalInt.empty();
            case PERCEPTION:
                return OptionalInt.of(perception(s));
            case LORE:
                return id.index() < s.lores().size() ? OptionalInt.of(lore(s, id.index()))
                        : OptionalInt.empty();
            case STRIKE:
                return id.index() < s.strikes().size() ? OptionalInt.of(strikeAttack(s, id.index()))
                        : OptionalInt.empty();
            default:
                return OptionalInt.empty();
        }
    }
}
