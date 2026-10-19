# Phase 3 Teil 4 – Aktionsmenü Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In their own turn a player (and the GM for the selected, current NPC) opens an action menu with the **V** key, picks a basic action (Schritt, Schild heben, Demoralisieren, Umstoßen, Packen, Suchen, Deckung nehmen), clicks a target when the action needs one, and the server pays the actions, rolls, applies temporary AC effects and announces the result.

**Architecture:** Action definitions, target-DC rules and temporary combat effects (AC bonuses with expiry) live in `core` with tests. A server `ActionService` validates and executes requests (`ActionRequestPayload`), including a „pending target“ mode: the menu selects an action, the next left-click on a creature executes it. The client only shows the menu and the pending hint. Conditions caused by results (prone, frightened, grabbed …) are **not applied** here; the GM gets a hint — the condition engine is Teil 5.

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn 1.21.1+build.3, Fabric API 0.116.6+1.21.1), Gson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-18-phase3-kampf-design.md` (German) — „Teil 4 – Aktionsmenü“ and „Entscheidungen von Claude nach dem Builder-Check“ (NPC-Züge: V öffnet das Menü für den ausgewählten NPC). Existing combat code: `combat/{CombatService, AttackService, Enforcement, CombatMessages, NpcWalk}`, core `de.pf2craft.core.combat.*`.

## Global Constraints

- Rules in `core` (`de.pf2craft.core.*`), no `net.minecraft`/`net.fabricmc` imports there; tests first. Server authoritative; client only requests and displays; every C2S payload validated (combatant? current turn? actions left? action valid? target valid? reach?). Client-only code only under `fabric/src/client`.
- New text keys in **both** `en_us.json` and `de_de.json`; messages via `Text.translatableWithFallback`; German dice „W“.
- The menu opens **only in your own turn** (players) or for the **selected current NPC** (GM); costs shown as ◆; not payable actions greyed out.
- Actions needing a target: pick the action, then click the target (left-click) — Esc, V again, or the end of the turn cancels.
- Menu categories: **Grundaktionen**, **Klassenfähigkeiten** (shows „noch keine“), **Zauber** (disabled: „folgt“), **Gegenstände** (disabled: „folgt“). Favorites (★) listed first, stored client-side.
- Skill actions count as attacks for the **multiple attack penalty** where PF2 says so (Umstoßen, Packen): they use `turn().nextAttackNumber()` for MAP (agile never applies) and call `recordAttack()`; Demoralisieren/Suchen do not.
- No AC, roll total or natural die to non-GMs (same privacy rule as attacks); public line shows action, target and degree of success only.
- Temporary effects: **Schild heben** +shield AC bonus (circumstance) until the start of the acting combatant's next turn; **Deckung nehmen** +2 circumstance AC until the combatant moves (budget `tryMove` spends/charges) or the start of their next turn. Circumstance bonuses of the same source do not stack (a repeated Schild heben refreshes).
- Conditions are Teil 5: results only produce a GM hint line („Ziel kann liegend sein“).
- Windows dev: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"; export PATH="$JAVA_HOME/bin:$PATH"`; `./gradlew.bat -PcoreOnly :core:test` before every commit, plus `./gradlew.bat build` after Fabric changes. Source files plain UTF-8, no raw control characters (check with `file`).

## Review Focus

1. A client requests an action outside its turn, with no actions left, for a combatant it does not control, with an unknown action id, or twice quickly → rejected with a message, nothing spent (Task 3 tests of the validation helpers + bot checks with `/pf2 combat action`).
2. Pending-target mode never gets stuck: turn end, disconnect, combat end, target defeated or removed, V pressed again → pending state cleared (Task 3).
3. Temporary effects expire exactly once and never outlive the combat or a removed combatant; Schild heben without a shield (or Take Cover while a different cover is active) behaves sanely (Task 2 tests `shieldExpiresAtNextTurnStart`, `coverEndsOnMove`, Task 3).
4. Step: exactly one block of movement, then the budget is empty again, and it does not break a running Stride (Task 2 test `stepGivesExactlyOneBlock`).
5. Skill checks for creatures without the skill (monster `skills` map has no athletics/intimidation; players untrained) use a defined fallback, never an exception or 0-division (Task 1 tests).

---

## File Structure

```
core/src/main/java/de/pf2craft/core/combat/
    ActionKind.java, BasicAction.java, BasicActions.java   action definitions
    ActionDcs.java                                         skill modifier fallbacks and target DCs
    CombatEffects.java                                     temporary AC effects with expiry
    MovementBudget.java                                    + grantStep()
core/src/test/java/de/pf2craft/core/combat/…Test.java
fabric/src/main/java/de/pf2craft/combat/
    ActionService.java      validation, execution, pending target mode
    CombatMessages.java     + action lines
    CombatService.java      + effects, pending clear hooks
    Enforcement.java        + left-click on a creature executes the pending action, movement ends cover
    AttackService.java      + AC bonus from CombatEffects
fabric/src/main/java/de/pf2craft/network/
    ActionRequestPayload, PendingActionPayload
fabric/src/client/java/de/pf2craft/client/
    ActionMenuScreen.java, ClientActions.java (favorites + pending hint), CombatHud.java (+ hint line), Pf2CraftClient.java (V key)
```

---

### Task 1: Core action definitions and DC rules

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/{ActionKind,BasicAction,BasicActions,ActionDcs}.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/BasicActionsTest.java`, `ActionDcsTest.java`

**Interfaces:**
- Consumes: `core.character.{CharacterSheet, CharacterMath, Skill, Save}`, `core.monster.{MonsterData, NpcStats}`.
- Produces:
  - `enum ActionKind { SELF, TARGET_MELEE, TARGET_RANGE }`.
  - `record BasicAction(String id, int cost, ActionKind kind, String skill /* "" for none; Skill.id() or "perception" */, String defense /* "" | "fortitude" | "reflex" | "will" | "perception_dc" */, boolean countsAsAttack, boolean needsShield, int rangeBlocks /* TARGET_RANGE only */)`.
  - `final class BasicActions { static final List<BasicAction> ALL; static Optional<BasicAction> byId(String id) }` with exactly these ids: `step` (cost 1, SELF), `raise_shield` (1, SELF, needsShield), `take_cover` (1, SELF), `seek` (1, SELF, skill `perception`), `demoralize` (1, TARGET_RANGE 6 blocks, skill `intimidation`, defense `will`), `trip` (1, TARGET_MELEE, skill `athletics`, defense `reflex`, countsAsAttack), `grapple` (1, TARGET_MELEE, skill `athletics`, defense `fortitude`, countsAsAttack). (Stand up/drop prone/escape come with the prone/grabbed conditions in Teil 5.)
  - `final class ActionDcs`:
    - `static int skillBonus(CharacterSheet sheet, String skill)` — `perception` → `CharacterMath.perception`, else `CharacterMath.skill(sheet, Skill)` by id (unknown id → 0).
    - `static int skillBonus(MonsterData m, String skill)` — `m.skills().get(skill)` if present; fallbacks *(plan decision, documented in code)*: `perception` → `m.perception()`; `athletics` → highest strike `attackBonus` − 2 (no strikes → `m.level() + 4`); `intimidation` → `m.perception()`; other → `m.level() + 4`.
    - `static OptionalInt defenseDc(CharacterSheet target, String defense)` and `defenseDc(MonsterData target, String defense)`: `reflex|fortitude|will` → 10 + the save bonus (`CharacterMath.save(sheet, Save.X)`, monster `m.reflex()` …); `perception_dc` → 10 + perception; unknown → empty.
    - `static int sizeRank(String size)` (tiny 0 … gargantuan 5) for later use; not used by actions in Teil 4.
- [ ] **Step 1: Write failing tests:** `BasicActionsTest`: `ALL` has the 7 ids above; `byId("trip")` → cost 1, TARGET_MELEE, `countsAsAttack`, defense `reflex`; `byId("demoralize")` → TARGET_RANGE with 6 blocks; `byId("raise_shield").needsShield()`; `byId("nope")` empty. `ActionDcsTest`: fighter5 (`422753-fighter5.json` via `PathbuilderImporter`): `skillBonus(sheet,"athletics") == 13`, `skillBonus(sheet,"intimidation") == 0`, `skillBonus(sheet,"perception") == 12`, `defenseDc(sheet,"reflex") == 19`, `defenseDc(sheet,"will") == 22`, `defenseDc(sheet,"bogus")` empty; kobold-warrior from the shipped catalog: `defenseDc(m,"reflex") == 10 + m.reflex()`, `skillBonus(m,"athletics")` equals its map entry if present else strike bonus − 2, `skillBonus(m,"intimidation")` equals perception when absent; a monster without strikes and without skills → `level + 4` for athletics.
- [ ] **Step 2:** Run `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add basic action definitions and target DC rules`.

---

### Task 2: Core temporary effects and the Step

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/CombatEffects.java`
- Modify: `core/src/main/java/de/pf2craft/core/combat/MovementBudget.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/CombatEffectsTest.java`, additions to `MovementBudgetTest`

**Interfaces:**
- Produces:
  - `final class CombatEffects { enum Expiry { OWN_TURN_START, MOVE_OR_OWN_TURN_START }; void addAcBonus(String combatantId, String source, int bonus, Expiry expiry); int acBonus(String combatantId) /* sum over distinct sources, same source replaced not stacked */; void onTurnStart(String combatantId); void onMoved(String combatantId); void clear(String combatantId); void clearAll(); List<String> sources(String combatantId) }`. Bonuses of different sources stack only up to **+2 circumstance maximum** (PF2: highest circumstance bonus applies): `acBonus` returns the **highest** single bonus among active effects, not the sum *(Remaster rule: same-type bonuses don't stack)*.
  - `MovementBudget.grantStep()` — a Step: sets `remainingInStride` to exactly 1 block **without** spending an action (the caller pays the action) and marks it as a step so `tryMove` does not charge another action while it is used; after the block is used the budget is empty.
- [ ] **Step 1: Write failing tests:** `CombatEffectsTest`: `shieldExpiresAtNextTurnStart` (add `raise_shield` +2 OWN_TURN_START → `acBonus == 2`; `onTurnStart("b")` for another id keeps it; `onTurnStart("a")` clears); `coverEndsOnMove` (`take_cover` +2 MOVE_OR_OWN_TURN_START → `onMoved("a")` clears; so does `onTurnStart`); `sameSourceIsReplacedNotStacked` (two `raise_shield` adds with bonus 2 and 3 → 3); `highestBonusWinsAcrossSources` (source `a` +1 and source `b` +3 → 3); `clearRemovesAll`; unknown id → 0. `MovementBudgetTest`: `stepGivesExactlyOneBlock` (speed 25, fresh turn with 3 actions: `grantStep()`; `tryMove(0.5)` twice → both true and `turn.actionsLeft()` still 3; the third `tryMove(0.5)` starts a normal Stride and spends 1 action → `actionsLeft()` 2); `stepDoesNotBreakRunningStride` (mid-Stride with 3 blocks left: `grantStep()` leaves `remainingInStride()` at 3 — a Step cannot shrink a larger remaining Stride); `stepAfterInterruptGivesOneBlock`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add combat effects and the one-block step`.

---

### Task 3: Server ActionService, pending targets, effects wiring

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/combat/ActionService.java`, `fabric/src/main/java/de/pf2craft/network/{ActionRequestPayload,PendingActionPayload}.java`
- Modify: `CombatService.java` (own `CombatEffects effects` field: `effects()` accessor; call `effects.onTurnStart(id)` when a turn starts — start and `nextTurn`; `effects.clear(id)` on remove/defeat/leave; `effects.clearAll()` on finish/reset; clear pending actions on those events), `Enforcement.java` (a current combatant's left-click on a living entity with a pending action calls `ActionService.executePending`; `moveCurrent` calls `effects.onMoved(id)` when a step was accepted), `AttackService.java` (target AC includes `effects.acBonus(target id)` for NPC and player targets), `CombatMessages.java`, `Pf2Networking.java`, `Pf2Commands.java` (`/pf2 combat action <id>` fallback: GM or any combatant — executes self actions directly and target actions against the creature looked at, GM acts for the selected current NPC when the sender is not the current combatant), lang files
- Test: pure helpers only if moved to core; Fabric glue is checked by bot

**Interfaces:**
- Consumes: Tasks 1–2; `CombatService.{turn(), budget(), currentId(), effects(), sync(null), encounter()}`; `AttackService.hint`; `CharacterMath`; `RollService.rollQuiet(String)`; `DegreeOfSuccess.determine`; `CharacterItems.weaponRef`/shield ref check for `needsShield` (player must hold an item with `Pf2Components.SHIELD` owned by them and have `sheet.shield()` present); `NpcService.lookedAt`; `GmSelection`.
- Produces:
  - `ActionRequestPayload(UUID actorId, String actionId, int targetEntityId /* −1 = none */)` C2S (actionId ≤ 24 chars). `PendingActionPayload(String actionId /* "" = none */)` S2C, sent when pending starts/clears (HUD hint).
  - `ActionService`:
    - `static void request(ServerPlayerEntity sender, ActionRequestPayload p)` — sender must be the actor (a combatant player) **or** a GM acting for an NPC actor; actor must be the current combatant; combat active; action id known; `actionsLeft ≥ cost`; `needsShield` satisfied; cooldown 300 ms per sender (stamped only after validation succeeded). SELF actions execute immediately. Target actions: if `targetEntityId < 0` → set the pending action (`PendingActionPayload` to the sender) and return; else validate target and execute.
    - `static void executePending(ServerPlayerEntity sender, Entity target)` — consumes the pending action of the sender (if any) and runs the same validation + execution; a failed validation keeps the pending state and shows a hint (target invalid / too far).
    - `static void clearPending(UUID sender)` and `static void clearAllPending()` (used by CombatService on turn change, end, leave).
    - Execution (after all validation, exactly then spend): `turn().spendActions(cost)`; then
      - `step`: `budget().grantStep()`; message „X macht einen Schritt“.
      - `raise_shield`: `effects.addAcBonus(id, "raise_shield", sheet.shield().acBonus(), OWN_TURN_START)`; message „X hebt den Schild“; GM line with the bonus.
      - `take_cover`: `effects.addAcBonus(id, "take_cover", 2, MOVE_OR_OWN_TURN_START)`; message „X geht in Deckung“.
      - `seek`: roll `1d20 + skillBonus(perception)` quietly; public line „X sucht“, the result **only to GMs** (natural, total).
      - skill actions with a target (`demoralize`, `trip`, `grapple`): MAP = `AttackMath.mapPenalty(turn.nextAttackNumber(), false)` only when `countsAsAttack` (then `recordAttack()` and `budget().interrupt()`); check = `1d20 + skillBonus(actor, skill) + map`; DC = `ActionDcs.defenseDc(target, defense)` (no DC → refuse before spending); degree by `DegreeOfSuccess.determine(total, dc, natural)`; reach: melee kinds ≤ 1 block hitbox-edge distance (+ epsilon 1e-6), `TARGET_RANGE` ≤ `rangeBlocks`; targets: any living `NpcEntity` or combatant player other than the actor.
      - Public line „X → <Aktion> gegen Y: Erfolg/kritischer Erfolg/Fehlschlag/kritischer Fehlschlag“ (coloured like attacks; **no DC, total, natural**); GM line adds natural, total, DC and a **condition hint** by action and degree: demoralize success/crit → „Ziel kann verängstigt 1/2 sein“, trip success/crit → „Ziel kann liegend sein“, trip crit failure → „Akteur kann liegend sein“, grapple success → „Ziel kann gepackt sein“ (crit → festgehalten), otherwise no hint. No condition is applied.
    - After a successful execution: `CombatService.sync(null)`, then the Task-8-style auto-end check that already exists for 0 actions runs as usual (call the existing helper after spending).
  - Attack-bonus display of the Teil 3 attack window is unchanged.
- [ ] **Step 1:** Implement service, payloads, receivers, wiring, commands, lang keys (both files).
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 3: Bot check** (dedicated server `./gradlew.bat :fabric:runServer` in the background; scratchpad `bot` with the patched mineflayer; GM bot (op + `/pf2 gm add`), player bot with `/pf2 character import 422753`, NPC `/pf2 npc spawn kobold-warrior`, combat started, bot current): `/pf2 combat action step` → actions 2 and the bot can walk exactly one block without further cost (then the next step costs an action); `action raise_shield` without a shield → red message, nothing spent (the imported 422753 has none: use `/pf2 sheet` data or a fixture with a shield — if no importable ID has a shield, test the refusal and say what could not be verified); `action take_cover` → a following attack by the GM-controlled NPC against the bot has +2 AC in the GM detail line, a bot step removes it; `action seek` → public line without numbers, GM bot sees the roll; `action trip` looking at the NPC (≤ 1 block) → result line, MAP uses the attack counter (after one `attack 1` the check shows −5 in the GM line); out of turn / no actions / unknown id → red message, nothing spent; pending flow via `/pf2 combat action demoralize` without a looked-at target → pending, then attack-click path cannot be driven by bots — verify by calling the fallback with a target. Record the transcript; state what bots cannot verify. Stop the server; commit nothing from `fabric/run` or the scratchpad.
- [ ] **Step 4:** Commit `Add action service with basic actions and temporary effects`.

---

### Task 4: Client menu, key V, pending hint

**Files:**
- Create: `fabric/src/client/java/de/pf2craft/client/{ActionMenuScreen,ClientActions}.java`
- Modify: `Pf2CraftClient.java` (key `key.pf2craft.action_menu` default `GLFW_KEY_V`, `PendingActionPayload` receiver, disconnect clears), `CombatHud.java` (hint line above the action line: „Aktion: Umstoßen – Ziel anklicken (Esc/V: abbrechen)“), lang files

**Interfaces:**
- Consumes: `ActionRequestPayload`, `PendingActionPayload` (Task 3), `ClientCombat` (actions left, whose turn, entries), `ClientSheets` (own sheet: shield present), `BasicActions` (core, usable on the client for ids/costs/kinds).
- Produces: `ClientActions` — `Set<String> favorites()` (persisted in `config/pf2craft-client.json`, read/write guarded with try/catch, Gson), `toggleFavorite(String id)`, `String pending()`, `void setPending(String id)`.
- Behaviour: **V** with no screen open: if it is not your turn (and you are not a GM with a selected current NPC — the GM case: the actor is the current NPC, known from the sync entries with `npc` and `current`) → action-bar hint „Du bist nicht dran“ (lang key); pending action set → V cancels it (client clears pending and sends `ActionRequestPayload(actor, "cancel", −1)` — add `cancel` handling in the server `request` as a special id that clears pending, costs nothing); otherwise opens `ActionMenuScreen`.
- `ActionMenuScreen` (vanilla look, legible at ~640×360 scaled px): left a category list (Grundaktionen, Klassenfähigkeiten, Zauber, Gegenstände), right the actions of the selected category, **favorites first** with a small ★ toggle per row; each row shows name, cost „◆“, and is greyed (with a tooltip reason: „keine Aktion übrig“, „kein Schild“, „nicht dein Zug“) when not payable; Klassenfähigkeiten shows „noch keine“, Zauber and Gegenstände are disabled with „folgt“. Click on an action → `ActionRequestPayload(actor, id, −1)` and close (target actions then wait for the click; SELF actions run at once). German/English action names via lang keys `action.pf2craft.<id>`.
- [ ] **Step 1:** Implement; lang keys in both files.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings; client start check (`runClient` in the background, log in the scratchpad, „Sound engine started“, no exceptions, kill) and a dedicated server smoke start (no client class loaded).
- [ ] **Step 3:** Commit `Add action menu screen and key V`. The screen is checked by the user.

---

### Task 5: Shield by item, NPC actions, docs

**Files:**
- Modify: `fabric/src/main/java/de/pf2craft/combat/{Enforcement,ActionService}.java` (right-click with the player's own shield item in the main/off hand during their turn = `raise_shield`; GM: `V` for the selected current NPC uses the same menu), `Pf2Commands.java` (`/pf2 combat action` for the GM's selected NPC already in Task 3 — verify), `README.md`, `CLAUDE.md`, lang files

- [ ] **Step 1:** `Enforcement`'s `UseItemCallback` for a current combatant holding an item with `Pf2Components.SHIELD` owned by them (check `CharacterItems`) → `ActionService.request(player, own uuid, "raise_shield", −1)` and cancel the vanilla use (shields are not usable otherwise); out of turn the existing block applies. For GM combatants and NPC actors nothing changes.
- [ ] **Step 2:** Verify NPC actions end to end by command: GM with a selected current NPC runs `/pf2 combat action trip` looking at a player/NPC (reach ≤ 1 block of the NPC), `seek`, `demoralize`, `take_cover`, `step` (NPC step: `budget().grantStep()` then the NPC's walk logic is not involved — the step for an NPC means „1 block movement allowed“; implement as: spend the action and tell the GM „NPC darf 1 Block gehen“, no automatic walk) — document what NPCs can do: all except `raise_shield` (no shield data → refused with `pf2craft.combat.no_shield`). Bot-check what is checkable.
- [ ] **Step 3:** README (German): „Aktionsmenü“ section (Taste V, Kategorien, Favoriten, Kosten, Ziel anklicken, Abbrechen, Schild-Rechtsklick), the list of basic actions with one line of what they do, effects (Schild/Deckung) and when they end, that results only produce hints until Teil 5, NPC actions via V with a selected current NPC and `/pf2 combat action <id>`, the command table row; „Bekannte Grenzen“: Aufstehen/Hinlegen/Befreien and all conditions come with Teil 5, no spells/items menu yet, Reaktionen fehlen, Schild-Block fehlt. CLAUDE.md: structure (`core.combat` additions, `client/ActionMenuScreen`), roadmap Teil 4 built (in-game test pending); „Bekannter Stand“ honest. Also in CLAUDE.md change „Ingame-Test des Nutzers steht aus“ of Teil 3 to the real state: tested by the user in single player (combat start, turns, auto turn end, attacks, GM staff incl. far reach) — multiplayer still untested.
- [ ] **Step 4:** `./gradlew.bat -PcoreOnly :core:test --console=plain` and `./gradlew.bat build --console=plain` → SUCCESS.
- [ ] **Step 5:** Commit `Add shield raising by item, NPC actions and document the action menu`.
