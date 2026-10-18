package de.pf2craft.core.monster;

/** Shared fixture for the NPC tests. */
final class NpcTestData {
    static final String KOBOLD_WARRIOR = """
            [{"id":"kobold-warrior","name":"Kobold Warrior","level":-1,"size":"small",
             "traits":["humanoid","kobold"],"perception":2,"ac":16,"fortitude":5,"reflex":7,"will":3,
             "hp":8,"speed":25,
             "strikes":[{"name":"Spear","attackBonus":7,"ranged":false,"traits":["thrown 20 ft."],
               "damage":[{"dice":"1d6","bonus":1,"type":"piercing"},{"dice":"","bonus":1,"type":"fire"}]}],
             "resistances":[{"type":"fire","value":2}],
             "weaknesses":[{"type":"cold","value":2}],
             "immunities":["paralyzed"],
             "skills":{"Acrobatics":5,"Stealth":5},
             "abilities":["Dragon Lore"]}]""";

    private NpcTestData() {
    }

    static MonsterData kobold() {
        return MonsterParser.parse(KOBOLD_WARRIOR).get(0);
    }
}
