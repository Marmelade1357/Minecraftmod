# Phase 5 Teil 1 – Zauber Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spellcasters cast their imported Pathbuilder spells in game: slots/focus points tracked, spell attack/DC computed, combat action costs, automatic resolution for a starter catalog, GM resolution for everything else.

**Architecture:** New pure package `de.pf2craft.core.spell` (spellbook model, Pathbuilder spell import, math, catalog, resolver; tests first). Server: a second data attachment `SpellbookStore` next to `CharacterStore`, a `SpellData` catalog loader (jar + config, like `MonsterDataLoader`/`ItemData`), and `SpellService` for casting with pending-target clicks (pattern of `ActionService`). Client: a "Spells" tab in `CharacterScreen` and the "Spells" category of `ActionMenuScreen`.

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn), Fabric API data attachments, Gson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-24-phase5-zauber-design.md`

## Global Constraints

- Code/comments/commits English; docs/chat German. Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Rules in `core` (no `net.minecraft`), tests first; server authoritative (validate sender, ownership, slots, turn, actions, ranges, string lengths); client code only under `fabric/src/client`; payloads registered in `Pf2Networking.registerPayloads()`; texts via `Text.translatableWithFallback` with keys in BOTH lang files.
- License: catalog holds names and numbers only, no rule text copied from AoN.
- Cantrips and focus spells auto-heighten to rank `ceil(level / 2)`; spell attack = level + proficiency bonus (rank) + key spellcasting ability mod; spell DC = 10 + that.
- Basic save multipliers: crit success 0, success half (round down), failure full, crit failure double. Spell attack crit = double damage.
- Unknown (non-catalog) spells cost 2 actions in combat and are resolved by the GM.
- Build: `./gradlew.bat -PcoreOnly :core:test --console=plain`, `./gradlew.bat build --console=plain`; JAVA_HOME `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.

## Review Focus

- Characters imported before this feature (no spellbook) must keep working (empty spellbook, no crash, sheet tab says "no spells").
- Casting with no slot left / no focus point / not the own turn / too few actions must be refused without spending anything.
- A forged cast request (other player's entry index, unknown spell name, rank above 10, too many targets, targets out of range or in another world) must be refused server-side.
- Multi-target pending state must be cleared on turn end, combat end, disconnect and cancel (V) — no stuck targeting.
- Healing a dying player through a heal spell must go through the same heal path as `/pf2 hp` (dying removed, wounded +1).

---

### Task 1: Core spellbook model and Pathbuilder spell import

**Files:**
- Create in `core/src/main/java/de/pf2craft/core/spell/`: `CastingType.java` (enum PREPARED, SPONTANEOUS, FOCUS, INNATE; `fromPathbuilder(String type, String tradition)` → FOCUS when tradition is `focus`, INNATE for `innate`, SPONTANEOUS for `spontaneous`, else PREPARED), `SpellcastingEntry.java`, `Spellbook.java`, `SpellMath.java`, `PathbuilderSpells.java`, `SpellbookJson.java`
- Test: `core/src/test/java/de/pf2craft/core/spell/{PathbuilderSpellsTest,SpellbookTest,SpellMathTest,SpellbookJsonTest}.java`

**Interfaces (produces):**
- `record SpellcastingEntry(String name, String tradition, CastingType type, Ability ability, Rank rank, int[] perDay /* length 11, index = spell rank 0..10 */, Map<Integer, List<String>> spellsByRank)` — defensive copies, `perDay` copied.
- `final class Spellbook`: `static Spellbook empty()`; `List<SpellcastingEntry> entries()`; `int focusMax()`; `int focusCurrent()`; `int slotsLeft(int entry, int rank)` (= perDay − used; rank 0 → `Integer.MAX_VALUE` meaning cantrips are free); `boolean spendSlot(int entry, int rank)` (false, unchanged, when none left or entry type FOCUS/INNATE handled separately); `boolean spendFocus()`; `void rest()` (used slots → 0, focusCurrent → focusMax); `void resetSlots(int entry)`; `Spellbook copy()`.
- `final class SpellMath` (static): `int spellAttack(CharacterSheet sheet, SpellcastingEntry e)` = `sheet.level() + CharacterMath`-style proficiency bonus of `e.rank()` (untrained 0, else level + 2/4/6/8) + `sheet.abilityMod(e.ability())`; `int spellDc(...)` = 10 + spellAttack; `int autoHeightenRank(int level)` = `Math.max(1, (level + 1) / 2)`.
- `final class PathbuilderSpells` (static): `Spellbook parse(JsonObject build)` reading `spellCasters[]` (`name`, `magicTradition`, `spellcastingType`, `ability` (str/dex/con/int/wis/cha), `proficiency` (0/2/4/6/8 → Rank via `Rank.fromPathbuilder`), `perDay` (pad/truncate to 11), `spells[]` of `{spellLevel, list[]}`) and `focusPoints` (top-level `build.focusPoints` if present, else the max `focusPoints` of the FOCUS entries); entries with no spells and no slots are kept (prepared casters like Wizard may have empty lists in Pathbuilder) — Ruling of the plan; missing/invalid fields → defaults, never throw.
- `final class SpellbookJson` (static): `String write(Spellbook)`, `Spellbook read(String json)` round trip incl. used slots and focusCurrent; unknown/garbage input → `Spellbook.empty()`.

- [ ] **Step 1: Write failing tests:** `wizardFixtureHasWizardEntryWithTenRanksOfSlots` (fixture `pathbuilder/142926-wizard20.json`: an entry named "Wizard", type PREPARED, ability INT, rank LEGENDARY, `perDay[10] == 2`, `perDay[1] == 3`); `focusEntryIsFocusType` (same fixture: entry "Focus Spells" type FOCUS, spells at rank 1 contain "Magic's Vessel"; `focusMax()` == 2); `cantripsAreFree` (`slotsLeft(i, 0) == Integer.MAX_VALUE`); `spendSlotCountsDown` and `spendSlotRefusedWhenEmpty`; `restRefillsSlotsAndFocus`; `spellAttackForLevel5ExpertWis18` (level 5, expert, WIS 18 → 5 + 4 + 4 = 13, DC 23 — express with a minimal sheet built by an existing test helper or `PathbuilderImporter` fixture, whichever exists); `autoHeightenRank` for levels 1→1, 2→1, 3→2, 20→10; `jsonRoundTripKeepsUsedSlots`; `garbageJsonGivesEmptyBook`; `missingSpellCastersGivesEmptyBook`; a loop test `allFixturesParseWithoutException` over every file in `core/src/test/resources/pathbuilder/`.
- [ ] **Step 2:** `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL.
- [ ] **Step 3:** Implement. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add spellbook model and Pathbuilder spell import`.

### Task 2: Core spell catalog, heightening and resolution (+ starter data)

**Files:**
- Create in `core/src/main/java/de/pf2craft/core/spell/`: `SpellKind.java` (ATTACK, SAVE, HEAL, UTILITY), `SpellDefinition.java`, `SpellCatalog.java`, `SpellCatalogParser.java`, `SpellResolver.java`
- Create: `core/src/main/resources/pf2craft/spells/spells.json` (starter catalog)
- Test: `core/src/test/java/de/pf2craft/core/spell/{SpellCatalogParserTest,SpellResolverTest,StarterCatalogTest}.java`

**Interfaces (produces):**
- `record SpellDefinition(String id, String name, int rank /* 0 = cantrip */, int actions /* 1..3 */, int rangeFeet /* 0 = touch/self */, int maxTargets /* 1 single, >1 multi/area, 0 self */, SpellKind kind, String save /* fortitude|reflex|will|"" */, boolean basicSave, String damageDice /* e.g. "2d4" or "" */, int damageBonus, String damageType, String heightenDice /* e.g. "1d4" or "" */, int heightenStep /* ranks per heighten step, 1 or 2; 0 = none */, boolean addAbilityMod /* cantrip/heal rider using spellcasting mod */)`.
- `SpellCatalog`: `Optional<SpellDefinition> byName(String name)` (case-insensitive, also matches `id`), `List<SpellDefinition> all()`, `SpellCatalog merged(SpellCatalog override)` (later wins by id).
- `SpellCatalogParser.parse(String json)` → `SpellCatalog`, invalid entries skipped with a reason list (`ParseResult(SpellCatalog, List<String> errors)`), like the monster/item parsers.
- `SpellResolver` (static): `String damageAtRank(SpellDefinition d, int castRank)` (base dice + `(castRank - max(d.rank,1)) / heightenStep` × heighten dice, same die size merged, e.g. "2d4"+"1d4"×2 → "4d4"); `int multiplier(SpellKind kind, boolean basicSave, DegreeOfSuccess d)` returns damage multiplier ×2 as integer quarters or a small enum — choose `double` factor 0, 0.5, 1, 2: ATTACK: crit success 2, success 1, else 0; basic SAVE: crit success 0, success 0.5, failure 1, crit failure 2 (degree is the TARGET's save result); non-basic SAVE: success/crit success 0, failure 1, crit failure 2 (Annahme for the starter set); `int applyFactor(int rolled, double factor)` (round down).
- Starter catalog: about 25 common Remaster combat spells with values read from Archives of Nethys (2e.aonprd.com, Remaster rules) — **numbers and names only**, no descriptions. Must include at least: Electric Arc, Force Barrage, Heal, Harm, Fireball, Breathe Fire, Ignition, Frostbite, Caustic Blast, Needle Darts, Telekinetic Projectile, Divine Lance, Gale Blast, Slashing Gust, Thunderstrike, Chilling Spray, Daze, Fear (UTILITY/save with frightened handled by GM text), Shield (UTILITY), Sure Strike (UTILITY), Bless (UTILITY), Lightning Bolt, Horizon Thunder Sphere, Spout, Puff of Poison. Where a spell's behaviour does not fit the model (e.g. Force Barrage auto-hit, Heal 1/2/3-action variants), model the common 2-action single-target use and note it in `StarterCatalogTest` comments.
- [ ] **Step 1: Write failing tests:** parser accepts a valid entry and rejects missing id/name, actions out of 1..3, bad dice; `damageAtRank` for a cantrip "2d4"/"1d4"/step 1 at rank 3 → "4d4", for a rank-3 spell "6d6"/"2d6"/step 1 at rank 5 → "10d6", step 2 handling; multipliers per table above; `StarterCatalogTest`: the bundled json parses with 0 errors, has ≥ 25 entries, contains the names listed above, every damage spell has a valid damage type from `DamageResolver`'s type set.
- [ ] **Step 2:** run → FAIL. **Step 3:** implement and research/write the json. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add spell catalog, heightening and resolution rules`.

### Task 3: Server – spellbook storage, import, data loading, rest, sync

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/spell/{SpellbookStore,SpellData}.java`, `fabric/src/main/java/de/pf2craft/network/SpellbookSyncPayload.java` (server→client: `UUID owner, String json` ≤ 32767), registered in `Pf2Networking`
- Modify: `CharacterService.java` (import also stores `PathbuilderSpells.parse(build)`; reset clears the spellbook), `Pf2Craft.java` (register store + load data on server start like monsters/items), `Pf2Commands.java` (`/pf2 rest <targets>|all` GM; `/pf2 spells reset <player>` GM), `SheetService.java` (send spellbook sync together with sheet sync to owner and GMs viewing), lang files

**Interfaces:**
- `SpellbookStore`: `Spellbook get(ServerPlayerEntity)` (empty if absent), `void set(ServerPlayerEntity, Spellbook)`, `void remove(...)`; persisted as attachment with a JSON string codec (`SpellbookJson`).
- `SpellData`: `SpellCatalog catalog()`; loads `pf2craft/spells/spells.json` from the jar and `config/pf2craft/spells/*.json` (custom entries override by id; log parse errors).
- [ ] **Step 1:** Implement; lang keys (both files). **Step 2:** Build → SUCCESS. **Step 3:** Bot check (dedicated server, scratchpad `bot`, patched mineflayer): import a caster fixture id is not available offline — use the existing import path used by earlier bot checks (character 422753) and additionally a test-only path is NOT allowed; instead verify with a spellcaster id if 422753 is a caster, else verify `/pf2 rest all` and sync payload of an empty spellbook, and the import code path by unit tests from Task 1. **Step 4:** Commit `Store and sync spellbooks, add rest command`.

### Task 4: Server – casting

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/spell/SpellService.java`, `fabric/src/main/java/de/pf2craft/network/CastSpellPayload.java` (client→server: `int entry, String spell ≤ 64, int rank`), `SpellTargetPayload`? — no: reuse the click path: pending cast completes via `Enforcement` left-click / staff like `ActionService.executePending`; multi-target confirm via `ActionRequestPayload(actor, "confirm", -1)` or a new `CastConfirmPayload` (builder's choice, document it)
- Modify: `Enforcement.java`, `ActionService.java` (cancel also clears pending casts), `CombatService.java` (clear pending on turn/combat end), `AttackService.java` (expose damage application helpers if needed — no duplication: extract a shared `applyDamage(target, instances, …)` if the attack code has one inline), `SheetService.java` / `DyingService.java` (heal path), `CombatMessages.java`, lang files

**Behaviour:**
- `cast(player, entry, spellName, rank)`: validate own spellbook entry index, spell present in that entry's list (cantrips at rank 0, focus entries, or `rank ≥` listed rank for spontaneous heightening), rank 0..10, slot/focus availability; in combat: own turn, `canAct`, enough actions (`def.actions` or 2); spend NOTHING until the cast actually happens (targets chosen).
- Self (maxTargets 0) and UTILITY spells resolve immediately; single-target spells enter pending mode (one click); multi-target collect up to `maxTargets` distinct targets within `rangeFeet / 5` blocks, confirm → resolve. HUD hint via existing `PendingActionPayload` text or a new payload (builder's choice).
- Resolve: ATTACK → d20 + spell attack (+ `ConditionEffects.attackPenalty`) vs target AC (incl. effects/conditions as in `AttackService`); SAVE → per target d20 + save bonus (+ target `checkPenalty`) vs spell DC; damage via `SpellResolver` + `DamageResolver` with target defenses; HEAL → heal amount (dice + ability mod if flagged) on allies via the same heal path as HP edits; UTILITY/unknown → chat line, GM line with attack/DC.
- Spend slot/focus and actions only when the cast resolves; interrupt stride like other actions; does not increase the attack count (MAP).
- Chat: public line per cast and per target result; NPC numbers only to GMs (like attacks).
- [ ] **Step 1:** Implement; lang keys both files. **Step 2:** Build → SUCCESS, no new warnings. **Step 3:** Bot check: GM gives a bot spells? — use `/pf2 rest` and a caster character if available; otherwise add a GM debug command `/pf2 spells grant <player> <spell> <rank>` that adds a spell + 1 slot to a GM-chosen player's spellbook (GM only, documented) and use it to test: cast Electric Arc (multi 2 targets) at two NPCs → save results and damage lines, HP reduced; cast Heal on a downed player → dying removed; casting without slot refused; not own turn refused. **Step 4:** Commit `Add spell casting with automatic resolution`.

### Task 5: Client – spells tab and menu

**Files:**
- Modify: `CharacterScreen.java` (tab "Spells": entries, ranks, spells, slots left, focus points, spell attack/DC, cast button with rank picker for spontaneous casters; GM viewing another player: read-only + "reset slots" button sending `/pf2 spells reset` equivalent payload or command), `ActionMenuScreen.java` (category "Spells" enabled: list castable spells with action cost, greyed with reason when not castable), `ClientSheets.java` (store synced spellbook), `Pf2CraftClient.java` (receiver), `CombatHud.java` (cast target hint), lang files
- [ ] **Step 1:** Implement; both lang files. **Step 2:** Build → SUCCESS; `runClient` smoke start ("Sound engine started", no exceptions); dedicated server smoke start. **Step 3:** Commit `Add spells tab and spell menu`.

### Task 6: Docs

**Files:** `README.md` (German: Zauber-Reiter, Wirken, Plätze/Fokus, Rast `/pf2 rest`, Ziele anklicken/V, Startkatalog und eigene Zauber per `config/pf2craft/spells/*.json` mit Feldbeschreibung, Grenzen), `CLAUDE.md` (structure `core.spell`, `fabric spell/`, roadmap Phase 5 Teil 1 gebaut, Ingame-Test steht aus, known limits), `HOW-TO-USE.md` (Zauber-Abschnitt kurz).
- [ ] **Step 1:** Update. **Step 2:** core tests + build → SUCCESS. **Step 3:** Commit `Document spellcasting`.
