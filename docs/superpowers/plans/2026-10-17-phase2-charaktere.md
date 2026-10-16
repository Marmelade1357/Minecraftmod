# Phase 2 – Characters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players import their Pathbuilder 2e character (or wait as spectators until they do), see it in a tabbed character sheet, roll checks by clicking, and level up in-game after the GM grants it; the GM can view and edit every sheet.

**Architecture:** All rules (character model, derived values, Pathbuilder parsing, class progression, level-up) live in `core` as pure Java with JUnit tests against real Pathbuilder exports. The Fabric side stores the sheet as a persistent player attachment (JSON string codec), fetches Pathbuilder asynchronously, and talks to the client only through request/sync payloads; the server recomputes every bonus itself. The client renders sheets from synced JSON using the same `core` math.

**Tech Stack:** Java 21, Fabric Loader 0.16.14, Fabric API 0.116.6+1.21.1, Yarn 1.21.1+build.3, Gson 2.11.0 (ships with Minecraft 1.21.1), JUnit 5, `java.net.http.HttpClient`.

**Spec:** `docs/superpowers/specs/2026-10-17-phase2-charaktere-design.md` (German). Read it before any task.

## Global Constraints

- Rules logic goes in `core` (`de.pf2craft.core.*`); `core` must not import `net.minecraft`/`net.fabricmc`. Gson is allowed in `core`.
- Server is authoritative: clients only send requests; the server validates lengths, ranges, permissions and recomputes every roll bonus from the stored sheet.
- Client-only code (screens, key bindings, `MinecraftClient`) only under `fabric/src/client`.
- New payloads: `record … implements CustomPayload` with `ID` and `CODEC`, registered in `Pf2Networking.registerPayloads()`.
- Player messages: `Text.translatableWithFallback(key, englishFallback, …)`; every new key goes into **both** `en_us.json` and `de_de.json` (German uses „W“ for dice).
- Code, comments, identifiers, commit messages in English; style as existing code (4 spaces, `final` utility classes with private constructor, few but meaningful comments).
- No AoN rules text in data files or UI — names and numbers only.
- Proficiency bonus: untrained 0, otherwise level + 2/4/6/8. Ability modifier = floor((score − 10) / 2). 1 block = 5 ft.
- Content names (classes, feats, weapons) stay as Pathbuilder delivers them (English); only UI labels are translated.
- Before every commit: `./gradlew -PcoreOnly :core:test` must pass; after any `fabric` change also `./gradlew build`. On this machine set `JAVA_HOME` to `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` first.

## Review Focus

1. Pathbuilder unreachable, slow, returns `{"success":false}`, HTML, or a huge body → player gets a clear error, stays spectator, server tick never blocks (Task 6: `PathbuilderResponseTest`; fetch runs async with timeouts).
2. ID input like `""`, `abc`, `12 34`, `99999999999` → rejected before any HTTP request (Task 6: `PathbuilderIdTest`).
3. A character whose class has no progression file → import succeeds, sheet works, level-up is refused with "not available for this class yet" (Task 5: `UnsupportedClassException` test; Task 7 sends `classSupported=false`, Task 8 shows the disabled button).
4. GM edits or grants for a player who logged off between opening and clicking → server answers "player not online", nothing crashes (Task 7 behaviour, Task 9 manual check by having the target leave).
5. Re-import while a level-up is pending, or a second import while the first is still downloading → re-import clears the pending flag and sets current HP to max; a second concurrent import is refused (Task 3 asserts `levelUpGranted false` and `currentHp == maxHp`; Task 6 in-flight guard, manual double-click check in Task 8).

---

## File Structure

```
core/build.gradle                                   + gson dependency
core/src/main/java/de/pf2craft/core/character/
    Ability.java            enum STR..CHA, modifier(score), Pathbuilder key
    Rank.java               enum UNTRAINED..LEGENDARY, bonus(level), fromPathbuilder(int)
    Skill.java              enum of the 16 skills with key ability
    Save.java               enum FORTITUDE/REFLEX/WILL with key ability
    Lore.java, Strike.java, Feat.java, HitPoints.java, ArmorClass.java   small records
    CharacterSheet.java     immutable record of all building blocks + wither methods
    CharacterMath.java      derived values (max HP, AC, saves, skills, strikes …)
    CheckId.java            parse/format "skill:athletics", "save:reflex", "perception", "lore:0", "strike:0"
    CharacterJson.java      Gson (de)serialisation of CharacterSheet
    CharacterImportException.java
    PathbuilderImporter.java   Pathbuilder JSON → CharacterSheet
core/src/main/java/de/pf2craft/core/progression/
    ClassProgression.java   record loaded from class JSON
    ClassProgressionParser.java
    ClassRegistry.java      lookup by Pathbuilder class name
    LevelUp.java            options(...) and apply(...)
    LevelUpOptions.java, LevelUpChoices.java, InvalidChoiceException.java, UnsupportedClassException.java
core/src/main/resources/pf2craft/classes/
    index.json              list of class file names
    fighter.json, …         one file per class
core/src/test/resources/pathbuilder/              Pathbuilder fixtures (MIT, from MrPrimate/pathmuncher) + LICENSE-pathmuncher.txt
core/src/test/java/de/pf2craft/core/character/…Test.java
core/src/test/java/de/pf2craft/core/progression/…Test.java

fabric/build.gradle                                 + resources.srcDir '../core/src/main/resources'
fabric/src/main/java/de/pf2craft/character/
    CharacterStore.java     attachment type + get/set/remove
    PathbuilderFetcher.java async HTTP, size/timeout limits
    PathbuilderId.java      input validation
    CharacterService.java   import flow, join handling, rolls, GM edits, level-up
    ClassData.java          loads ClassRegistry from jar + config/pf2craft/classes
fabric/src/main/java/de/pf2craft/network/
    ImportCharacterPayload, SheetRequestPayload, CheckRollPayload, GmEditPayload,
    GrantLevelUpPayload, LevelUpPayload (C2S); SheetSyncPayload, OpenImportPayload (S2C)
fabric/src/main/java/de/pf2craft/command/Pf2Commands.java   + /pf2 hp, /pf2 sheet, /pf2 character reset
fabric/src/client/java/de/pf2craft/client/
    ImportScreen.java, CharacterScreen.java, LevelUpScreen.java, ClientSheets.java
```

---

### Task 1: Core basics – enums, Gson, test fixtures

**Files:**
- Modify: `core/build.gradle`, `fabric/build.gradle`
- Create: `core/src/main/java/de/pf2craft/core/character/{Ability,Rank,Skill,Save}.java`
- Create: `core/src/test/resources/pathbuilder/*.json`, `core/src/test/resources/pathbuilder/LICENSE-pathmuncher.txt`
- Test: `core/src/test/java/de/pf2craft/core/character/BasicsTest.java`

**Interfaces:**
- Produces:
  - `enum Ability { STR, DEX, CON, INT, WIS, CHA; static int modifier(int score); String pathbuilderKey(); static Ability fromPathbuilder(String key) }` (keys `"str"`…; case-insensitive, also accepts `"Str"`)
  - `enum Rank { UNTRAINED, TRAINED, EXPERT, MASTER, LEGENDARY; int bonus(int level); static Rank fromPathbuilder(int value); Rank next() }` (`fromPathbuilder`: 0/2/4/6/8, anything else → `IllegalArgumentException`)
  - `enum Skill` – ACROBATICS(DEX), ARCANA(INT), ATHLETICS(STR), CRAFTING(INT), DECEPTION(CHA), DIPLOMACY(CHA), INTIMIDATION(CHA), MEDICINE(WIS), NATURE(WIS), OCCULTISM(INT), PERFORMANCE(CHA), RELIGION(WIS), SOCIETY(INT), STEALTH(DEX), SURVIVAL(WIS), THIEVERY(DEX); `Ability ability(); String id()` (lowercase name, equals the Pathbuilder key); `String displayName()` (English, e.g. "Athletics")
  - `enum Save { FORTITUDE(CON), REFLEX(DEX), WILL(WIS); Ability ability(); String id() }`

- [ ] **Step 1:** Add `implementation 'com.google.code.gson:gson:2.11.0'` to `core/build.gradle`; add `resources.srcDir '../core/src/main/resources'` to `sourceSets.main` in `fabric/build.gradle`.
- [ ] **Step 2:** Copy every `*.json` from `https://github.com/MrPrimate/pathmuncher/tree/main/tests/fixtures/pathbuilder` (including `edge/` and `generated/`, flattened, keep file names, prefix `edge-`/`generated-` for the subfolders) into `core/src/test/resources/pathbuilder/`; copy the repo's MIT `LICENSE` as `LICENSE-pathmuncher.txt`.
- [ ] **Step 3: Write the failing test** `BasicsTest`:

```java
assertEquals(4, Ability.modifier(18)); assertEquals(0, Ability.modifier(10)); assertEquals(-1, Ability.modifier(9));
assertEquals(0, Rank.UNTRAINED.bonus(5)); assertEquals(7, Rank.TRAINED.bonus(5)); assertEquals(9, Rank.EXPERT.bonus(5));
assertEquals(13, Rank.LEGENDARY.bonus(5)); assertEquals(Rank.EXPERT, Rank.fromPathbuilder(4));
assertThrows(IllegalArgumentException.class, () -> Rank.fromPathbuilder(3));
assertEquals(16, Skill.values().length); assertEquals(Ability.STR, Skill.ATHLETICS.ability());
assertEquals(Ability.DEX, Ability.fromPathbuilder("Dex"));
```

- [ ] **Step 4:** Run `./gradlew -PcoreOnly :core:test` → FAIL (classes missing).
- [ ] **Step 5:** Implement the four enums.
- [ ] **Step 6:** Run `./gradlew -PcoreOnly :core:test` → PASS.
- [ ] **Step 7:** Commit `Add core character enums, Gson and Pathbuilder test fixtures`.

---

### Task 2: CharacterSheet, CharacterMath, CharacterJson

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/character/{Lore,Strike,Feat,HitPoints,ArmorClass,CharacterSheet,CharacterMath,CheckId,CharacterJson}.java`
- Test: `core/src/test/java/de/pf2craft/core/character/CharacterMathTest.java`, `CharacterJsonTest.java`, `CheckIdTest.java`

**Interfaces:**
- Consumes: Task 1 enums.
- Produces:
  - `record Lore(String name, Rank rank)`
  - `record Feat(String name, String type, int level)` (`type` as Pathbuilder delivers, e.g. "Class Feat"; in-game level-up uses "Class Feat", "Skill Feat", "General Feat", "Ancestry Feat")
  - `record Strike(String name, String category, String damageDie, String damageType, Ability attackAbility, int otherAttackBonus, int otherDamageBonus)` – `category` is a proficiency key (`simple`, `martial`, `advanced`, `unarmed`); `otherAttackBonus` = item/rune part, `otherDamageBonus` = everything except the STR modifier
  - `record HitPoints(int ancestryHp, int classHpPerLevel, int bonusHp, int bonusHpPerLevel)`
  - `record ArmorClass(String armorCategory, OptionalInt dexCap, int itemBonus)` – `armorCategory` ∈ `unarmored|light|medium|heavy`
  - `record CharacterSheet(String name, String className, String ancestry, String heritage, String background, int level, Ability keyAbility, Map<Ability,Integer> abilityScores, Map<String,Rank> proficiencies, List<Lore> lores, HitPoints hitPoints, int speed, ArmorClass armorClass, List<Strike> strikes, List<Feat> feats, List<String> specials, int currentHp, boolean levelUpGranted)`
    - proficiency keys: `perception`, `fortitude`, `reflex`, `will`, `class_dc`, `unarmored`, `light`, `medium`, `heavy`, `simple`, `martial`, `advanced`, `unarmed`, plus every `Skill.id()`; a missing key means `UNTRAINED`
    - `Rank rank(String key)`; withers: `withCurrentHp(int)`, `withAbilityScore(Ability,int)`, `withRank(String,Rank)`, `withLevelUpGranted(boolean)`; constructor copies collections defensively (`Map.copyOf`, `List.copyOf`, `EnumMap` for scores)
  - `final class CharacterMath` (static): `int maxHp(CharacterSheet)`, `int armorClass(CharacterSheet)`, `int save(CharacterSheet, Save)`, `int perception(CharacterSheet)`, `int skill(CharacterSheet, Skill)`, `int lore(CharacterSheet, int index)`, `int classDc(CharacterSheet)`, `int strikeAttack(CharacterSheet, int index)`, `int strikeDamageBonus(CharacterSheet, int index)`, `int speedBlocks(CharacterSheet)` (= speed / 5), `OptionalInt checkBonus(CharacterSheet, CheckId)`
    - max HP = ancestryHp + bonusHp + level × (classHpPerLevel + CON mod + bonusHpPerLevel)
    - AC = 10 + min(DEX mod, dexCap) + rank(armorCategory).bonus(level) + itemBonus
    - lore = INT mod + rank bonus; class DC = 10 + keyAbility mod + rank(`class_dc`) bonus
    - strike attack = attackAbility mod + rank(category) bonus + otherAttackBonus; damage bonus = otherDamageBonus + (attackAbility == STR ? STR mod : 0)
  - `record CheckId(Kind kind, String key, int index)` with `enum Kind { SKILL, SAVE, PERCEPTION, LORE, STRIKE }`; `static Optional<CheckId> parse(String)` for `skill:<id>`, `save:<id>`, `perception`, `lore:<n>`, `strike:<n>`; `String format()`; `String label(CharacterSheet)` (English: "Athletics", "Reflex", "Perception", "Lore: Farming", strike name)
  - `final class CharacterJson`: `static String write(CharacterSheet)`, `static CharacterSheet read(String) throws CharacterImportException`; JSON object with field `"version": 1`

- [ ] **Step 1: Write failing tests.** Build the sheet of the Pathbuilder fighter by hand in a test helper `fighter5()` (level 5, STR 18, DEX 10, CON 16, INT 16, WIS 16, CHA 10; perception/fortitude/reflex/will EXPERT, athletics EXPERT, acrobatics TRAINED, HitPoints(8,10,2,0), speed 25, ArmorClass("heavy", OptionalInt.of(0), 6) with heavy TRAINED, strike `Lion Scythe` martial EXPERT, STR, other 0/0). Assert: `maxHp 75`, `armorClass 23`, `save FORTITUDE 12`, `save REFLEX 9`, `save WILL 12`, `perception 12`, `skill ATHLETICS 13`, `skill ACROBATICS 7`, `skill DECEPTION 0`, `strikeAttack(0) 13`, `strikeDamageBonus(0) 4`, `speedBlocks 5`. `CharacterJsonTest`: `read(write(fighter5())).equals(fighter5())`; `read("{}")` and `read("not json")` throw `CharacterImportException`. `CheckIdTest`: parse/format round trip for all five kinds; `parse("skill:flying")`, `parse("lore:-1")`, `parse("")` → empty.
- [ ] **Step 2:** Run `./gradlew -PcoreOnly :core:test` → FAIL.
- [ ] **Step 3:** Implement the records, `CharacterMath`, `CheckId`, `CharacterJson` (Gson with explicit field mapping – do not rely on Gson reflection on records).
- [ ] **Step 4:** Run tests → PASS.
- [ ] **Step 5:** Commit `Add character sheet model, derived values and JSON storage`.

---

### Task 3: Pathbuilder importer with parity tests

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/character/{PathbuilderImporter,CharacterImportException}.java`
- Test: `core/src/test/java/de/pf2craft/core/character/PathbuilderImporterTest.java`

**Interfaces:**
- Consumes: Task 2 model and math.
- Produces: `final class PathbuilderImporter { static CharacterSheet parse(String json) throws CharacterImportException }`; `class CharacterImportException extends Exception` (message English, shown to the player).

Mapping (from the spec appendix): `build.name/class/level/ancestry/heritage/background/keyability`; `abilities.<key>` → scores; `proficiencies.<key>` via `Rank.fromPathbuilder` (`classDC` → `class_dc`; ignore `casting*`, `piloting`, `computers`); `lores` `[name, value]`; `attributes` → `HitPoints(ancestryhp, classhp, bonushp, bonushpPerLevel)`, speed = `speed + speedBonus`; feats `[name, extra, type, level]` → `Feat(extra == null ? name : name + " (" + extra + ")", type, level)`; `specials` minus the literal `"Not Selected"`.

Armor class: `armorCategory` = `prof` of the first `armor[]` entry with `worn == true`, else `unarmored`; `itemBonus = acTotal.acItemBonus`; `dexCap` = `acTotal.acAbilityBonus` if it is lower than the DEX modifier, otherwise empty.

Strikes (algorithm the signature does not determine): for each `weapons[]` entry, `rankBonus = rank(prof).bonus(level)`. Try `attackAbility = STR`: `other = attack − STR mod − rankBonus`; if `0 ≤ other ≤ 4` take it, otherwise use `DEX` the same way; if neither fits, use the ability with the higher modifier and accept any `other`. `otherDamageBonus = damageBonus − (attackAbility == STR ? STR mod : 0)`. `damageDie` = `die`, `damageType` = `damageType`.

Validation: missing `build`, `success == false`, missing `name`/`class`/`level`/`abilities`/`proficiencies`, level outside 1–20, ability score outside 1–30, or more than 64 weapons/lores/200 feats → `CharacterImportException`. `dualClass` set → `CharacterImportException("Dual class characters are not supported yet")`. Fresh import: `currentHp = maxHp`, `levelUpGranted = false`.

- [ ] **Step 1: Write failing tests:**
  - `fighter5MatchesPathbuilder`: parse `422753-fighter5.json`; `maxHp 75`, `armorClass 23`, `skill ATHLETICS 13`, `save REFLEX 9`, `perception 12`, `lore index of "Farming"` rank TRAINED, `currentHp 75`, `levelUpGranted false`.
  - `armorClassMatchesEveryFixture` (parameterised over every fixture file): `CharacterMath.armorClass(sheet) == build.acTotal.acTotal`. If some fixtures differ for a reason the model intentionally ignores (e.g. shield or class feature bonus), list them in a `KNOWN_AC_EXCEPTIONS` set with a one-line reason each – no more than 10 % of fixtures.
  - `strikeAttackMatchesEveryFixture`: for each weapon, `strikeAttack == weapon.attack`.
  - `rejectsBrokenInput`: `{"success":false}`, `{}`, `"[]"`, a build with `level: 0`, and one with `dualClass: "Wizard"` each throw `CharacterImportException`.
- [ ] **Step 2:** Run tests → FAIL.
- [ ] **Step 3:** Implement `PathbuilderImporter.parse` with Gson `JsonParser`.
- [ ] **Step 4:** Run tests → PASS (investigate every AC mismatch before adding an exception).
- [ ] **Step 5:** Commit `Import characters from Pathbuilder 2e JSON`.

---

### Task 4: Class progression format, registry and Fighter data

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/progression/{ClassProgression,ClassProgressionParser,ClassRegistry}.java`
- Create: `core/src/main/resources/pf2craft/classes/index.json`, `fighter.json`
- Test: `core/src/test/java/de/pf2craft/core/progression/ClassProgressionTest.java`

**Interfaces:**
- Produces:
  - `record ClassProgression(String name, int hpPerLevel, Map<Integer, LevelEntry> levels)` with `record LevelEntry(Map<String,Rank> proficiencies, List<String> features, boolean skillIncrease, List<String> featSlots)`; `Map<String,Rank> ranksAt(int level)` (fixed ranks reached by that level, highest wins)
  - `final class ClassProgressionParser { static ClassProgression parse(String json) throws IllegalArgumentException }`
  - `final class ClassRegistry { ClassRegistry(Collection<ClassProgression>); Optional<ClassProgression> find(String className) /* case-insensitive */; int size() }`
  - Ancestry feats (1, 5, 9, 13, 17), general feats (3, 7, 11, 15, 19) and ability boosts (5, 10, 15, 20) are the same for every class: constants in `ClassProgression`, not in the JSON.

Class JSON format (exact):

```json
{
  "name": "Fighter",
  "hpPerLevel": 10,
  "levels": {
    "1": { "proficiencies": { "perception": "expert", "fortitude": "expert", "reflex": "expert", "will": "trained",
                              "simple": "expert", "martial": "expert", "advanced": "trained", "unarmed": "expert",
                              "unarmored": "trained", "light": "trained", "medium": "trained", "heavy": "trained",
                              "class_dc": "trained" },
           "features": ["Attack of Opportunity", "Shield Block"], "featSlots": ["Class Feat"] },
    "2": { "featSlots": ["Class Feat", "Skill Feat"] },
    "3": { "proficiencies": { "will": "expert" }, "features": ["Bravery"], "skillIncrease": true }
  }
}
```

`proficiencies` only lists fixed rank changes; choice-based ones (e.g. Fighter weapon mastery in one group) appear only as a feature name. Feature names may differ slightly from Pathbuilder's `specials`; that is fine.

- [ ] **Step 1:** Write `fighter.json` for levels 1–20 from the Remaster Fighter page on AoN (cross-check with the Foundry PF2e system's `classes/fighter.json`). Fixed changes to include: L3 will expert; L7 perception master; L9 fortitude master; L11 all armor + unarmored expert, class DC expert; L13 simple/martial master, advanced expert, unarmed master; L15 reflex master; L17 armor + unarmored master; L19 class DC master. Skill increases at 3, 5, …, 19; class feats at 1, 2, 4, …, 20; skill feats at 2, 4, …, 20. `index.json` = `["fighter.json"]`.
- [ ] **Step 2: Write failing tests:** parse `fighter.json`; `hpPerLevel 10`; `ranksAt(1).get("fortitude") == EXPERT`; `ranksAt(7).get("perception") == MASTER`; `ranksAt(6).get("perception") == EXPERT`; `levels.get(3).skillIncrease()`; `ClassRegistry.find("fighter")` present, `find("Wizard")` empty; `parse("{\"name\":\"X\"}")` throws.
- [ ] **Step 3:** Run tests → FAIL. **Step 4:** Implement. **Step 5:** Run tests → PASS.
- [ ] **Step 6:** Add `ClassDataConsistencyTest` (parameterised over every fixture whose class is in the registry): for `perception`, `fortitude`, `reflex`, `will`, `class_dc`: `progression.ranksAt(build.level)` rank ≤ fixture rank, and for the four saves/perception exactly equal unless listed in a per-class `KNOWN_RANK_EXCEPTIONS` set with a reason (feats like Canny Acumen raise them). Run → PASS.
- [ ] **Step 7:** Commit `Add class progression data format with Fighter`.

---

### Task 5: Level-up engine

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/progression/{LevelUp,LevelUpOptions,LevelUpChoices,InvalidChoiceException,UnsupportedClassException}.java`
- Test: `core/src/test/java/de/pf2craft/core/progression/LevelUpTest.java`

**Interfaces:**
- Consumes: `CharacterSheet`, `ClassRegistry`, `ClassProgression`.
- Produces:
  - `record LevelUpOptions(int newLevel, boolean skillIncrease, int abilityBoosts, List<String> featSlots, List<String> features, int hpGain)` – `abilityBoosts` is 4 on levels 5/10/15/20, else 0; `featSlots` = class slots + ancestry/general by the constants; `hpGain` = hpPerLevel + CON mod + bonusHpPerLevel
  - `record LevelUpChoices(Optional<String> skillIncrease /* Skill id or "lore:<n>" */, Set<Ability> boosts, List<String> featNames /* same order as featSlots */)`
  - `final class LevelUp`: `static LevelUpOptions options(CharacterSheet, ClassRegistry) throws UnsupportedClassException`; `static CharacterSheet apply(CharacterSheet, ClassRegistry, LevelUpChoices) throws UnsupportedClassException, InvalidChoiceException`
  - `UnsupportedClassException extends Exception` (class not in registry, or level already 20); `InvalidChoiceException extends Exception` (English message for the player)

Rules `apply` enforces: sheet must have `levelUpGranted`; level + 1; for each fixed rank in the new level entry set `max(current, fixed)`; skill increase required iff `skillIncrease`, raises one step, target MASTER needs new level ≥ 7, LEGENDARY needs ≥ 15, no increase beyond LEGENDARY; boosts: exactly `abilityBoosts` distinct abilities, each +2 if score < 18 else +1; feat names: one non-blank name (≤ 64 chars) per slot, added as `Feat(name, slot, newLevel)`; new features appended to `specials`; `currentHp += hpGain` (computed after boosts, so a CON boost counts); `levelUpGranted = false`.

- [ ] **Step 1: Write failing tests** using the imported `422753-fighter5.json` with `withLevelUpGranted(true)`:
  - `optionsForLevel6`: `newLevel 6`, `skillIncrease false`, `abilityBoosts 0`, `featSlots ["Class Feat", "Skill Feat"]`, `hpGain 13`.
  - `levelSevenRaisesPerception`: apply 6 (feats "A", "B"), grant again, apply 7 with skill increase `athletics` (EXPERT → MASTER allowed at 7) and slots filled → `perception` MASTER, `athletics` MASTER, `maxHp 101`, `currentHp 101`, level 7.
  - `masterSkillNeedsLevel7`: at level 6 → 7 is fine, but a sheet at level 2 → 3 choosing an EXPERT skill → `InvalidChoiceException`.
  - `boostsAtLevel10`: from the fighter5 sheet rebuilt with level 9 via the record constructor → options `abilityBoosts 4`; choosing STR (18 → 19) and CON (16 → 18) gives those scores; choosing 3 or duplicate abilities → `InvalidChoiceException`.
  - `requiresGrant`: without `levelUpGranted` → `InvalidChoiceException`.
  - `unknownClassAndLevel20`: fighter5 rebuilt with class name `"Homebrew"` → `UnsupportedClassException`; fighter5 rebuilt with level 20 → `UnsupportedClassException`. (Do not use a real class here – later tasks add data for all of them.)
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add in-game level-up rules engine`.

---

### Task 6: Server storage, class data loading, Pathbuilder fetch, join handling

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/character/{CharacterStore,ClassData,PathbuilderId,PathbuilderFetcher,CharacterService}.java`
- Create: `fabric/src/main/java/de/pf2craft/network/{ImportCharacterPayload,OpenImportPayload}.java`
- Modify: `Pf2Craft.java`, `Pf2Networking.java`, both lang files
- Test: `core`-side tests are not possible for Fabric classes; `PathbuilderId` and the fetcher's response handling are pure Java → put them in `core`: `core/src/main/java/de/pf2craft/core/character/PathbuilderId.java` and `PathbuilderResponse.java`, tested in `core/src/test/.../PathbuilderIdTest.java`

**Interfaces:**
- Consumes: `CharacterJson`, `PathbuilderImporter`, `ClassRegistry`, `ClassProgressionParser`.
- Produces:
  - `final class PathbuilderId { static Optional<String> parse(String input) }` – trims, accepts 1–10 digits only
  - `final class PathbuilderResponse { static CharacterSheet toSheet(int httpStatus, String body) throws CharacterImportException }` – non-200 → "Pathbuilder answered with status N"; body > 2 MB → error; else `PathbuilderImporter.parse`
  - `CharacterStore`: `static void register()`; `static Optional<CharacterSheet> get(ServerPlayerEntity)`; `static void set(ServerPlayerEntity, CharacterSheet)`; `static void remove(ServerPlayerEntity)`. Attachment `pf2craft:character`, type `String` (sheet JSON), `.persistent(Codec.STRING).copyOnDeath()`; reading uses `CharacterJson.read`, a corrupt value logs a warning and counts as "no character".
  - `ClassData`: `static ClassRegistry registry()`; `static void load(MinecraftServer)` on `ServerLifecycleEvents.SERVER_STARTING` – reads `/pf2craft/classes/index.json` from the jar, then every `*.json` in `<server dir>/config/pf2craft/classes/` (same name overrides jar class); bad files log an error and are skipped.
  - `PathbuilderFetcher`: `static CompletableFuture<CharacterSheet> fetch(String id)` – `HttpClient` (connect timeout 5 s, request timeout 10 s), `GET https://pathbuilder2e.com/json.php?id=<id>`, body via `PathbuilderResponse.toSheet`; network errors → `CharacterImportException("Pathbuilder is not reachable")`.
  - `CharacterService`: `static void onJoin(ServerPlayerEntity)`; `static void startImport(ServerPlayerEntity, String rawId)`; in-flight guard `Set<UUID>`; completion runs via `server.execute(...)`, looks the player up again by UUID (may have left).
  - Payloads: `ImportCharacterPayload(String pathbuilderId)` C2S, `PacketCodecs.string(32)`; `OpenImportPayload()` S2C (unit codec).

Behaviour: join → if not GM and no character: `changeGameMode(GameMode.SPECTATOR)`, chat hint `pf2craft.character.need_import`, send `OpenImportPayload` if `ServerPlayNetworking.canSend`. Import success → store sheet; if the player had no character before: `changeGameMode(GameMode.SURVIVAL)`; message `pf2craft.character.imported` to the player; every online GM gets `pf2craft.character.imported_gm` ("%s imported %s, level %s"); `Pf2Craft.LOGGER.info`. Failure → `pf2craft.character.import_failed` with the exception message. Second request while in flight → `pf2craft.character.import_busy`.

- [ ] **Step 1: Write failing core tests** `PathbuilderIdTest`: `"422753"` → present; `" 422753 "` → `"422753"`; `""`, `"abc"`, `"12 34"`, `"12345678901"` → empty. `PathbuilderResponseTest`: status 500 → exception; `"<html>"` → exception; a 3 MB string → exception; fixture body with 200 → sheet named `August [Kholo, F]`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement the two core classes. **Step 4:** Run → PASS.
- [ ] **Step 5:** Implement the Fabric classes and payloads; register store, class loading, join event (`ServerPlayConnectionEvents.JOIN`) and the import receiver in `Pf2Craft`/`Pf2Networking`; add the lang keys in both files.
- [ ] **Step 6:** Run `./gradlew build` → BUILD SUCCESSFUL.
- [ ] **Step 7: Manual check** with `./gradlew :fabric:runClient`, new creative world with cheats: player becomes spectator on join; `/pf2 gm add @s` not yet relevant. (The import screen arrives in Task 8; for now test via the fallback command added in Step 8.)
- [ ] **Step 8:** Add `/pf2 character import <id>` (any player, for their own character) and `/pf2 character reset <player>` (GM or permission level 2; removes the sheet and sets the target to spectator) to `Pf2Commands`. Manually: `/pf2 character import 422753` → survival mode, chat confirmation; rejoin → stays survival; `/pf2 character reset @s` → spectator.
- [ ] **Step 9:** Commit `Store characters on the server and import them from Pathbuilder`.

---

### Task 7: Sheet sync, click-to-roll, GM edits, level-up on the server

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/network/{SheetRequestPayload,SheetSyncPayload,CheckRollPayload,GmEditPayload,GrantLevelUpPayload,LevelUpPayload}.java`
- Modify: `CharacterService.java`, `Pf2Networking.java`, `Pf2Commands.java`, lang files
- Create (core, pure logic of GM edits): `core/src/main/java/de/pf2craft/core/character/GmEdit.java` + `core/src/test/.../GmEditTest.java`

**Interfaces:**
- Consumes: Task 6 services, `CheckId`, `CharacterMath`, `LevelUp`, `RollService.roll(...)`.
- Produces:
  - C2S `SheetRequestPayload(UUID target)` – own sheet always; others only for GMs
  - S2C `SheetSyncPayload(UUID owner, String sheetJson, boolean gmView, boolean classSupported, boolean open)` – `string(262144)`; `classSupported` = class in registry and level < 20
  - C2S `CheckRollPayload(String checkId, boolean secret)` – `string(32)`, own character only
  - C2S `GmEditPayload(UUID target, String field, int value)` – fields: `hp` (absolute current HP, clamped 0..maxHp), `ability:<str..cha>` (1–30), `rank:<proficiency key>` (0–4 = rank ordinal); GM only
  - C2S `GrantLevelUpPayload(UUID target)` – GM only
  - C2S `LevelUpPayload(String skillIncrease, List<String> boosts, List<String> featNames)` – own character; lists max 8 entries, strings max 64
  - `final class GmEdit { static CharacterSheet apply(CharacterSheet, String field, int value) throws InvalidChoiceException }`
  - Commands: `/pf2 hp <player> <amount>` (relative, e.g. `-8`; GM or permission 2), `/pf2 sheet <player>` (GM: sends `SheetSyncPayload` with `gmView=true, open=true`)

Behaviour: every change to a sheet (import, edit, grant, level-up, hp command) re-sends `SheetSyncPayload(open=false)` to the owner and to every GM who requested that sheet in this session (track `Map<UUID, Set<UUID>> viewers`, cleared on disconnect). Check roll: resolve `CheckId.parse`, bonus via `CharacterMath.checkBonus`; invalid → ignore; then `RollService.roll(server, player, player.getName().getString(), "1d20" + signed(bonus), CheckId.label(sheet), secret)` using the existing cooldown. Grant: set `levelUpGranted`, tell the owner `pf2craft.character.levelup_granted`. Level-up: map payload → `LevelUpChoices`, `LevelUp.apply`, store, message `pf2craft.character.leveled` to owner and GMs; exceptions → their message in red. Offline target → `pf2craft.character.player_offline`.

- [ ] **Step 1: Write failing core test** `GmEditTest`: `hp` 200 on fighter5 → 75 (clamped), `hp` -5 → 0; `ability:str` 20 → STR 20; `ability:str` 31 → exception; `rank:athletics` 3 → MASTER; `rank:flying` 1 → exception; `mana` → exception.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement `GmEdit`. **Step 4:** Run → PASS.
- [ ] **Step 5:** Implement payloads, receivers and commands; lang keys in both files.
- [ ] **Step 6:** `./gradlew build` → SUCCESS.
- [ ] **Step 7: Manual check** (runClient, imported fighter): `/pf2 hp @s -8` → chat confirmation; `/pf2 gm add @s`, `/pf2 sheet @s` → no crash (screen arrives in Task 8).
- [ ] **Step 8:** Commit `Sync sheets, roll checks and handle GM edits and level-ups on the server`.

---

### Task 8: Client – import screen and character sheet

**Files:**
- Create: `fabric/src/client/java/de/pf2craft/client/{ClientSheets,ImportScreen,CharacterScreen}.java`
- Modify: `Pf2CraftClient.java`, lang files

**Interfaces:**
- Consumes: payloads from Tasks 6–7, `CharacterJson`, `CharacterMath`, `CheckId`.
- Produces: `ClientSheets` (`static void put(SheetSyncPayload)`, `static Optional<Synced> get(UUID)`, record `Synced(CharacterSheet sheet, boolean gmView, boolean classSupported)`); `CharacterScreen(UUID owner)`; `ImportScreen()`.

UI (spec: tabs, plain vanilla look, readable at GUI scale 3 on 1080p, i.e. ~640×360 scaled pixels):
- Key **C** (`key.pf2craft.sheet`) → if own sheet cached open `CharacterScreen`, else send `SheetRequestPayload(own uuid)` and open on sync; no character → `ImportScreen`.
- `OpenImportPayload` → open `ImportScreen`: text field (digits, max 10), button „Import“, status line; closes on the next sync for the own UUID.
- `CharacterScreen`: header (name, ancestry, class, level, AC, HP current/max); vanilla buttons as tabs: **Overview** (6 ability modifiers, saves, perception, speed "25 ft (5 blocks)", strikes with attack and damage), **Skills** (16 skills + lores, rank letter U/T/E/M/L, bonus, scrollable), **Feats** (feats with level, then specials). Every check row is a button that sends `CheckRollPayload`. A **Secret** toggle in the footer applies to all clicks. Footer buttons: „Re-import“ (opens `ImportScreen`), „Level up“ (visible when `levelUpGranted` and `classSupported`; Task 9 opens `LevelUpScreen`; if granted but not supported, show disabled with tooltip `screen.pf2craft.sheet.levelup_unsupported`).
- Receiving a sync with `open=true` opens the screen for that owner.

- [ ] **Step 1:** Implement `ClientSheets`, receivers, key binding, `ImportScreen`, `CharacterScreen`; lang keys in both files (German labels: Übersicht, Fertigkeiten, Talente, Verdeckt, Importieren, Aufsteigen …).
- [ ] **Step 2:** `./gradlew build` → SUCCESS.
- [ ] **Step 3: Manual check** (runClient, fresh world): join → spectator + import screen; enter `422753` → survival, sheet opens with AC 23, HP 75/75; tab through all three tabs; click Athletics → chat shows `1d20+13`; Secret on → `[Secret]`; press C again → sheet; switch game language to Deutsch → labels German.
- [ ] **Step 4:** Commit `Add character sheet and import screens`.

---

### Task 9: Client – GM view and level-up screen

**Files:**
- Create: `fabric/src/client/java/de/pf2craft/client/LevelUpScreen.java`
- Modify: `CharacterScreen.java`, lang files

**Interfaces:**
- Consumes: `GmEditPayload`, `GrantLevelUpPayload`, `LevelUpPayload`, `LevelUp.options` (computed client-side from the synced sheet + a client copy of the class registry is **not** available – so the server sends options: extend `SheetSyncPayload` with `String levelUpOptionsJson` (empty when not granted/unsupported), produced from `LevelUpOptions` via a `LevelUpOptions.toJson()/fromJson()` pair added in core with a round-trip test).

GM view (`gmView=true`): HP shown with − / + buttons (Shift = ±5) and a „Set“ text field; on the Overview tab each ability has − / +; on the Skills tab clicking the rank letter cycles the rank (sends `rank:<key>`); a „Grant level up“ button. In GM view of another player's sheet the check rows are display-only (the server only rolls for the sender's own character).

`LevelUpScreen`: shows new level, HP gain, features; one cycle-button for the skill increase (only skills/lores that may be raised at the new level), four toggle buttons for boosts when required, one text field per feat slot labelled with the slot type; „Confirm“ disabled until complete; sends `LevelUpPayload`; closes on the next sync.

- [ ] **Step 1:** Add `LevelUpOptions.toJson/fromJson` in core with a round-trip test; run core tests → PASS.
- [ ] **Step 2:** Extend `SheetSyncPayload`, implement GM controls and `LevelUpScreen`; lang keys in both files.
- [ ] **Step 3:** `./gradlew build` → SUCCESS.
- [ ] **Step 4: Manual check:** `/pf2 gm add @s`, `/pf2 sheet @s` → GM view; HP − → 74 in header; cycle Athletics to legendary and back; „Grant level up“ → own sheet shows „Level up“; complete 5 → 6 with two feat names → level 6, HP max 88; `/pf2 gm remove @s`, open own sheet → no GM controls. With a second client (`runClient` twice, offline names), open the other player's sheet as GM, let that player disconnect, click HP − → "player not online", no crash.
- [ ] **Step 5:** Commit `Add GM sheet editing and level-up screen`.

---

### Task 10: Class data – Player Core

**Files:**
- Create: `core/src/main/resources/pf2craft/classes/{bard,cleric,druid,ranger,rogue,witch,wizard}.json`; modify `index.json`

- [ ] **Step 1:** For each class write levels 1–20 in the Task 4 format from the Remaster class page on AoN, cross-checked with the Foundry PF2e system data. Mind class-specific skill increases (Rogue: every level from 2) and skill feats (Rogue: every level). Spellcasting proficiencies (`spell_attack`/`spell_dc`) are out of scope – list them as features only.
- [ ] **Step 2:** Run `./gradlew -PcoreOnly :core:test` → `ClassDataConsistencyTest` now covers these classes' fixtures; every exception needs a reason → PASS.
- [ ] **Step 3:** Commit `Add Player Core class progressions`.

### Task 11: Class data – Player Core 2

Same as Task 10 for `alchemist, barbarian, champion, monk, sorcerer`. Commit `Add Player Core 2 class progressions`.

### Task 12: Class data – remaining Pathbuilder classes

- [ ] **Step 1:** Open `https://pathbuilder2e.com/app.html`, new character, open the class picker, and note every class offered (with and without „Core Only“). Expected at least: investigator, oracle, psychic, summoner, swashbuckler, thaumaturge, magus, gunslinger, inventor, kineticist, animist, exemplar, commander, guardian.
- [ ] **Step 2:** Same as Task 10 for each, using the newest published version of each class.
- [ ] **Step 3:** Add `allPathbuilderClassesHaveData`: the list from Step 1 as a constant; every entry is found in the registry. Run core tests → PASS.
- [ ] **Step 4:** Commit `Add progressions for all remaining Pathbuilder classes`.

---

### Task 13: Docs and end-to-end check

**Files:** `README.md`, `CLAUDE.md`

- [ ] **Step 1:** README (German, for players/admins): section „Charaktere“ – first join as spectator, Pathbuilder export ID, key C, tabs, click-to-roll, GM view (`/pf2 sheet`, `/pf2 hp`, `/pf2 character reset`, grant level-up), `config/pf2craft/classes/` for own classes; command table updated.
- [ ] **Step 2:** CLAUDE.md: project structure (`core.character`, `core.progression`, `fabric/.../character`), roadmap Phase 2 ✅ with open items (guided creation = Schritt B, multiplayer test), mark the Pathbuilder decision as confirmed.
- [ ] **Step 3:** `./gradlew -PcoreOnly :core:test` and `./gradlew build` → both SUCCESS.
- [ ] **Step 4: Manual end-to-end** in runClient: fresh world → spectator → import → roll → GM grant → level up → reset. Note any finding.
- [ ] **Step 5:** Commit `Document Phase 2 characters`.
