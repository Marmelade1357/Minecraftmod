# Phase 3 Teil 1 – Items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a Player Core item list (weapons, armor, shields) as data, enrich imported characters with it, and give every player their Pathbuilder weapons and shield as real Minecraft items whose look follows the damage die and whose tooltip shows name, damage, traits and runes.

**Architecture:** Item data and all mapping rules (catalog lookup, visual tier, rune parsing) live in `core` with JUnit tests. The character sheet stays the source of truth: `Strike` gains the item facts later combat needs (group, traits, property runes) and the sheet gains an optional shield. On the Fabric side a custom data component marks mod items and points at the owner's strike; a factory builds vanilla item stacks with name, lore and unbreakable flag; import and re-import replace the player's mod items.

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn 1.21.1+build.3, Fabric API 0.116.6+1.21.1), Gson, JUnit 5; data source Foundry PF2e system (`foundryvtt/pf2e`, `packs/equipment`), checked against https://2e.aonprd.com.

**Spec:** `docs/superpowers/specs/2026-10-18-phase3-kampf-design.md` (German) — section „Teil 1 – Items“ and „Entscheidungen von Claude nach dem Builder-Check“ (Waffen-Items).

## Global Constraints

- Rules logic in `core` (`de.pf2craft.core.*`), no `net.minecraft`/`net.fabricmc` imports there; new rules always with tests.
- Server authoritative; client-only code only under `fabric/src/client`.
- Every new text key in **both** `en_us.json` and `de_de.json`; German uses „W“ for dice („1W8 Hieb“), English „d“ („1d8 slashing“); messages via `Text.translatableWithFallback`.
- Content names (weapons, armor, runes) stay English as Pathbuilder/Player Core deliver them; only UI labels are translated.
- License: mechanics (numbers, traits) from Remaster books under ORC; Golarion proper names allowed via the Paizo Community Use Policy; **no copied description text** — tooltips get at most a short own summary (≤ 1 sentence) or none.
- Visual tier by damage die: **d4 = wood, d6 = gold, d8 = diamond, d10/d12 = netherite**.
- The character sheet is the source of truth; items only point at it (spec: „Der Charakterbogen ist die Wahrheit“).
- Re-import removes the player's old mod weapons/shields and gives new ones.
- Vanilla weapons without PF2 data do not count in combat.
- Windows dev: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"; export PATH="$JAVA_HOME/bin:$PATH"`; `./gradlew.bat -PcoreOnly :core:test` before every commit, plus `./gradlew.bat build` after Fabric changes.

## Review Focus

1. A Pathbuilder weapon whose name is not in the catalog (homebrew, typo, „Lion Scythe“) → the player still gets an item with name, die and damage type from the export, traits empty, visual from the die (Task 3: `unknownWeaponStillProducesStrike`).
2. Pathbuilder lists the shield before the body armor in `armor[]` → AC category must come from the worn body armor, never `"shield"` (Task 3: `shieldFirstDoesNotBecomeArmorCategory`).
3. Character JSON stored by Phase 2 (no group/traits/runes/shield fields) → loads with empty defaults, nothing lost (Task 3: `legacySheetJsonStillLoads`).
4. Re-import while the old sword is in the off-hand, a chest or dropped on the floor → off-hand and main inventory are cleaned; items outside the inventory stay but no longer work for anyone but their owner's current sheet (Task 4: `WeaponRef` carries a sheet stamp; bot check).
5. Inventory full on import → the remaining items drop at the player's feet with a chat note, nothing is silently lost (Task 4 bot check).

---

## File Structure

```
core/src/main/java/de/pf2craft/core/item/
    WeaponData.java        record: one catalog weapon
    ArmorData.java         record: one catalog armor
    ShieldData.java        record: one catalog shield
    ItemCatalog.java       lookup by normalised name; built from parsed lists
    ItemCatalogParser.java JSON → lists (strict, clear IllegalArgumentException)
    ItemVisual.java        record(Shape, Material) + forWeapon(die, group)
    RuneNames.java         Pathbuilder potency/striking/property rune text helpers
core/src/main/resources/pf2craft/items/
    weapons.json, armor.json, shields.json
core/src/test/java/de/pf2craft/core/item/…Test.java
core/src/main/java/de/pf2craft/core/character/
    Strike.java            + group, traits, propertyRunes, potency
    Shield.java            new record
    CharacterSheet.java    + Optional<Shield> shield (last component)
    PathbuilderImporter.java  enrich strikes from catalog, parse shield, armor-category fix
    CharacterJson.java     read/write new fields, defaults for old data
fabric/src/main/java/de/pf2craft/item/
    ItemData.java          loads ItemCatalog from jar + config/pf2craft/items/*.json
    Pf2Components.java     registers data components pf2craft:weapon, pf2craft:shield
    WeaponRef.java, ShieldRef.java   component records with CODEC/PACKET_CODEC
    Pf2ItemFactory.java    CharacterSheet → List<ItemStack>
    CharacterItems.java    give/replace a player's mod items
fabric/src/main/java/de/pf2craft/character/CharacterService.java   call CharacterItems on import/reset
```

---

### Task 1: Core item model and catalog parser

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/item/{WeaponData,ArmorData,ShieldData,ItemCatalog,ItemCatalogParser}.java`
- Test: `core/src/test/java/de/pf2craft/core/item/ItemCatalogTest.java`

**Interfaces:**
- Produces:
  - `record WeaponData(String id, String name, String category, String die, String damageType, String group, int hands, int range, List<String> traits)` — `category` ∈ `simple|martial|advanced|unarmed`; `damageType` ∈ `B|P|S` (Pathbuilder letters); `range` in feet, 0 = melee; `traits` lowercase Foundry slugs (`agile`, `finesse`, `reach`, `thrown-20`, `deadly-d8`, `two-hand-d10`, …)
  - `record ArmorData(String id, String name, String category, int acBonus, int dexCap, int checkPenalty, int speedPenalty, int strength, List<String> traits)` — `category` ∈ `unarmored|light|medium|heavy`
  - `record ShieldData(String id, String name, int acBonus, int hardness, int hitPoints, int brokenThreshold, int speedPenalty)`
  - `final class ItemCatalog { ItemCatalog(List<WeaponData>, List<ArmorData>, List<ShieldData>); Optional<WeaponData> weapon(String name); Optional<ArmorData> armor(String name); Optional<ShieldData> shield(String name); int size(); static String normalise(String name) }` — `normalise`: lowercase (Locale.ROOT), strip leading `+N `, strip the words `striking`, `greater`, `major`, remove everything but letters/digits/spaces, collapse spaces, trim. Lookup uses `normalise` on both sides; later lists override earlier entries with the same normalised name.
  - `final class ItemCatalogParser { static List<WeaponData> weapons(String json); static List<ArmorData> armor(String json); static List<ShieldData> shields(String json) }` — each file is a JSON array of objects with exactly the record's field names; missing/mistyped field, unknown `category`/`damageType`, `die` not in `d4|d6|d8|d10|d12`, negative numbers, duplicate `id` within a file → `IllegalArgumentException` naming the entry `id` and field.

- [ ] **Step 1: Write the failing tests** `ItemCatalogTest`:
  - parse a 2-weapon JSON (`longsword`: martial, d8, S, sword, 1 hand, range 0, traits `["versatile-p"]`; `shortbow`: martial, d6, P, bow, 1+ → hands 2, range 60, traits `["deadly-d10"]`) → `weapon("Longsword")` present, `weapon("+1 Striking Longsword")` present (normalise), `weapon("Lion Scythe")` empty.
  - `normalise("+2 Greater Striking Rapier") == "rapier"`, `normalise("Composite  Longbow") == "composite longbow"`.
  - rejects: `die: "d7"`, `category: "exotic"`, missing `group`, `hands: -1`, two entries with `id: "dagger"` → each throws with the id in the message.
  - armor and shield files parse; `armor("Full Plate").acBonus()` from the test JSON (6), `shield("Steel Shield").hardness()` (5).
- [ ] **Step 2:** Run `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL (classes missing).
- [ ] **Step 3:** Implement the five classes with Gson `JsonParser` (explicit field reads, no reflection).
- [ ] **Step 4:** Run tests → PASS.
- [ ] **Step 5:** Commit `Add item catalog model and parser`.

---

### Task 2: Item data files from the Player Core

**Files:**
- Create: `core/src/main/resources/pf2craft/items/{weapons,armor,shields}.json`
- Test: `core/src/test/java/de/pf2craft/core/item/ItemDataConsistencyTest.java`

**Interfaces:**
- Consumes: `ItemCatalogParser`, `ItemCatalog` (Task 1).
- Produces: the three resource files at classpath `/pf2craft/items/…`.

- [ ] **Step 1:** Generate the data from the Foundry PF2e system repository (`github.com/foundryvtt/pf2e`, `packs/equipment`, items whose `publication.title` is "Pathfinder Player Core" and `type` ∈ weapon/armor/shield; base items only, no specific magic items). Mapping: `system.damage.die`, `system.damage.damageType` (bludgeoning→B, piercing→P, slashing→S), `system.category`, `system.group`, `system.usage.value` (`held-in-two-hands`→2, `held-in-one-plus-hands`→2, else 1), `system.range` (null→0), `system.traits.value`; armor `system.acBonus`, `dexCap`, `checkPenalty`, `speedPenalty`, `strength`; shields `system.acBonus`, `hardness`, `hp.max`, `hp.brokenThreshold`, `speedPenalty`. Write the converter as a throwaway script outside the repo (scratchpad); commit only the JSON. Use English names; no descriptions. Sort by `id`.
- [ ] **Step 2: Write the failing test** `ItemDataConsistencyTest`:
  - all three files parse; `weapons ≥ 60`, `armor ≥ 10`, `shields ≥ 4` entries.
  - spot values (Player Core): Longsword d8 S martial sword, traits contain `versatile-p`; Dagger d4 P simple knife, traits contain `agile`, `finesse`, `thrown-10`, `versatile-s`; Greatsword d12 S martial sword hands 2; Longbow d8 P martial bow range 100 traits contain `deadly-d10`, `volley-30`; Full Plate heavy acBonus 6 dexCap 0 strength 4; Leather Armor light acBonus 1 dexCap 4; Steel Shield acBonus 2 hardness 5 hitPoints 20 brokenThreshold 10.
  - for every weapon/armor/shield name in the 261 Pathbuilder fixtures (`core/src/test/resources/pathbuilder/`), the catalog finds it unless listed in `KNOWN_MISSING` with a one-line reason (e.g. not in Player Core); `KNOWN_MISSING` ≤ 20 % of distinct names. Print nothing to stdout.
- [ ] **Step 3:** Run tests → fix data until PASS.
- [ ] **Step 4:** Commit `Add Player Core weapons, armor and shields`.

---

### Task 3: Sheet enrichment — strike item facts, shield, visual tier

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/item/{ItemVisual,RuneNames}.java`, `core/src/main/java/de/pf2craft/core/character/Shield.java`
- Modify: `Strike.java`, `CharacterSheet.java`, `PathbuilderImporter.java`, `CharacterJson.java`, every `new CharacterSheet(...)`/`new Strike(...)` call site (tests, `LevelUp`, `GmEdit`, fixtures helper)
- Test: `core/src/test/java/de/pf2craft/core/item/ItemVisualTest.java`, additions to `PathbuilderImporterTest`, `CharacterJsonTest`

**Interfaces:**
- Consumes: `ItemCatalog` (Task 1), data files (Task 2).
- Produces:
  - `Strike` gains (appended in this order) `String group, List<String> traits, List<String> propertyRunes, int potency` — `group` from catalog or `""`; `traits` from catalog or empty; `propertyRunes` from Pathbuilder `runes[]` (lowercased, as given); `potency` = Pathbuilder `pot`.
  - `record Shield(String name, int acBonus, int hardness, int hitPoints, int brokenThreshold)` (from catalog; unknown shield → acBonus 2, hardness 3, hitPoints 12, brokenThreshold 6 = wooden shield defaults *(plan decision)*).
  - `CharacterSheet` gains last component `Optional<Shield> shield`.
  - `PathbuilderImporter.parse(String json, ItemCatalog catalog)`; the old one-argument `parse(String)` stays and uses an empty catalog (keeps existing callers/tests valid).
  - `record ItemVisual(Shape shape, Material material)`, `enum Shape { SWORD, AXE, PICKAXE, SHOVEL, HOE, BOW, CROSSBOW, TRIDENT, STICK }`, `enum Material { WOOD, GOLD, DIAMOND, NETHERITE }`; `static ItemVisual forWeapon(String die, String group, int range)`.
    - Material: `d4`→WOOD, `d6`→GOLD, `d8`→DIAMOND, `d10`/`d12`→NETHERITE, anything else → WOOD.
    - Shape by group: `sword`, `knife` → SWORD; `axe` → AXE; `pick` → PICKAXE; `polearm`, `spear` → TRIDENT; `bow` → BOW; `crossbow`, `firearm` → CROSSBOW; `club`, `hammer`, `flail`, `brawling` → STICK; `dart`, `sling`, `bomb` and unknown → SWORD if `range == 0`, otherwise BOW.
  - `final class RuneNames { static String strikingWord(int dice) }` → `""`, `"Striking"`, `"Greater Striking"`, `"Major Striking"` for 1–4 (used for display names like „+1 Striking Rapier“).
- Importer rules: when choosing the AC armor category, skip `armor[]` entries with `prof == "shield"`; the first worn entry with `prof == "shield"` becomes `shield`; strikes look up the catalog by Pathbuilder `name`.

- [ ] **Step 1: Write failing tests:**
  - `ItemVisualTest`: `("d8","sword",0)` → SWORD/DIAMOND; `("d4","knife",0)` → SWORD/WOOD; `("d12","axe",0)` → AXE/NETHERITE; `("d6","bow",60)` → BOW/GOLD; `("d10","polearm",0)` → TRIDENT/NETHERITE; `("d6","club",0)` → STICK/GOLD; `("d4","dart",20)` → BOW/WOOD; `("d7","whatever",0)` → SWORD/WOOD.
  - `PathbuilderImporterTest.magusRapierGetsCatalogFacts`: parse `315329-magus9.json` with the shipped catalog → strike 0 group `sword`, traits contain `deadly-d8`, `disarm`, `finesse`; potency 1; damageDice 2.
  - `unknownWeaponStillProducesStrike`: `422753-fighter5.json` (Lion Scythe) with the shipped catalog → strike present, die `d6`, damageType `S`, group `""`, traits empty.
  - `shieldIsParsed`: a fixture with a worn Steel Shield (find one via grep in the fixtures folder and name it in the test) → `shield` present, hardness 5.
  - `shieldFirstDoesNotBecomeArmorCategory`: fighter5 JSON with a `{"name":"Steel Shield","prof":"shield","worn":true}` entry inserted at index 0 of `armor` → `armorClass().armorCategory() == "heavy"` and AC still 23.
  - `CharacterJsonTest.legacySheetJsonStillLoads`: write fighter5, strip the new keys (`group`, `traits`, `propertyRunes`, `potency`, `shield`) from the JSON, read → succeeds, empty defaults, `shield` empty; round-trip of a sheet with all new fields equal.
- [ ] **Step 2:** Run tests → FAIL.
- [ ] **Step 3:** Implement; update all call sites mechanically (pass `""`, `List.of()`, `0`, `Optional.empty()` where no data exists).
- [ ] **Step 4:** Run full core suite → PASS (all existing parity tests stay green).
- [ ] **Step 5:** Commit `Enrich strikes with item data and parse shields`.

---

### Task 4: Fabric — mod items, giving them on import

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/item/{ItemData,Pf2Components,WeaponRef,ShieldRef,Pf2ItemFactory,CharacterItems}.java`
- Modify: `Pf2Craft.java` (register components, load ItemData on SERVER_STARTING next to ClassData), `CharacterService.java` (import uses `PathbuilderImporter.parse(json, ItemData.catalog())`; after a successful import call `CharacterItems.replace(player, sheet)`; on reset call `CharacterItems.remove(player)`), `PathbuilderResponse`/fetch path if it calls `parse` (pass the catalog), both lang files

**Interfaces:**
- Consumes: Task 3 `Strike`/`Shield`/`ItemVisual`/`RuneNames`, Task 1 `ItemCatalog`.
- Produces:
  - `ItemData`: `static ItemCatalog catalog()`, `static void load(MinecraftServer)` — jar files, then `config/pf2craft/items/{weapons,armor,shields}.json` appended (override by name); bad files logged and skipped (same pattern as `ClassData`).
  - `record WeaponRef(UUID owner, int strikeIndex, long sheetStamp)` and `record ShieldRef(UUID owner, long sheetStamp)` with `Codec` + `PacketCodec`; `Pf2Components.WEAPON`, `Pf2Components.SHIELD` registered as `ComponentType` under `pf2craft:weapon` / `pf2craft:shield` (`Registries.DATA_COMPONENT_TYPE`).
  - `sheetStamp`: `ItemStamp.of(sheet)` (Step 1) at import time; an item whose stamp differs from the owner's current sheet is stale.
  - `Pf2ItemFactory`: `static List<ItemStack> create(UUID owner, CharacterSheet sheet)`; one stack per strike except category `unarmed`, plus one vanilla `SHIELD` if `shield` present. Vanilla item from `ItemVisual` (SWORD+DIAMOND → `Items.DIAMOND_SWORD`, BOW → `Items.BOW`, CROSSBOW → `Items.CROSSBOW`, TRIDENT → `Items.TRIDENT`, STICK → `Items.STICK`, …; GOLD → golden_*, WOOD → wooden_*). Components: `CUSTOM_NAME` = display name (`+1 Striking Rapier` style, no italics), `LORE` lines (translatable): damage („1W8 Hieb“ / „1d8 slashing“, dice count from `damageDice`), traits (comma list), property runes, owner name; `UNBREAKABLE` hidden; `ATTRIBUTE_MODIFIERS` hidden in tooltip; plus `WEAPON`/`SHIELD` component.
  - `CharacterItems`: `static void replace(ServerPlayerEntity, CharacterSheet)` (remove, then insert each stack; if `insertStack` fails, `dropItem` at the player and send `pf2craft.items.dropped`), `static void remove(ServerPlayerEntity)` (removes all stacks in main inventory, armor and off-hand whose WEAPON/SHIELD component has this owner), `static Optional<Strike> strikeFor(ServerPlayerEntity holder, ItemStack stack)` (present only if component owner == holder, stamp == current sheet stamp and index valid — used by Teil 3).
- Lang keys (both files): `pf2craft.items.damage` („%s %s“ with die text), damage types `pf2craft.damage.b|p|s` (Wucht/Stich/Hieb · bludgeoning/piercing/slashing), `pf2craft.items.traits`, `pf2craft.items.runes`, `pf2craft.items.owner`, `pf2craft.items.dropped`, `pf2craft.items.shield` („RK +%s, Härte %s, TP %s“).

- [ ] **Step 1: Core helper with test first.** Write `core/src/test/java/de/pf2craft/core/item/ItemStampTest.java`: `ItemStamp.of(sheet) == ItemStamp.of(sameSheet)`; changing a strike or the level gives a different stamp; changing only `currentHp` gives the **same** stamp (HP changes must not invalidate weapons). Run → FAIL. Implement `final class de.pf2craft.core.item.ItemStamp { static long of(CharacterSheet sheet) }` as a hash over name, className, level and strikes. Run → PASS. Use it as `sheetStamp`.
- [ ] **Step 2:** Implement the Fabric classes and hooks; lang keys in both files.
- [ ] **Step 3:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 4: Bot check** (dedicated server + mineflayer, scratch folder outside the repo, as in Phase 2): `/pf2 character import 422753` → bot inventory contains exactly one **golden** sword-shaped item named „Lion Scythe“ (d6, group unknown → SWORD) with lore lines; Full Plate is armor and is not given; re-import → still exactly one Lion Scythe; fill the bot's inventory with dirt (`/give`) and re-import → item dropped at its feet + chat note; `/pf2 character reset` → item removed. Record the transcript in the report. Stop the server; commit nothing from `fabric/run` or the scratch folder.
- [ ] **Step 5:** Commit `Give players their Pathbuilder weapons and shield as items`.

---

### Task 5: Docs and license notices

**Files:** `README.md`, `CLAUDE.md`, `fabric/src/main/resources/fabric.mod.json`, new `NOTICE.md`

- [ ] **Step 1:** Read the current Paizo Community Use Policy (https://paizo.com/licenses/communityuse) and the ORC notice requirements (https://paizo.com/orclicense). Write `NOTICE.md` (English) with the required ORC attribution for Player Core content and the exact Community Use Policy notice text; if the policy forbids combining with ORC material or requires something else, stop and report BLOCKED with the quote instead of guessing.
- [ ] **Step 2:** README (German): section „Waffen und Ausrüstung“ — items on import, look by damage die (W4 Holz, W6 Gold, W8 Diamant, W10/W12 Netherite), tooltip, re-import replaces them, own items via `config/pf2craft/items/`; link `NOTICE.md`. `fabric.mod.json` description: one sentence pointing to the notices. CLAUDE.md: project structure (`core.item`, `fabric/.../item`), roadmap Phase 3 Teil 1 ✅ (pending user test).
- [ ] **Step 3:** `./gradlew.bat -PcoreOnly :core:test` and `./gradlew.bat build` → SUCCESS.
- [ ] **Step 4:** Commit `Document items and add license notices`.
