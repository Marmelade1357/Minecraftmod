# Phase 3 Teil 5 – Zustände Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Conditions (verängstigt, hilflos, liegend, gepackt, verlangsamt, benommen, bewusstlos, sterbend, verwundet) are applied, computed, displayed and ended automatically in combat; dying/recovery runs on the server; the GM can override everything.

**Architecture:** Pure rules in `core.combat` (condition set, effect numbers, dying rules) with JUnit tests first. A server-side `ConditionService` keeps one `ConditionSet` per combatant id in memory (like `CombatEffects`), hooks into `CombatService` turn start/end, `AttackService`, `ActionService`, `Enforcement`, `SheetService`/`NpcService` (0 HP). Per-recipient sync adds a conditions string to `CombatSyncPayload.Entry`; the client shows it next to the HP text; GM edits go through one request payload.

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn), JUnit 5, existing PF2 Craft packages.

**Spec:** `docs/superpowers/specs/2026-10-20-phase3-teil5-zustaende-design.md` (builds on Teil 4: `ActionService`, `CombatEffects`, `BasicActions`).

## Global Constraints

- Code/comments/identifiers/commit messages English; docs and chat German (CLAUDE.md).
- Rules logic only in `core` (no `net.minecraft`); new rules always with tests; server authoritative, validate everything.
- Client-only code only under `fabric/src/client`; payloads are `record … implements CustomPayload` registered in `Pf2Networking.registerPayloads()`.
- New texts via `Text.translatableWithFallback`, keys in BOTH `en_us.json` and `de_de.json` (German dice "W").
- Combat state lives in memory only (not saved). Vanilla damage/death stay off.
- No AoN rule text copying: names and numbers only.
- Commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Build: `./gradlew.bat -PcoreOnly :core:test --console=plain`, `./gradlew.bat build --console=plain` (JAVA_HOME `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`).

## Review Focus

- Conditions on a creature that leaves/ends combat or disconnects must not leak into the next combat (clearAll/clear).
- Frightened 1 decrementing to 0 must remove the condition (no "Frightened 0" shown).
- Dying 4 must only REPORT death to the GM, never delete/kill; reporting once, not every turn.
- A healed player at 0 HP must leave unconscious/dying and become active again; wounded rises by 1.
- Same condition applied twice keeps the higher value (no stacking); status penalties of different conditions take only the worst.
- A player who is prone and wants to stay prone is never forced to stand; stand up costs 1 action.
- Hidden conditions (frightened, slowed …) must not reach other players' sync payloads.
- Unconscious/dying combatants cannot take actions or attacks (server-side), and recovery roll happens once per own turn start.

---

### Task 1: Core condition model and effects

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/Condition.java`, `ConditionSet.java`, `ConditionEffects.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/ConditionSetTest.java`, `ConditionEffectsTest.java`

**Interfaces:**
- Produces:
  - `enum Condition { FRIGHTENED(true), OFF_GUARD(false), PRONE(false), GRABBED(false), SLOWED(true), STUNNED(true), UNCONSCIOUS(false), DYING(true), WOUNDED(true); boolean valued(); String id(); boolean publicToAll(); static Optional<Condition> byId(String) }` — `id()` lowercase snake (`off_guard`); `publicToAll()` true only for PRONE, UNCONSCIOUS, DYING.
  - `ConditionSet`: `int value(Condition)` (0 = absent; unvalued present = 1), `void set(Condition, int value)` (≤0 removes; unvalued clamps to 1; raising keeps the higher: `apply(Condition,int)` keeps max, `set` overwrites), `void apply(Condition, int)`, `void remove(Condition)`, `boolean has(Condition)`, `void decrement(Condition)` (removes at 0), `Map<Condition,Integer> all()` (insertion order), `boolean isEmpty()`, `ConditionSet copy()`.
  - `ConditionEffects` (static): `int checkPenalty(ConditionSet)` = −(max of frightened, and sickened-style status penalties; frightened is the only one now → −frightened); `int acPenalty(ConditionSet)` = −frightened plus −2 if OFF_GUARD, PRONE, GRABBED or UNCONSCIOUS (circumstance −2 applies once); `int attackPenalty(ConditionSet)` = −frightened and additionally −2 if PRONE; `int actionsLost(ConditionSet)` = slowed value + stunned value capped at 3; `boolean canAct(ConditionSet)` = not UNCONSCIOUS and not DYING.

- [ ] **Step 1: Write failing tests** — `ConditionSetTest`: `applyKeepsHigher` (apply FRIGHTENED 2 then 1 → 2), `decrementRemovesAtZero` (frightened 1 → decrement → `!has`), `unvaluedClampsToOne`, `setZeroRemoves`, `allKeepsInsertionOrder`. `ConditionEffectsTest`: frightened 2 → checkPenalty −2, acPenalty −2, attackPenalty −2; prone → acPenalty −2, attackPenalty −2; prone+off-guard+grabbed → acPenalty −2 (not −6); slowed 1 + stunned 3 → actionsLost 3 (cap); dying → `canAct` false; empty set → all 0/true.
- [ ] **Step 2:** Run `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL (types missing).
- [ ] **Step 3:** Implement the three types as specified (final classes, private constructors for `ConditionEffects`).
- [ ] **Step 4:** Run the core tests → PASS.
- [ ] **Step 5:** Commit `Add condition model and effect numbers`.

### Task 2: Core dying rules

**Files:**
- Create: `core/src/main/java/de/pf2craft/core/combat/DyingRules.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/DyingRulesTest.java`

**Interfaces:**
- Consumes: `ConditionSet`, `Condition`, `DegreeOfSuccess` (`core.check`).
- Produces (static, all pure, mutate the passed `ConditionSet`):
  - `static void knockOut(ConditionSet s, boolean critical)` — sets UNCONSCIOUS and DYING = (critical ? 2 : 1) + WOUNDED value.
  - `static int recoveryDc(ConditionSet s)` = 10 + dying value.
  - `static RecoveryResult recover(ConditionSet s, DegreeOfSuccess d)` with `record RecoveryResult(boolean stabilized, boolean wouldBeDead)`: critical success → dying −2; success → −1; failure → +1; critical failure → +2. If dying reaches 0 → remove DYING, keep UNCONSCIOUS, WOUNDED +1, `stabilized=true`. If dying ≥ 4 → `wouldBeDead=true` (value stays capped at 4, nothing removed).
  - `static void heal(ConditionSet s)` — removes DYING and UNCONSCIOUS, WOUNDED +1 only if it was dying.

- [ ] **Step 1: Write failing tests** — `knockOutNormalGivesDying1`, `knockOutCriticalGivesDying2`, `woundedRaisesDying` (wounded 2 + normal → dying 3), `recoverSuccessLowersByOne`, `recoverCritSuccessLowersByTwo`, `recoverFailureRaises`, `recoverCritFailureRaisesTwo`, `stabilizingKeepsUnconsciousAndRaisesWounded`, `dyingFourReportsWouldBeDeadOnceAndKeepsCondition`, `healRemovesDyingAndUnconsciousAndRaisesWounded`, `recoveryDcIs10PlusDying`.
- [ ] **Step 2:** Run → FAIL. **Step 3:** Implement. **Step 4:** Run → PASS.
- [ ] **Step 5:** Commit `Add dying and recovery rules`.

### Task 3: Server ConditionService, wiring into attacks/AC/turns

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/combat/ConditionService.java`
- Modify: `CombatService.java`, `AttackService.java`, `ActionService.java`, `Enforcement.java`, `CombatMessages.java`, both lang files

**Interfaces:**
- Consumes: Task 1 and 2 types; `CombatService.effects()` pattern (static in-memory holder, cleared in `finish`/`reset`/`deactivate`/`remove`).
- Produces `ConditionService` (static):
  - `ConditionSet of(String id)` (creates empty on demand), `void clear(String id)`, `void clearAll()`.
  - `void add(String id, Condition c, int value, String source)` — applies via `ConditionSet.apply`, sends one chat line to all combatant recipients if `c.publicToAll()` else to the affected player and GMs; `void remove(String id, Condition c)` (+chat line); `boolean canAct(String id)`.
  - `void onTurnStart(String id)` — applies `actionsLost` to the new `TurnState` (spend that many actions immediately); stunned decrements by the actions lost; runs the dying recovery roll if DYING (Task 4 owns the roll; call `DyingService.recover(id)` placeholder-free: this task leaves the call out).
  - `void onTurnEnd(String id)` — `decrement(FRIGHTENED)`.
- Behaviour wiring:
  - `AttackService`: attacker's attack total adds `ConditionEffects.attackPenalty`, target's AC adds `acPenalty`; an attack by a combatant that `!canAct` is refused with a hint.
  - `ActionService.prepareActor`: refuse when `!canAct` (new message `cannotAct`); `demoralize` success/crit → target gets FRIGHTENED 1 (crit 2) via `ConditionService.add`; `trip` success → PRONE, crit → PRONE (+1d6 damage is NOT implemented); skill checks of the actor add `checkPenalty`.
  - `CombatService`: call `onTurnStart` / `onTurnEnd` from `nextTurn` around the pointer change; `clearAll` in `finish`/`reset`, `clear(id)` in `remove`.
  - `Enforcement`: a combatant that `!canAct` cannot use items/attack/interact (same cancel path as "not your turn").
- Existing `ActionService` special: the "Take cover" / "raise shield" effects stay in `CombatEffects`; conditions are separate.

- [ ] **Step 1:** Implement `ConditionService`; wire as above; lang keys for each chat line (`pf2craft.condition.<id>`, `pf2craft.condition.gained`, `pf2craft.condition.lost`, `pf2craft.combat.cannot_act`) in both files.
- [ ] **Step 2:** `./gradlew.bat build --console=plain` → SUCCESS, no new warnings.
- [ ] **Step 3:** Bot check on a dedicated server (scratchpad `bot`, patched mineflayer): `/pf2 combat action trip` on an NPC → PRONE in chat; frightened from `demoralize` decrements at the target's turn end; an NPC prone has AC −2 in the next attack roll line.
- [ ] **Step 4:** Commit `Apply conditions to attacks, AC, actions and turns`.

### Task 4: Dying/unconscious flow for players, defeat for NPCs

**Files:**
- Modify: `fabric/src/main/java/de/pf2craft/character/SheetService.java` (~line 229), `fabric/src/main/java/de/pf2craft/npc/NpcService.java` (~line 79), `ConditionService.java`, `AttackService.java`, `CombatMessages.java`, lang files
- Create: `fabric/src/main/java/de/pf2craft/combat/DyingService.java`

**Interfaces:**
- Produces `DyingService`: `void knockOut(ServerPlayerEntity p, boolean critical)` (sets conditions via `DyingRules.knockOut`, keeps the combatant active in the encounter order — it still gets turns for recovery rolls — but `canAct` is false; announces to all), `void recover(String id)` (called from `ConditionService.onTurnStart`: server rolls d20 vs `recoveryDc`, uses `DegreeOfSuccess`, applies `DyingRules.recover`, public chat line with roll, DC, degree and new dying value; on `wouldBeDead` sends a GM-only line "X would now be dead" once and stops further automatic dying changes for that combatant until the GM changes it), `void onHealed(String id)` (calls `DyingRules.heal`, chat line).
- Behaviour: player at 0 HP after damage → `knockOut` (critical when the damaging hit was a critical success) instead of `markDefeated`; HP raised above 0 → `onHealed`; the turn of a dying combatant is NOT skipped (recovery roll then auto turn end); NPCs at 0 HP keep today's `markDefeated`; GM can revive via existing path.

- [ ] **Step 1:** Implement `DyingService` and the hooks; lang keys (both files).
- [ ] **Step 2:** Build → SUCCESS.
- [ ] **Step 3:** Bot check: set a player's HP to 0 via `/pf2 hp` during combat → "dying 1" public line; on that player's turn a recovery line with roll and DC appears and the turn ends by itself; `/pf2 hp` back above 0 → conditions gone, wounded 1.
- [ ] **Step 4:** Commit `Add dying, recovery rolls and healing flow`.

### Task 5: Stand up, Release actions and grab

**Files:**
- Modify: `core/src/main/java/de/pf2craft/core/combat/BasicActions.java` (+ `BasicActionsTest`), `ActionService.java`, lang files

**Interfaces:**
- Produces new `BasicAction`s: `stand_up` (1 action, self, no roll; removes PRONE; refuses when not prone), `escape` (1 action, athletics or acrobatics vs the grabber's Fortitude DC via `ActionDcs`, only when GRABBED; success removes GRABBED, crit success also grants a free step via `grantStep`).
- `grapple` success → target GRABBED (crit: also the existing hint); GRABBED ends when the grabber moves (`Enforcement`/`NpcWalk` `onMoved` hook calls `ConditionService.onGrabberMoved(grabberId)` which removes GRABBED from everyone it grabbed — store `grabbedBy` map in `ConditionService`).
- Prone creature standing up is never automatic; a combatant that is prone and chooses not to act simply stays prone.

- [ ] **Step 1:** Extend `BasicActionsTest` (ids present, costs 1, `stand_up` self kind) → FAIL. **Step 2:** Implement action defs, ActionService handlers, lang keys (both). **Step 3:** Core tests + build → SUCCESS.
- [ ] **Step 4:** Bot check: prone → `/pf2 combat action stand_up` → condition removed, 1 action spent; stand_up while not prone → refused without spending.
- [ ] **Step 5:** Commit `Add stand up and escape actions`.

### Task 6: Sync, display and GM editing

**Files:**
- Modify: `CombatSyncPayload.java` (Entry gets `String conditions`, sent per recipient: all `publicToAll` conditions for everyone, all conditions for the owner and GMs), `CombatService.sync`, `CombatHud.java`, `ClientCombat.java`, `CharacterScreen.java`, `NpcSheetScreen.java`, `Pf2Commands.java`, lang files
- Create: `fabric/src/main/java/de/pf2craft/network/ConditionEditPayload.java` (client→server: `UUID target, String conditionId, int value`; value 0 = remove), registered in `Pf2Networking`

**Interfaces:**
- Consumes: `Condition.byId`, `ConditionService.add/remove`.
- Behaviour: HP text of the player's own HUD reads `HP: 42 · Verängstigt 1, Liegend` (translation keys `pf2craft.condition.<id>`); initiative bar shows public conditions next to each name; Character/NPC screens list all conditions for GM and owner with +/− buttons for GM (sends `ConditionEditPayload`); server checks sender is GM, target is combatant, value 0–4 (valued) / 0–1 (unvalued); `/pf2 combat condition <player|npc> <id> <value>` as fallback (GM, level 2).
- Dying 4 "would be dead" shown in the GM's chat only.

- [ ] **Step 1:** Implement payload, sync filtering, HUD and screens, command, lang keys (both files).
- [ ] **Step 2:** Build → SUCCESS; `runClient` smoke start ("Sound engine started", no exceptions) and dedicated-server start without client class errors.
- [ ] **Step 3:** Bot check: a non-GM bot sees `prone` of another player but not that player's `frightened` in its sync (log of payload or chat); GM command sets frightened 2 → HUD text via bot-visible status line; non-GM sending `ConditionEditPayload` is refused.
- [ ] **Step 4:** Commit `Show conditions and let the GM edit them`.

### Task 7: Docs and wrap-up

**Files:**
- Modify: `README.md` (German section "Zustände": list, automatic effects, dying and recovery rolls, "wäre tot"-Meldung, GM-Bearbeitung, Befehl, Sichtbarkeit), `CLAUDE.md` (structure: `core.combat` additions, `ConditionService`, `DyingService`; roadmap Teil 5 built, Ingame-Test pending; known limits: no spell conditions, no persistence, no reactions)

- [ ] **Step 1:** Update both documents honestly. **Step 2:** `./gradlew.bat -PcoreOnly :core:test` and `build` → SUCCESS. **Step 3:** Commit `Document conditions and dying rules`.
