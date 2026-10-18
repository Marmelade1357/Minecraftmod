# Phase 3 Teil 2 – NPCs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The GM can spawn Monster Core creatures as villager-shaped NPCs, select them with a command staff, send them walking, view and edit their stats, rename them and save edited stats as server templates.

**Architecture:** Monster data, NPC stat instances, GM edits and template file naming live in `core` with JUnit tests. A custom villager-based entity carries an `NpcStats` JSON blob in its NBT and does nothing on its own. The GM's command staff item drives everything through C2S payloads; the server validates the GM role and applies changes, then syncs. Combat use of NPCs (initiative, attacks, damage) is Teil 3.

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn 1.21.1+build.3, Fabric API 0.116.6+1.21.1), Gson, JUnit 5; data source Foundry PF2e system (`foundryvtt/pf2e`, Monster Core pack), checked against https://2e.aonprd.com/Monsters.aspx.

**Spec:** `docs/superpowers/specs/2026-10-18-phase3-kampf-design.md` (German) — „Teil 2 – NPCs“, „Entscheidungen von Claude nach dem Builder-Check“ (NPC-Züge, Daten, NPC-Namen), „Rahmenbedingungen“ (Lizenz).

## Global Constraints

- Rules/data logic in `core`, no `net.minecraft`/`net.fabricmc` imports; new logic always with tests.
- Server authoritative; every C2S payload validated (GM role, lengths, ranges); client-only code under `fabric/src/client`.
- New text keys in **both** `en_us.json` and `de_de.json`; messages via `Text.translatableWithFallback`.
- **GM tools are items**, not buttons (spec). Commands only as fallback, like `/roll`.
- NPC look = **Villager** placeholder with name tag; NPCs do **nothing** on their own (no AI wandering, no trading, no vanilla damage, no despawn).
- Monster data: **Monster Core (Remaster) only**, values and names only, **no description text**; ability names only. Golarion names allowed (Community Use Policy, see `NOTICE.md`).
- GM can edit every NPC's values, either for this NPC only or saved as a template (JSON in the server config folder, like classes and items).
- Players never see NPC HP numbers or AC (spec); in Teil 2 the name tag shows only the name.
- NPC at 0 HP is **defeated**: falls over, takes no part, stays until the GM removes it.
- Windows dev: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"; export PATH="$JAVA_HOME/bin:$PATH"`; `./gradlew.bat -PcoreOnly :core:test` before every commit, plus `./gradlew.bat build` after Fabric changes.

## Review Focus

1. GM searches „kobold“, „Kobold Warrior“, „KOBOLD“ or „warr“ → the same creatures are found (Task 1: `searchIsCaseInsensitiveAndPartial`).
2. A template saved under a name like `../../evil` or `Kobold/Warrior` must not escape `config/pf2craft/monsters/` (Task 3: `templateFileNameIsSanitised`).
3. Server restart → every NPC is still there with its edited stats, current HP, custom name and defeated state (Task 4 bot check: restart server, re-read via `/pf2 npc info`).
4. A non-GM player holding a command staff, or sending NPC payloads with a modded client → nothing happens, red message (Task 5: server checks `GmRoles.isGm` in every receiver; bot check with a non-GM).
5. GM sets an NPC's HP to 0 → it lies down and counts as defeated; sets HP above 0 again → it stands up (Task 3: `NpcStats.defeated()` test; Task 4 bot check).

---

## File Structure

```
core/src/main/java/de/pf2craft/core/monster/
    DamagePart.java        record(dice, bonus, type)
    MonsterStrike.java     record(name, attackBonus, damage, traits, ranged)
    MonsterData.java       record: one Monster Core creature
    MonsterParser.java     JSON array → List<MonsterData> (strict)
    MonsterCatalog.java    lookup by id, search by text
    NpcStats.java          instance: MonsterData + currentHp + displayName
    NpcEdit.java           GM edits on NpcStats
    NpcJson.java           NpcStats ⇄ JSON (for entity NBT and templates)
    TemplateNames.java     safe file names for templates
core/src/main/resources/pf2craft/monsters/monster-core.json
core/src/test/java/de/pf2craft/core/monster/…Test.java
fabric/src/main/java/de/pf2craft/npc/
    MonsterDataLoader.java jar + config/pf2craft/monsters/*.json (templates override by id)
    NpcEntity.java         villager subclass, no brain, carries NpcStats
    Pf2Entities.java       EntityType registration + attributes
    NpcService.java        spawn, move, edit, rename, remove, save template, info
    CommandStaffItem.java  the GM item
    GmSelection.java       per-GM selected NPC (server memory)
fabric/src/main/java/de/pf2craft/network/
    NpcSearchPayload, NpcSearchResultPayload, NpcSpawnPayload, NpcSheetPayload,
    NpcEditPayload, NpcActionPayload, OpenNpcScreenPayload
fabric/src/client/java/de/pf2craft/client/
    NpcSpawnScreen.java, NpcSheetScreen.java, (renderer registration in Pf2CraftClient)
```

---

### Task 1: Core monster model, parser and catalog

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/monster/{DamagePart,MonsterStrike,MonsterData,MonsterParser,MonsterCatalog}.java`
- Test: `core/src/test/java/de/pf2craft/core/monster/MonsterCatalogTest.java`

**Interfaces:**
- Produces:
  - `record DamagePart(String dice, int bonus, String type)` — `dice` like `"1d6"`, `"2d8"` or `""` (flat damage); `type` a lowercase PF2 damage type (`bludgeoning`, `piercing`, `slashing`, `fire`, `cold`, `electricity`, `acid`, `sonic`, `vitality`, `void`, `force`, `spirit`, `mental`, `poison`, `bleed`, `precision`, `untyped`)
  - `record MonsterStrike(String name, int attackBonus, List<DamagePart> damage, List<String> traits, boolean ranged)`
  - `record Resistance(String type, int value)` (used for both resistances and weaknesses)
  - `record MonsterData(String id, String name, int level, String size, List<String> traits, int perception, int ac, int fortitude, int reflex, int will, int hp, int speed, List<MonsterStrike> strikes, List<Resistance> resistances, List<Resistance> weaknesses, List<String> immunities, Map<String,Integer> skills, List<String> abilities)` — `size` ∈ `tiny|small|medium|large|huge|gargantuan`; `speed` = land speed in feet; `abilities` = names only
  - `final class MonsterParser { static List<MonsterData> parse(String json) }` — JSON array of objects with exactly these field names (nested objects for strikes/damage/resistances); strict like `ItemCatalogParser`: missing/mistyped field, unknown size or damage type, level outside −1..25, negative ac/hp/speed, malformed dice (`^\d+d(4|6|8|10|12)$` or empty), duplicate `id` → `IllegalArgumentException` naming the `id` and field.
  - `final class MonsterCatalog { MonsterCatalog(List<MonsterData>); Optional<MonsterData> byId(String id); List<MonsterData> search(String text, int limit); int size() }` — later list entries replace earlier ones with the same `id`; `search`: case-insensitive (Locale.ROOT), every whitespace-separated word of `text` must be contained in the name; sort by level, then name; empty text → first `limit` by level/name.

- [ ] **Step 1: Write failing tests** `MonsterCatalogTest` with a 3-monster test JSON (`kobold-warrior` level −1 / `kobold-scout` level 1 / `goblin-warrior` level −1):
  - `parsesFullEntry`: all fields of kobold-warrior read back (one strike with two damage parts, one weakness, one immunity, two skills).
  - `searchIsCaseInsensitiveAndPartial`: `search("kobold",10)`, `search("KOBOLD",10)` → 2 results, kobold-warrior first (lower level); `search("warr",10)` → kobold-warrior and goblin-warrior; `search("kobold warrior",10)` → only kobold-warrior; `search("",2)` → 2 results.
  - `laterEntriesReplaceById`: second list with `kobold-warrior` hp 99 → `byId("kobold-warrior").hp() == 99`.
  - `rejectsBrokenEntries`: `size: "colossal"`, `level: 30`, `dice: "3d7"`, damage type `"lava"`, missing `ac`, duplicate id → each throws, message contains the id.
- [ ] **Step 2:** Run `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL.
- [ ] **Step 3:** Implement with explicit Gson reads.
- [ ] **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add monster data model, parser and catalog`.

---

### Task 2: Monster Core data

**Files:**
- Create: `core/src/main/resources/pf2craft/monsters/monster-core.json`
- Test: `core/src/test/java/de/pf2craft/core/monster/MonsterDataConsistencyTest.java`

**Interfaces:**
- Consumes: `MonsterParser`, `MonsterCatalog` (Task 1).
- Produces: classpath resource `/pf2craft/monsters/monster-core.json`.

- [ ] **Step 1:** Generate from the Foundry PF2e repository's Monster Core pack (`packs/` folder for "Pathfinder Monster Core"; actors of type `npc`). Mapping: `system.details.level.value`; `system.traits.size.value` (tiny/sm/med/lg/huge/grg → tiny/small/medium/large/huge/gargantuan); `system.traits.value`; `system.perception.mod`; `system.attributes.ac.value`; `system.saves.{fortitude,reflex,will}.value`; `system.attributes.hp.max`; `system.attributes.speed.value`; strikes = embedded items of type `melee` (`system.bonus.value`, `system.damageRolls` → DamagePart per roll, parsing `"2d6+4"` into dice and bonus; `system.traits.value`; ranged if traits contain `range-*` or `range-increment-*` or `thrown-*`); `system.attributes.resistances`, `weaknesses` (`type`,`value`), `immunities` (`type`); skills from embedded `lore` items (`name` → `system.mod.value`, key lowercased); abilities = names of embedded `action` items. No `description`/`publicNotes`. Converter is a throwaway script in the scratchpad, not committed. Sort by `id` (Foundry slug of the name).
- [ ] **Step 2: Write the failing test** `MonsterDataConsistencyTest`: file parses; `size() ≥ 350`; spot values — pick **five** creatures, one each of level −1, 1, 4, 10 and 20 (Kobold Warrior for −1; the others your choice, e.g. an ogre, a young and an ancient dragon), fetch their AoN Monster pages and assert level, AC, HP, Fortitude, Perception and the first strike's attack bonus as literals; record the AoN URLs in a comment. Every monster has ≥ 1 strike or is listed in a `NO_STRIKES` set (≤ 5 % of entries) with reason; no ability name longer than 64 chars; no field contains the substring `@UUID` or `<p>` (no leaked description markup).
- [ ] **Step 3:** Run → fix data/converter until PASS.
- [ ] **Step 4:** Commit `Add Monster Core creature data`.

---

### Task 3: NPC stat instances, GM edits, templates

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/monster/{NpcStats,NpcEdit,NpcJson,TemplateNames}.java`
- Test: `core/src/test/java/de/pf2craft/core/monster/{NpcEditTest,NpcJsonTest,TemplateNamesTest}.java`

**Interfaces:**
- Consumes: `MonsterData`, `MonsterStrike`, `Resistance` (Task 1). Reuses `de.pf2craft.core.progression.InvalidChoiceException` for rejected edits (as `GmEdit` does).
- Produces:
  - `record NpcStats(MonsterData base, String displayName, int currentHp)` with `boolean defeated()` (`currentHp <= 0`), `static NpcStats fresh(MonsterData base)` (displayName = base name, currentHp = base hp).
  - `final class NpcEdit { static NpcStats apply(NpcStats stats, String field, String value) throws InvalidChoiceException }` — fields: `hp` (current, clamped 0..max hp), `maxhp` (1..9999; current clamped down), `ac`, `fortitude`, `reflex`, `will`, `perception` (−10..99), `speed` (0..300, multiples of 5), `level` (−1..25), `name` (1..32 chars, no `§`), `strike:<i>:bonus` (−10..99). Edits on `base` produce a new `MonsterData` (records are immutable). Unknown field / bad number → `InvalidChoiceException` with English message.
  - `final class NpcJson { static String write(NpcStats); static NpcStats read(String) throws IllegalArgumentException; static String writeTemplate(MonsterData); }` — template format = one-element JSON array in the `MonsterParser` format so `MonsterParser.parse` reads it.
  - `final class TemplateNames { static String fileName(String id) }` — lowercase, `[a-z0-9-]` only (others → `-`, collapse repeats, trim `-`), max 64 chars, empty → `"template"`; returns `<name>.json`.
- [ ] **Step 1: Write failing tests:**
  - `NpcEditTest`: hp 999 → clamped to max; hp −5 → 0 and `defeated()`; hp back to 3 → not defeated; `maxhp` 5 when current 8 → current 5; `ac` 25 → base.ac 25; `speed` 27 → throws; `name` `"§cX"` → throws; `name` `"Kobold 1"` → displayName; `strike:0:bonus` 9 → strike 0 bonus 9; `strike:7:bonus` on a 1-strike monster → throws; `mana` → throws.
  - `NpcJsonTest`: round trip of an edited NpcStats equals; `MonsterParser.parse(writeTemplate(base))` equals `List.of(base)`; `read("{}")` throws.
  - `TemplateNamesTest.templateFileNameIsSanitised`: `"../../evil"` → `"evil.json"`; `"Kobold/Warrior"` → `"kobold-warrior.json"`; `"   "` → `"template.json"`; a 200-char name → ≤ 69 chars total.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add NPC stat instances, GM edits and template naming`.

---

### Task 4: NPC entity, data loading and fallback commands

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/npc/{MonsterDataLoader,NpcEntity,Pf2Entities,NpcService}.java`
- Modify: `Pf2Craft.java` (register entity + attributes, load monsters on SERVER_STARTING), `Pf2CraftClient.java` (register `VillagerEntityRenderer` for the NPC type), `Pf2Commands.java` (`/pf2 npc spawn <id>`, `/pf2 npc info`, `/pf2 npc remove` — GM or permission 2; act on the NPC the executor looks at within 16 blocks, or the GM's selected NPC once Task 5 exists), both lang files

**Interfaces:**
- Consumes: Task 1–3 core classes.
- Produces:
  - `MonsterDataLoader`: `static MonsterCatalog catalog()`, `static void load(MinecraftServer)` — jar `monster-core.json`, then every `config/pf2craft/monsters/*.json` (templates; replace by `id`); bad files logged and skipped.
  - `Pf2Entities.NPC` = `EntityType<NpcEntity>` id `pf2craft:npc`, villager dimensions.
  - `NpcEntity extends VillagerEntity`: holds `NpcStats stats`; persisted in `writeCustomDataToNbt`/`readCustomDataFromNbt` as `NpcJson` string under key `pf2craft_npc`; brain/AI activities never run (override `mobTick` without brain ticking; navigation still works for Task 5); `interactMob` → `ActionResult.PASS` (no trading screen); `isInvulnerableTo` true for all vanilla damage except `/kill` (out of world / generic kill); `canImmediatelyDespawn` false, persistent; custom name = `stats.displayName()`, name always visible; `defeated()` → entity pose `SLEEPING` (lying), otherwise standing; `setStats(NpcStats)` updates name and pose.
  - `NpcService`: `static NpcEntity spawn(ServerPlayerEntity gm, MonsterData data)` (1 block in front of the GM, facing them), `static void apply(NpcEntity npc, NpcStats stats)`, `static void remove(NpcEntity npc)`, `static Text info(NpcEntity npc)` (GM-only text: name, monster, level, HP x/y, AC, saves, perception, speed, strikes).
- [ ] **Step 1:** Implement loader, entity, registration, renderer registration and the fallback commands; lang keys in both files.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 3: Bot check** (dedicated server + mineflayer in the scratchpad, as before; op the bot and give it the GM tag via `/pf2 gm add`): `/pf2 npc spawn kobold-warrior` → an entity appears (bot sees an entity of type `pf2craft:npc` or a villager-like entity with name „Kobold Warrior“); right-clicking it opens no trade window; `/pf2 npc info` → line with HP and AC; attack it with a sword → no damage (`/pf2 npc info` HP unchanged); stop and restart the server → NPC still there, info unchanged; edit HP to 0 via a temporary test path is not available yet, so check the lying pose in Task 5. Record transcript. Stop the server; commit nothing from `fabric/run` or the scratchpad.
- [ ] **Step 4:** Commit `Add villager-based NPC entity with Monster Core stats`.

---

### Task 5: Command staff and NPC screens

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/npc/{CommandStaffItem,GmSelection}.java`, payloads in `fabric/src/main/java/de/pf2craft/network/` (`NpcSearchPayload`, `NpcSearchResultPayload`, `NpcSpawnPayload`, `NpcSheetPayload`, `NpcEditPayload`, `NpcActionPayload`, `OpenNpcScreenPayload`), `fabric/src/client/java/de/pf2craft/client/{NpcSpawnScreen,NpcSheetScreen}.java`
- Modify: `Pf2Networking.java`, `Pf2Craft.java` (item registration), `Pf2CraftClient.java`, `Pf2Commands.java` (`/pf2 tools` gives the GM their items — GM only), `GmRoles.setGm` (when a player becomes GM, give the staff), lang files

**Interfaces:**
- Consumes: Task 4 `NpcService`, `NpcEntity`, `MonsterDataLoader`; Task 3 `NpcEdit`, `NpcJson`, `TemplateNames`.
- Produces:
  - Item `pf2craft:command_staff` (vanilla look: `blaze_rod` model via item model JSON pointing to `minecraft:item/blaze_rod`, custom name „Befehlsstab“/„Command Staff“, max stack 1). Only GMs can use it; for others: red message, nothing else.
  - Staff interactions (all decided on the server):
    - right-click **air** → server sends `OpenNpcScreenPayload("spawn")`; client opens `NpcSpawnScreen`.
    - right-click an **NPC** → select it (`GmSelection.select(gm, npc)`); feedback as action-bar text „Ausgewählt: Kobold 1“ *(plan decision: vanilla glowing would be visible to all players)*.
    - **sneak** + right-click an NPC → `NpcSheetPayload` with `NpcJson.write(stats)` + entity id; client opens `NpcSheetScreen`.
    - right-click a **block** with an NPC selected → NPC walks there (`getNavigation().startMovingTo(x, y+1, z, 0.6)`), outside combat without action limits.
    - left-click (attack) an NPC with the staff → nothing (no damage).
  - `NpcSpawnScreen`: search field + scrollable list (name, level), results requested from the server via `NpcSearchPayload(text)` → `NpcSearchResultPayload(List<entry id,name,level>)` (max 50 results, request debounce 300 ms on client, server cooldown 200 ms); click → `NpcSpawnPayload(id)` → server spawns next to the GM.
  - `NpcSheetScreen` (GM only): shows name, monster, level, HP current/max, AC, Fort/Ref/Will, perception, speed, strikes (name, bonus, damage like „1W6+2 Hieb, 1W4 Feuer“), resistances/weaknesses/immunities, ability names. Edit controls: −/+ for HP (Shift ±5), text fields for the numeric fields and the name with a „Setzen“ button each → `NpcEditPayload(entityId, field, value)`; buttons „Als Vorlage speichern“ (`NpcActionPayload(entityId, "save_template")`), „Entfernen“ (`"remove"`, with confirm click). Server re-sends `NpcSheetPayload` after each change.
  - Save template: `config/pf2craft/monsters/<TemplateNames.fileName(id)>` with `NpcJson.writeTemplate(base with id = sanitised display name)`; reload catalog entry in memory; message with the file name.
  - All receivers: `GmRoles.isGm(player)` else red `pf2craft.npc.not_gm`; entity id must resolve to a loaded `NpcEntity` within 64 blocks of the GM, else `pf2craft.npc.not_found`; strings max 32/64 chars per field.
- [ ] **Step 1:** Implement items, selection, payloads, receivers, screens; lang keys in both files (German: Befehlsstab, NPC hinzufügen, Suchen, Als Vorlage speichern, Entfernen, Ausgewählt …).
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS; client start check (runClient in background until „Sound engine started“, no pf2craft exceptions, then kill).
- [ ] **Step 3: Bot check** (server-side parts): GM bot runs `/pf2 tools` → has `pf2craft:command_staff`; non-GM bot with a staff (`/give`) right-clicks an NPC → red message; GM bot selects an NPC (right-click entity via mineflayer `activateEntity`) → action-bar „Ausgewählt“; right-clicks a block → NPC position changes within 10 s; `/pf2 npc info` after an HP edit to 0 is not reachable without the screen — instead add a GM-only fallback `/pf2 npc hp <value>` for the selected NPC and check: hp 0 → lying pose (entity pose SLEEPING in server log/info), hp 5 → standing. Record transcript. Screens are checked by the user.
- [ ] **Step 4:** Commit `Add GM command staff with NPC spawn and sheet screens`.

---

### Task 6: Docs

**Files:** `README.md`, `CLAUDE.md`

- [ ] **Step 1:** README (German): section „NPCs (Spielleiter)“ — `/pf2 tools`, Befehlsstab (Luft: NPC hinzufügen; NPC: auswählen; Schleichen + NPC: Werte; Block: hinlaufen), Werte ändern, Vorlagen in `config/pf2craft/monsters/`, besiegte NPCs liegen am Boden, Ersatzbefehle `/pf2 npc spawn|info|remove`; command table updated.
- [ ] **Step 2:** CLAUDE.md: structure (`core.monster`, `fabric/.../npc`), roadmap: Phase 3 Teil 1 ✅ ingame getestet, Teil 2 gebaut (Ingame-Test ausstehend).
- [ ] **Step 3:** `./gradlew.bat -PcoreOnly :core:test` and `./gradlew.bat build` → SUCCESS.
- [ ] **Step 4:** Commit `Document NPCs and the command staff`.
