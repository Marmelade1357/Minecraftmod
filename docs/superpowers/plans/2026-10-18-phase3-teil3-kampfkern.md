# Phase 3 Teil 3 – Kampfkern Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The GM starts a combat with a combat item, initiative is rolled and shown as a bar, everybody except the player whose turn it is is frozen, the active player has 3 actions and 1 reaction, moves with WASD (blocks are counted as Stride actions), attacks by clicking an enemy with a weapon item (attack-number window, server roll, instant result, damage with resistances), and the GM runs NPC turns with the command staff.

**Architecture:** All rules live in `core` (`de.pf2craft.core.combat`): damage resolution with resistances/weaknesses/immunities, rune damage, multiple attack penalty, attack and range rules, health labels, the `Encounter` (initiative order, turns), `TurnState` and `MovementBudget`. The Fabric side holds one server-side `CombatService` (single encounter, not persisted), enforces freezing and movement each server tick, keeps vanilla damage/hunger off, applies damage to NPC stats and player sheets, and syncs per-recipient state to the client HUD. Clients only send requests (click target, confirm attack number, end turn).

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn 1.21.1+build.3, Fabric API 0.116.6+1.21.1), Gson, JUnit 5, Mixin only if the HUD cannot be replaced through Fabric API.

**Spec:** `docs/superpowers/specs/2026-10-18-phase3-kampf-design.md` (German) — „Teil 3 – Kampfkern“, „TP und Minecraft-Gesundheit“, „Entscheidungen von Claude nach dem Builder-Check“. Interface gaps found in the Teil 2 final review are listed in Task 7/8 notes.

## Global Constraints

- Rules logic in `core` (`de.pf2craft.core.*`), no `net.minecraft`/`net.fabricmc` imports there; every new rule with tests.
- Server authoritative: clients only request; every payload validated (combatant? current turn? actions left? weapon valid? reach/range?). Client-only code under `fabric/src/client`.
- New text keys in **both** `en_us.json` and `de_de.json`; German dice „W“ („1W8“); messages via `Text.translatableWithFallback`.
- GM tools are **items**, not buttons (spec): the combat item opens the combat window; commands are fallbacks.
- Initiative: server rolls d20 + Perception bonus (players from the sheet, NPCs from `MonsterData.perception`); ties: NPCs before players, then higher Perception bonus.
- Turn: **3 actions + 1 reaction**; not-their-turn combatants are frozen (no movement, no attacks, no block break/place/use).
- Movement: **1 block = 5 ft**; the first step starts a **Stride** (1 action) worth `speed / 5` blocks; walking on starts the next Stride while actions remain; no actions left → blocked. Horizontal distance only.
- Attack: d20 + attack bonus − multiple attack penalty (**−5/−10, agile −4/−8**) against AC; degrees via `DegreeOfSuccess` (nat 20/1 shift one step). Result shown to everybody at once, no GM confirmation, **AC is never shown to players**.
- Damage: roll by type; critical hit doubles every rolled instance; then per instance: **immunity → weakness → resistance** (min 0); weakness does not apply to 0 damage.
- Players never see enemy HP numbers: only „unverletzt / verletzt / schwer verletzt“ (below 50 % = schwer verletzt). GMs see numbers.
- **Vanilla damage, death and hunger are off** for all players (exceptions: `/kill`, void); hearts and hunger bar replaced by text „TP: 70/85“.
- One combat at a time; not persisted (a server restart ends it).
- Windows dev: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"; export PATH="$JAVA_HOME/bin:$PATH"`; `./gradlew.bat -PcoreOnly :core:test` before every commit, plus `./gradlew.bat build` after Fabric changes. Source files plain UTF-8, no raw control characters (check with `file`).

## Review Focus

1. A modified client sends payloads or moves while it is not its turn → server rejects/teleports back, nothing is spent (Task 6 bot check: out-of-turn bot position stays; Task 7: attack confirm out of turn rejected).
2. Player disconnects during their turn, GM ends the combat mid-turn, or all NPCs/all players are defeated → the combat advances or ends, nobody stays frozen, rejoin works (Task 3: `Encounter` tests `removeCurrentAdvances`, `allInactiveEndsEncounter`; Task 4 bot check).
3. Attack with a foreign/stale weapon item, target out of reach/range, target already defeated, or no action left → rejected with a message and no action spent (Task 2 range tests, Task 7 command-fallback bot checks).
4. Unknown defence types from the monster data (`cold-iron`, `critical-hits`, `paralyzed`) and zero damage → never silently applied/boosted; unmatched types are reported to the GM (Task 1 tests).
5. HP edge cases: damage larger than HP, NPC HP exactly 0, player with HP 0 gets no turn but stays in the bar (Task 1/3 tests, Task 7 bot check).

---

## File Structure

```
core/src/main/java/de/pf2craft/core/combat/
    DamageInstance.java, DamageDice.java     records
    Defenses.java                            record(resistances, weaknesses, immunities)
    DamageResolver.java                      immunity → weakness → resistance
    DamageRoller.java                        roll DamageDice (crit doubling)
    RuneDamage.java                          property rune → extra DamageDice
    StrikeDamage.java                        Strike / MonsterStrike → List<DamageDice>
    AttackMath.java                          MAP, attack resolution
    RangeRules.java                          reach and range increments
    HealthLabel.java                         UNHURT/HURT/BADLY_HURT/DEFEATED
    Combatant.java, Encounter.java           initiative order and turns
    TurnState.java, MovementBudget.java      actions, attacks, Stride accounting
core/src/test/java/de/pf2craft/core/combat/…Test.java
fabric/src/main/java/de/pf2craft/combat/
    CombatService.java      the single encounter, tick, start/end/advance, sync
    CombatItem.java         "Kampfglocke" (GM item, vanilla bell look)
    Enforcement.java        freezing, movement counting, interaction callbacks, vanilla damage/hunger off
    AttackService.java      attack resolution for players and NPCs, damage application
    CombatMessages.java     chat lines
fabric/src/main/java/de/pf2craft/network/
    CombatSyncPayload, CombatStartPayload, CombatControlPayload, EndTurnPayload,
    OpenAttackScreenPayload, AttackConfirmPayload, OpenCombatScreenPayload
fabric/src/client/java/de/pf2craft/client/
    ClientCombat.java, CombatHud.java, CombatSetupScreen.java, AttackScreen.java
```

---

### Task 1: Core damage — dice, runes, resistances

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/{DamageInstance,DamageDice,Defenses,DamageResolver,DamageRoller,RuneDamage,StrikeDamage}.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/DamageTest.java`

**Interfaces:**
- Consumes: `core.monster.{Resistance, MonsterStrike, DamagePart, MonsterData}`, `core.character.{Strike, CharacterSheet, CharacterMath}`, `core.dice.{DiceExpression, DieRoller}`.
- Produces:
  - `record DamageInstance(String type, int amount)`; `record DamageDice(String dice, int bonus, String type)` (`dice` like `2d6` or `""` for flat; `type` a lowercase damage type).
  - `record Defenses(List<Resistance> resistances, List<Resistance> weaknesses, List<String> immunities)` with `static final Defenses NONE`.
  - `record AppliedPart(String type, int rolled, int applied)`; `record DamageOutcome(int total, List<AppliedPart> parts, List<String> unmatched)`; `final class DamageResolver { static DamageOutcome resolve(List<DamageInstance> instances, Defenses defenses) }`.
    - A defence type matches a damage type when equal, or `physical` ↔ bludgeoning/piercing/slashing, `energy` ↔ acid/cold/electricity/fire/sonic/force/vitality/void, `all-damage`/`all` ↔ everything. Defence types that match no damage type at all (`cold-iron`, `silver`, `critical-hits`, `precision`, `paralyzed`, …) go into `unmatched` (once each) and have no effect.
    - Per instance: any matching immunity → applied 0; else if rolled > 0: `applied = max(0, rolled + highestMatchingWeakness − highestMatchingResistance)`; rolled 0 → 0.
  - `final class DamageRoller { static List<DamageInstance> roll(List<DamageDice> parts, boolean critical, DieRoller roller) }` — each part `DiceExpression.parse(dice).roll(roller).total() + bonus` (flat when `dice` empty), minimum 0, doubled when `critical`.
  - `final class RuneDamage { static List<DamageDice> parts(List<String> propertyRunes) }` — rune name = first word before space/parenthesis, lowercase: flaming → `1d6 fire`, frost → `1d6 cold`, shock → `1d6 electricity`, corrosive → `1d6 acid`, thundering → `1d6 sonic`; all others → nothing.
  - `final class StrikeDamage { static List<DamageDice> forStrike(CharacterSheet sheet, int strikeIndex); static List<DamageDice> forMonster(MonsterStrike strike) }` — player: main part `dice = damageDice + damageDie` (e.g. `2d6` from `damageDice 2`, `damageDie "d6"`), `bonus = CharacterMath.strikeDamageBonus(sheet, index)`, type from Pathbuilder letter B/P/S → bludgeoning/piercing/slashing (anything else → bludgeoning), plus `RuneDamage.parts(strike.propertyRunes())`; monster: one `DamageDice` per `DamagePart`.

- [ ] **Step 1: Write failing tests** `DamageTest` (use a fixed `DieRoller` — look at `DieRoller` for a deterministic factory, else write a tiny test roller):
  - `fireResistanceOnlyReducesFire`: instances slashing 6 + fire 4, resistance fire 5 → total 6; parts fire applied 0.
  - `weaknessAddsThenResistanceSubtracts`: weakness fire 5 and resistance fire 3, fire 4 → 6; weakness fire 5 on fire 0 → 0.
  - `immunityZeroes`: immunity `fire`, fire 7 → 0.
  - `physicalAndAllDamageMatch`: resistance `physical` 5 on slashing 8 → 3; `all-damage` 2 on piercing 1 → 0; `energy` weakness 3 on cold 2 → 5.
  - `unmatchedTypesAreReported`: defences `cold-iron` resistance, immunity `critical-hits`, damage slashing 5 → total 5, `unmatched == ["cold-iron","critical-hits"]` (order of first appearance).
  - `criticalDoublesEachInstance`: roller giving 3 on 1d6, bonus 2, critical → 10; non-critical → 5.
  - `strikeDamageFromSheet`: fighter5 `422753-fighter5.json` parsed with `PathbuilderImporter`, strike 0 → main part `dice "1d6"`, `bonus 4`, `type "slashing"`; magus9 Rapier (striking, +1) → dice `2d6`, type `piercing`; a sheet whose strike has property rune `"flaming (greater)"` (build via `Strike` constructor/withers) → extra part `1d6 fire`.
  - `monsterStrikeDamage`: kobold-warrior's first strike (from the shipped catalog) → its `DamagePart`s one-to-one.
- [ ] **Step 2:** Run `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL.
- [ ] **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add combat damage rules with resistances and rune damage`.

---

### Task 2: Core attack math, range rules, health labels

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/{AttackMath,RangeRules,HealthLabel}.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/AttackRulesTest.java`

**Interfaces:**
- Consumes: `core.check.DegreeOfSuccess`.
- Produces:
  - `record AttackOutcome(int natural, int total, DegreeOfSuccess degree)`; `final class AttackMath { static int mapPenalty(int attackNumber, boolean agile); static AttackOutcome resolve(int natural, int attackBonus, int mapPenalty, int rangePenalty, int ac); static boolean isAgile(List<String> traits) }` — penalty: 1 → 0; 2 → −4 agile / −5; ≥3 → −8 agile / −10 (values returned negative); `total = natural + attackBonus + mapPenalty + rangePenalty`; `isAgile` = traits contain `agile`.
  - `final class RangeRules { static double meleeReachBlocks(List<String> traits); static OptionalInt rangePenalty(double distanceBlocks, int incrementFeet) }` — reach: traits contain `reach` or `reach-10` → 2.0; `reach-15` → 3.0; `reach-20` → 4.0; default 1.0 (blocks, measured hitbox edge to hitbox edge by the caller). `rangePenalty`: increments = `ceil(distanceBlocks * 5 / incrementFeet)` (min 1); more than 6 → empty; else `−2 * (increments − 1)`.
  - `enum HealthLabel { UNHURT, HURT, BADLY_HURT, DEFEATED }` with `static HealthLabel of(int current, int max)`: `current <= 0` → DEFEATED; `current >= max` → UNHURT; `current * 2 < max` → BADLY_HURT; else HURT.
- [ ] **Step 1: Write failing tests:** `mapPenalty(1,false)==0`, `(2,false)==-5`, `(3,false)==-10`, `(2,true)==-4`, `(3,true)==-8`, `(4,false)==-10`; `resolve(15, 7, -5, 0, 17)` → total 17, `SUCCESS`; `resolve(20, 0, 0, 0, 25)` → total 20, `SUCCESS` (nat 20 shifts a failure up); `resolve(1, 20, 0, 0, 10)` → total 21 but nat 1 shifts down → `FAILURE`; `resolve(10, 5, -10, -2, 15)` → total 3, `CRITICAL_FAILURE`; reach: `["agile"]` → 1.0, `["reach"]` → 2.0, `["reach-15"]` → 3.0; `rangePenalty(10, 60)` (50 ft) → −0; `(13,60)` (65 ft, 2 increments) → −2; `(72,60)` (360 ft = 6 increments) → −10; `(73,60)` (365 ft) → empty; `HealthLabel.of(10,10)==UNHURT`, `(9,10)==HURT`, `(5,10)==HURT`, `(4,10)==BADLY_HURT`, `(0,10)==DEFEATED`, `(-3,10)==DEFEATED`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add attack math, range rules and health labels`.

---

### Task 3: Core encounter, turn state and movement budget

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/{Combatant,TurnState,MovementBudget,Encounter}.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/EncounterTest.java`, `MovementBudgetTest.java`

**Interfaces:**
- Produces:
  - `record Combatant(String id, String name, boolean npc, int perception)`.
  - `final class TurnState { TurnState(); int actionsLeft(); boolean reactionAvailable(); int attacksMade(); boolean spendActions(int n) /* false and no change if not enough */; void spendReaction(); int nextAttackNumber() /* attacksMade+1 */; void recordAttack(); }` — starts with 3 actions, reaction available, 0 attacks.
  - `final class MovementBudget { MovementBudget(int speedFeet); int blocksPerStride(); boolean tryMove(double blocks, TurnState turn); double remainingInStride(); int stridesNeededFor(double blocks); }` — `blocksPerStride = max(1, speedFeet / 5)` when speed > 0, else 0 (never moves). `tryMove(b)`: `b <= 0` → true; if `remainingInStride <= 0`: needs a new Stride — `turn.spendActions(1)` else return false; set `remainingInStride = blocksPerStride`; then `remainingInStride -= b` (overshoot by one step is allowed); return true. `stridesNeededFor(blocks) = ceil(blocks / blocksPerStride)`.
  - `final class Encounter`:
    - `void add(Combatant c, int initiativeTotal)`; ordering: initiative descending; ties: NPC before player, then higher `perception`, then name.
    - `List<Entry> order()` where `record Entry(Combatant combatant, int initiative, boolean active)`; `Optional<Combatant> current()`; `int round()` (starts at 1); `TurnState turn()`; `void setActive(String id, boolean active)`; `void remove(String id)`; `Optional<Combatant> advance()` — moves to the next active combatant (round +1 when wrapping), resets `TurnState`, returns it, or empty when no combatant is active; removing/deactivating the current combatant does **not** advance by itself (the service calls `advance()`); `boolean hasActive(boolean npc)`.
    - The first `current()` after the first `add`s is the highest initiative active entry; `start()` finalises order and sets the pointer; adding later inserts into order without moving the current pointer off the current combatant.
- [ ] **Step 1: Write failing tests:**
  - `EncounterTest.ordersByInitiativeThenNpcThenPerception`: A(player, 15, perc 5), B(npc, 15, perc 3), C(player, 15, perc 8), D(player, 20) → order D, B, C, A.
  - `advanceWrapsAndCountsRounds`: three active → advance ×3 → back to first, `round() == 2`.
  - `advanceSkipsInactive`: middle inactive → skipped.
  - `removeCurrentAdvances`: remove current, then `advance()` gives the next; remaining order intact.
  - `allInactiveEndsEncounter`: everyone inactive → `advance()` is empty.
  - `lateAddKeepsCurrent`: add a combatant with the highest initiative after start → `current()` unchanged, appears first in `order()`.
  - `advanceResetsTurnState`: after spending 2 actions and `recordAttack()`, `advance()` → fresh `TurnState` (3 actions, 0 attacks, reaction available).
  - `MovementBudgetTest`: speed 25 → `blocksPerStride()==5`; moving 0.5 ×10 spends exactly 1 action (`actionsLeft()==2`), the 11th step spends a second; 0.5 ×30 spends all 3 and the 31st step returns false with 0 actions left (position budget exhausted); after `spendActions(2)` only one Stride (5 blocks) is possible; speed 0 → `tryMove(0.5)` false; `stridesNeededFor(5)==1`, `(5.1)==2`, `(12)==3`; `spendActions(4)` on a fresh turn → false and unchanged.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add encounter order, turn state and movement budget`.

---

### Task 4: Server combat service, combat item and fallback commands

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/combat/{CombatService,CombatItem,CombatMessages}.java`, payloads `CombatSyncPayload`, `CombatStartPayload`, `CombatControlPayload`, `EndTurnPayload`, `OpenCombatScreenPayload` in `fabric/src/main/java/de/pf2craft/network/`
- Modify: `Pf2Networking.java`, `Pf2Craft.java` (item registration, tick, disconnect/join hooks), `Pf2Items.java`, `Pf2Commands.java`, `GmRoles.setGm`/`/pf2 tools` (also give the combat item), `RollService.java` (new `static RollResult rollQuiet(String expression) throws DiceParseException`, same SecureRandom roller, no messages), lang files, item model `assets/pf2craft/models/item/combat_bell.json` (parent `minecraft:item/generated`, layer0 `minecraft:item/bell`)

**Interfaces:**
- Consumes: Tasks 1–3 core; `CharacterStore.get`, `CharacterMath.perception`, `NpcEntity.stats()`, `GmRoles.isGm`, `NpcService.apply`.
- Produces:
  - `CombatService` (static, server thread only): `boolean active()`; `Optional<Encounter> encounter()`; `Optional<String> currentId()`; `void start(MinecraftServer, ServerPlayerEntity gm, Collection<UUID> participantIds)` — rolls initiative for every participant that is a player with a character (`perception` from `CharacterMath.perception`) or an `NpcEntity` (`stats().base().perception()`), as `rollQuiet("1d20")` + bonus, builds the `Encounter`, announces „Kampf beginnt“ and the order in chat (NPC initiative hidden from non-GMs), syncs; `void add(...)` (late joiners roll and are inserted); `void endTurn(ServerPlayerEntity actor)` (actor must be the current player, or a GM for any current combatant); `void skip(ServerPlayerEntity gm)`; `void end(ServerPlayerEntity gm)`; `void onDisconnect(ServerPlayerEntity)` (deactivate; if current → advance); `void onJoin(ServerPlayerEntity)` (reactivate when still alive); `void markDefeated(String id)` (deactivate; if current → advance; if no active NPC or no active player remains → chat note to GMs „Kampf kann beendet werden“, and if nobody is active → end automatically); `void sync(MinecraftServer)` (per-recipient payloads, see below); `TurnState turn()`, `MovementBudget budget()` (reset by `advance`) for Tasks 5–8.
  - Recipients: every online combatant player and every GM. `CombatSyncPayload(boolean active, int round, List<Entry> entries, int actionsLeft, boolean reactionAvailable, int strideBlocksLeft)` with `Entry(UUID id, String name, boolean npc, int initiative /* -1 hidden */, byte label /* HealthLabel ordinal for NPCs, 255 for players */, int hp /* −1 for non-GMs */, int maxHp, boolean active, boolean current)`; GMs receive NPC initiative and numeric HP, everyone else not.
  - Payloads: `CombatStartPayload(List<UUID> participants)` C2S (GM, max 64); `CombatControlPayload(String action)` C2S (`skip`, `end`; ≤ 16 chars); `EndTurnPayload()` C2S; `OpenCombatScreenPayload(List<Candidate>)` S2C with `Candidate(UUID id, String name, boolean npc)` for characters/NPCs within 32 blocks of the GM, sent when the GM uses the combat item with no combat running (running combat → `OpenCombatScreenPayload` with a flag `running=true`, the screen then shows Skip/End). All receivers: `GmRoles.isGm` (except `EndTurnPayload`: the current player), else red `pf2craft.combat.not_gm`/`not_your_turn`.
  - `CombatItem` `pf2craft:combat_bell` (stack size 1, GM only): right-click air → `OpenCombatScreenPayload`; given by `/pf2 tools` and `GmRoles.setGm`.
  - Commands (fallback, GM or permission 2): `/pf2 combat start` (all players with a character and all NPCs within 32 blocks of the executor), `/pf2 combat status` (order, round, current, actions), `/pf2 combat next` (= skip), `/pf2 combat end`, `/pf2 combat endturn` (any player: ends own turn).
  - Tick hook: none yet except syncing on state change.
- [ ] **Step 1:** Implement items, payloads, receivers, service, commands, lang keys (both files; German: Kampf beginnt, Runde, Du bist dran, Zug beenden, Kampf beendet …).
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 3: Bot check** (dedicated server + mineflayer in the scratchpad `bot`, as in earlier tasks; two bots A (GM, opped) and B (player with imported `422753`)): `/pf2 combat start` with an NPC spawned → chat shows the order; `/pf2 combat status` shows round 1 and the current combatant; current combatant's player runs `/pf2 combat endturn` → current changes; a non-current player running `endturn` → red message; B disconnects while current → turn advances; B reconnects → active again; `/pf2 combat end` → status says no combat. Record transcript. Stop the server; commit nothing from `fabric/run` or scratchpad.
- [ ] **Step 4:** Commit `Add combat service, combat bell item and combat commands`.

---

### Task 5: Client HUD and combat window

**Files:**
- Create: `fabric/src/client/java/de/pf2craft/client/{ClientCombat,CombatHud,CombatSetupScreen}.java`
- Modify: `Pf2CraftClient.java` (receivers, key **Enter** is not rebindable: add key binding `key.pf2craft.end_turn` default `GLFW_KEY_ENTER`, sends `EndTurnPayload` when the sheet screen/chat is closed and it is your turn), `NpcSpawnScreen` (reuse for „+ NPC hinzufügen…“), lang files

**Interfaces:**
- Consumes: `CombatSyncPayload`, `OpenCombatScreenPayload`, `CombatStartPayload`, `CombatControlPayload`, `EndTurnPayload` (Task 4), `ClientSheets` (own sheet for TP).
- Produces:
  - `ClientCombat`: holds the latest `CombatSyncPayload`; cleared on disconnect and when `active == false`.
  - `CombatHud`: registered like `RollHud`; draws the **initiative bar** top centre — left-most slot larger = current, then the following ones; each slot: name, initiative (players; NPCs only for GMs), for NPCs the health label text („unverletzt“/„verletzt“/„schwer verletzt“, defeated greyed with „besiegt“; GMs see „hp/max“), inactive entries greyed; text „Runde N“. Below the hotbar for the active player: „Aktionen ◆◆◇ · Reaktion ↺ · Bewegung x/y Blöcke“ (strideBlocksLeft / blocksPerStride from the payload; use a neutral „–“ when no Stride is open). When it is your turn, a banner „Du bist dran“.
  - Health display (spec): replace the vanilla hearts and hunger bar with the text **„TP: 70/85“** from the own `ClientSheets` entry (no character → nothing). Prefer Fabric API's HUD layer API if it exists in 0.116.6 (check `net.fabricmc.fabric.api.client.rendering.v1.hud.*` in the Loom cache jar); otherwise add a client Mixin (`pf2craft.client.mixins.json`, registered in `fabric.mod.json` under `mixins` with `"environment": "client"`) cancelling `InGameHud`'s health, food, armor-adjacent status bars *(builder's call; report which)*.
  - `CombatSetupScreen` (opened by `OpenCombatScreenPayload`): two columns with checkboxes (players with a character left, NPCs right, all ticked by default), centre button **„Initiative würfeln“** → `CombatStartPayload(ticked ids)` and closes; under the NPC column „+ NPC hinzufügen…“ opens `NpcSpawnScreen` (afterwards the list is requested again); when `running == true`: buttons „Zug überspringen“ (`skip`), „Kampf beenden“ (`end`, second click confirms), and „Teilnehmer hinzufügen“ (shows the setup lists with only not-yet-participating candidates). Layout legible at ~640×360 scaled px, vanilla look.
- [ ] **Step 1:** Implement; lang keys in both files.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS; client start check (`runClient` in background to a log in the scratchpad, wait for „Sound engine started“, no pf2craft exceptions, kill it).
- [ ] **Step 3:** Commit `Add combat HUD, initiative bar and combat window`. Screens are checked by the user.

---

### Task 6: Enforcement — freezing, movement counting, no vanilla damage

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/combat/Enforcement.java`
- Modify: `Pf2Craft.java` (register callbacks and tick), `CombatService.java` (anchors, budgets), lang files

**Interfaces:**
- Consumes: `CombatService` (current combatant, `turn()`, `budget()`), core `MovementBudget`.
- Produces `Enforcement.register()` hooking:
  - `ServerTickEvents.END_SERVER_TICK`:
    - for every **non-current** combatant player (and every combatant player when the combat has no current): if horizontal distance to its anchor exceeds 0.02 blocks or it jumped (Δy > 0.5 upward), teleport back to the anchor (`networkHandler.requestTeleport`, keep yaw/pitch), zero velocity; the anchor is set when the combatant's turn ends / at combat start;
    - for the **current** player: horizontal movement since last tick → `budget().tryMove(delta, turn)`; `false` → teleport back to the last accepted position (message `pf2craft.combat.no_actions` rate-limited to once per 2 s); on `true` update the last accepted position and sync when an action was spent;
    - for every player (combat or not): food level reset to 20 every 20 ticks.
  - `UseBlockCallback`, `AttackBlockCallback`, `UseItemCallback`, `AttackEntityCallback`, `PlayerBlockBreakEvents.BEFORE`: for a **non-current combatant** return FAIL/false (message `pf2craft.combat.not_your_turn`, rate-limited); GMs are never combatants and never blocked. (The attack click of the *current* player is handled in Task 7; this task only blocks non-current ones.)
  - `ServerLivingEntityEvents.ALLOW_DAMAGE` → `false` for every `ServerPlayerEntity` unless the source is in the `BYPASSES_INVULNERABILITY` tag; `ALLOW_DEATH` → `false` for players (same exception).
  - NPCs: nothing to do (they never move on their own); Task 8 gates the staff.
  - A defeated/inactive combatant is frozen like a non-current one.
- [ ] **Step 1:** Implement.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 3: Bot check** (two player bots with imported characters `422753`, one GM bot, one NPC; `/pf2 combat start`): the non-current bot walks 3 blocks → server log/position shows it returned to the anchor within a second; it tries to dig a block (`bot.dig`) → block stays; the current bot walks forward 20 blocks continuously → it stops at ≈ 15 blocks (speed 25 ft = 5 blocks × 3 Strides) and `/pf2 combat status` shows 0 actions; with `/pf2 combat endturn` the next bot can move again; `/damage <bot> 5 minecraft:player_attack` and `/effect give <bot> minecraft:hunger 10 10` do not lower health/food (vanilla `health` stays 20 in the bot's view); `/kill <bot>` still kills. Record transcript. Stop the server; commit nothing from `fabric/run` or scratchpad.
- [ ] **Step 4:** Commit `Enforce turns and movement and switch vanilla damage off`.

---

### Task 7: Player attacks — screen, resolution, damage

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/combat/AttackService.java`, payloads `OpenAttackScreenPayload`, `AttackConfirmPayload`, `fabric/src/client/java/de/pf2craft/client/AttackScreen.java`
- Modify: `CombatService.java`, `Enforcement.java` (current player's `AttackEntityCallback`), `CharacterItems.java` (add `static OptionalInt strikeIndexFor(ServerPlayerEntity holder, ItemStack stack)` next to `strikeFor`), `SheetService.java` (public `static void setCurrentHp(ServerPlayerEntity player, int hp)` reusing the existing `/pf2 hp` path: store, clamp 0..max, sync), `NpcService.java` (public `static void damage(NpcEntity npc, int amount)` → `NpcEdit.apply(stats,"hp", …)`, then `CombatService.markDefeated` when 0), `Pf2Networking.java`, `Pf2Commands.java` (`/pf2 combat attack <number>`), `CombatMessages.java`, lang files

**Interfaces:**
- Consumes: Tasks 1–6, `CharacterItems.strikeFor/strikeIndexFor`, `ItemData.catalog()` (weapon range by `Strike.name()`), `RollService.rollQuiet`.
- Produces:
  - `AttackService.openFor(ServerPlayerEntity attacker, Entity target)`: preconditions (each failing with a red message and nothing sent): combat active, attacker is the current combatant, `turn().actionsLeft() ≥ 1`, main-hand stack is a valid weapon item (`strikeFor` present), target is a living, non-defeated `NpcEntity` (players attack NPCs only), target reachable. Then sends `OpenAttackScreenPayload(int targetEntityId, String strikeName, int suggestedNumber, List<Integer> bonuses /* index 0..2 = attack 1..3 incl. MAP, no range penalty */)`.
  - Reach/range: melee weapon (catalog `range == 0`): hitbox-edge distance ≤ `RangeRules.meleeReachBlocks(strike.traits())`; ranged (`range > 0` from `ItemData.catalog().weapon(name)`, default 60 ft if unknown and `traits` contain `thrown`/`propulsive`): `RangeRules.rangePenalty(distance, increment)`, empty → „zu weit“. Line of sight is **not** checked *(plan decision)*.
  - `AttackConfirmPayload(int targetEntityId, int attackNumber)` C2S (`attackNumber` 1..3): `AttackService.confirm(...)` re-validates everything from the preconditions, plus a per-player cooldown of 300 ms; spends 1 action (`turn().spendActions(1)`), `recordAttack()`; rolls `rollQuiet("1d20")`; `AttackMath.resolve(natural, CharacterMath.strikeAttack(sheet, idx), AttackMath.mapPenalty(number, agile), rangePenalty, npcAc)`; on SUCCESS/CRITICAL_SUCCESS rolls `StrikeDamage.forStrike` with `DamageRoller` (critical = CRITICAL_SUCCESS) and resolves with `Defenses(npc resistances, weaknesses, immunities)`; applies `total` via `NpcService.damage`. Any attack number 1..3 is accepted (spec: window pre-selects, user may change); the number used is part of the public message.
  - Message to **everyone** (`CombatMessages`): „August greift Kobold 1 an (2. Angriff): Treffer! – 9 Schaden“ with colour by degree (crit gold, success green, failure grey, crit failure red) and, for critical hits, „(kritisch)“; **no AC, no roll total, no HP numbers**. Messages to **GMs** add: natural die, total, AC, per-part damage (`rolled → applied`), unmatched defence types („Hinweis: cold-iron nicht berücksichtigt“), remaining HP.
  - Misses/zero damage: „verfehlt“ / „kein Schaden“ wording variants in both languages.
  - Fallback command `/pf2 combat attack <1-3>` (any combatant player; targets the NPC looked at within 16 blocks, weapon in main hand) → same `confirm` path (needed for bot checks).
  - `AttackScreen` (client): title „Angriff mit <weapon>“, three buttons „1. Angriff (+13)“, „2. Angriff (+8)“, „3. Angriff (+3)“ (bonus including MAP), the suggested one highlighted, Escape cancels; click → `AttackConfirmPayload`, closes.
  - `Enforcement` current-player `AttackEntityCallback`: cancel vanilla attack always; if the target is an `NpcEntity` call `AttackService.openFor`.
- [ ] **Step 1:** Implement (the suggested attack number is `Math.min(3, turn().nextAttackNumber())`); lang keys in both files.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 3: Bot check** (bots as in Task 6; the player bot must hold the golden „Lion Scythe“ from import; NPC `kobold-warrior` next to it; combat started with the bot current): `/pf2 combat attack 1` looking at the NPC → public message, one action spent (`/pf2 combat status` shows 2), GM bot sees the detailed line; again with `2`, `3` → 0 actions; a fourth → red „keine Aktionen“; out of turn → red message, nothing spent; target > reach → „zu weit“; after enough hits `/pf2 npc info` shows the NPC HP decreasing, at 0 the NPC is defeated and removed from the active order (`status`), and when it was the last NPC the GM gets the „Kampf kann beendet werden“ note. Record the transcript; the click → screen path is checked by the user. Stop the server; commit nothing from `fabric/run` or scratchpad.
- [ ] **Step 4:** Commit `Add player attacks with attack-number window, damage and resistances`.

---

### Task 8: NPC turns — staff gating, NPC attacks, defeated players

**Files:**
- Modify: `fabric/src/main/java/de/pf2craft/npc/CommandStaffItem.java`, `combat/AttackService.java`, `combat/CombatService.java`, `character/SheetService.java` (defeated hook), `Pf2Commands.java`, `OpenAttackScreenPayload`/`AttackConfirmPayload` (generalise), `AttackScreen.java`, `NpcService.java` (defeated/revive hook), lang files

**Interfaces:**
- Consumes: Tasks 4–7.
- Behaviour:
  - **Walking in combat:** the staff's block click on a selected NPC while a combat is active: allowed only when that NPC is the current combatant; the target must be reachable: compute `path = npc.getNavigation().findPathTo(x, y, z, 0)`; path length in blocks `L` (number of nodes; if no path → message `pf2craft.combat.no_path`); `strides = budget.stridesNeededFor(L − remainingInStride-adjusted)`; if `strides > turn.actionsLeft()` → refuse with `pf2craft.combat.not_enough_actions` and nothing spent; else spend `strides` actions (`turn().spendActions`), start walking (`startMovingTo`, speed 0.6). When the NPC is not current → `pf2craft.combat.npc_not_current`. Outside combat unchanged (Teil 2 behaviour).
  - **Walking NPCs are bound to the budget:** a tick check cancels the NPC's navigation when it has walked more than the paid-for blocks (`strides × blocksPerStride`) — simplest: stop navigation at the paid distance.
  - **NPC attacks:** with an NPC selected that is the current combatant, a right-click on an entity (player or NPC; `useOnEntity`) opens `OpenAttackScreenPayload` listing the NPC's strikes (`List<StrikeOption(String name, List<Integer> bonuses, boolean agile)>`), `AttackConfirmPayload` gets `attackerId` (UUID) and `strikeIndex`; the server resolves with `MonsterStrike.attackBonus` (+ MAP), target AC from the target (NPC `base().ac()` or player `CharacterMath.armorClass`), damage via `StrikeDamage.forMonster`, defences of the target (NPC from stats, players `Defenses.NONE`); player targets lose HP through `SheetService.setCurrentHp`; reach from the monster strike's `reach-*` traits (`RangeRules.meleeReachBlocks`), ranged strikes use increment 60 ft *(plan decision: monster data has no range increment)*; a monster with no strikes → message `pf2craft.combat.no_strikes`.
  - **Player at 0 HP:** `SheetService.setCurrentHp` hooks `CombatService.markDefeated` when a combatant player reaches 0 (inactive, frozen, bar shows „bewusstlos“); HP above 0 later reactivates (`CombatService.reviveIfNeeded`). **Dying/recovery checks are Teil 5** (spec item 11 is split: unconscious now, sterbend 1–4 with Wiederherstellungswurf with the condition engine) *(Claude decision)*.
  - The GM can still `/pf2 npc hp` etc. during combat; changes trigger `CombatService.sync` and defeated handling (`NpcService.apply` calls `CombatService.markDefeated`/`reviveIfNeeded` for NPCs).
  - `AttackScreen` shows a strike selector (cycling button) when more than one option exists.
  - Fallback `/pf2 combat attack <number> [strikeIndex]` for GMs: the selected current NPC attacks the entity looked at.
- [ ] **Step 1:** Implement; lang keys in both files.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings; client start check as in Task 5.
- [ ] **Step 3: Bot check** (GM bot with staff, player bot, NPC kobold-warrior; combat with the NPC current): staff walk request for a block 4 blocks away → NPC moves, `status` shows 2 actions; a block 40 blocks away → „nicht genug Aktionen“; walking out of turn → red message; `/pf2 combat attack 1` as GM with the NPC selected looking at the player bot → public message; the player sheet HP drops (`/pf2 sheet` via the GM or `info`); lowering the player bot to 0 with repeated attacks (or `/pf2 hp <bot> -500`) → the bot becomes inactive/„bewusstlos“ in `status` and its turn is skipped; `/pf2 hp <bot> 5` reactivates it. Record transcript. Stop the server; commit nothing from `fabric/run` or scratchpad.
- [ ] **Step 4:** Commit `Add NPC turns, NPC attacks and unconscious players`.

---

### Task 9: Docs

**Files:** `README.md`, `CLAUDE.md`, `NOTICE.md` (only if new Paizo content is named)

- [ ] **Step 1:** README (German): section „Kampf“ — Kampfglocke (`/pf2 tools`), Kampf-Fenster (Spieler/NPCs anhaken, „Initiative würfeln“, NPC hinzufügen), Initiative-Leiste, Einfrieren, 3 Aktionen/Reaktion, Bewegung (Schreiten pro `Geschwindigkeit/5` Blöcke, Anzeige), Zug beenden (Enter), Angriff (Waffe in die Hand, Gegner anklicken, 1./2./3. Angriff, Ergebnis für alle, Schaden mit Resistenzen/Schwächen/Immunitäten, Gegner-TP nur „verletzt/schwer verletzt“), NPC-Züge mit dem Befehlsstab, TP-Anzeige statt Herzen, Vanilla-Schaden/Hunger aus, Ersatzbefehle `/pf2 combat …`; command table updated. „Bekannte Grenzen“: keine Reaktionen, kein Sterbend/Wiederherstellungswurf (Teil 5), keine Zauber, keine Sichtlinienprüfung, Munition/Nachladen nicht erzwungen, Kampf wird bei Serverneustart beendet, Spawn-Schutz-Hinweis bleibt.
- [ ] **Step 2:** CLAUDE.md: structure (`core.combat`, `fabric/.../combat`), roadmap Teil 3 gebaut (Ingame-Test des Nutzers steht aus), Teil 4/5 offen; „Bekannter Stand“ ehrlich.
- [ ] **Step 3:** `./gradlew.bat -PcoreOnly :core:test` and `./gradlew.bat build` → SUCCESS.
- [ ] **Step 4:** Commit `Document combat`.
