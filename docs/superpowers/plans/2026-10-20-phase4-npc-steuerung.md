# Phase 4 – NPC-Steuerung, Schreiten, Sprechblasen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stride becomes a deliberately chosen action with a toggleable range display; the GM can take over an NPC (first-person, WASD, V menu, attack); NPCs speak with chat lines and speech bubbles.

**Architecture:** Stride rules extend `core.combat.MovementBudget` (tests first). Server `Enforcement` holds a combatant on their block until a Stride is active, then limits movement to the stride's blocks. NPC control is a server-side `NpcControl` state (GM → NPC) fed by a per-tick client input payload; the client switches the camera entity to the NPC. Speech is a server broadcast payload rendered client-side above the NPC.

**Tech Stack:** Java 21, Fabric 1.21.1 (Yarn), Fabric API (camera via `setCameraEntity`, client world rendering events), JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-20-phase4-npc-steuerung-design.md`

## Global Constraints

- Code/comments/commits English; docs and chat German (CLAUDE.md). Commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Rules in `core` with tests first; server authoritative (validate sender, GM role, turn, distance, sizes); client-only code only under `fabric/src/client`; payloads registered in `Pf2Networking.registerPayloads()`.
- Texts via `Text.translatableWithFallback`, keys in BOTH `en_us.json` and `de_de.json`.
- Combat state in memory only. Vanilla damage stays off.
- Stride size: `speedFeet / 5` blocks (existing `MovementBudget.blocksPerStride`); the one-block `step` action stays unchanged.
- Build: `./gradlew.bat -PcoreOnly :core:test --console=plain`, `./gradlew.bat build --console=plain`; JAVA_HOME `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.

## Review Focus

- Player walks without having chosen Stride: must be held on the own block, no action spent, no rubber-band loop spam (message rate-limited).
- Walking past the stride limit must stop at the border, never start a second Stride, never charge an action.
- Combat end / turn end / disconnect while a stride or a take-over is active must clear the state (no stuck camera, no stuck NPC control).
- A GM who disconnects or loses GM role while controlling an NPC must release it; the GM's own body must not take damage or drift.
- Speech bubble text from `/npc say` or chat must be length-capped and sanitized (no formatting-code abuse); only GMs can trigger it.

---

### Task 1: Core stride rules

**Files:**
- Modify: `core/src/main/java/de/pf2craft/core/combat/MovementBudget.java`
- Test: `core/src/test/java/de/pf2craft/core/combat/MovementBudgetTest.java` (extend)

**Interfaces:**
- Produces on `MovementBudget`:
  - `boolean startStride(TurnState turn)` — spends 1 action and sets `remainingInStride = blocksPerStride`; returns false (nothing changed) if no action left or `blocksPerStride == 0`; if a stride is already active with remainder > EPSILON it returns false and changes nothing ("already striding").
  - `boolean strideActive()` — `remainingInStride > EPSILON`.
  - `boolean moveWithinStride(double blocks)` — returns false and changes nothing if no stride is active or `blocks > remainingInStride + 0.5` (one block of tolerance for a step across a boundary like the existing overshoot rule: allow overshoot by at most one step, i.e. `remaining -= blocks` and clamp at 0, when `remaining > EPSILON`).
  - existing `tryMove` stays for compatibility (used by tests and NPC fallback) but `Enforcement` stops calling it (Task 2). `payWalk`, `grantStep`, `interrupt`, `reset` unchanged.

- [ ] **Step 1: Write failing tests** — `startStrideSpendsOneActionAndFillsBudget` (speed 25 → remaining 5, actions 3→2), `startStrideRefusedWithoutActions`, `startStrideRefusedWhileActive`, `moveWithinStrideWithoutStrideIsRefused`, `moveWithinStrideConsumesBlocks` (5 → 3 after 2), `moveWithinStrideAllowsOneStepOvershootThenEnds` (remaining 0.4, move 1 → true, remaining 0, strideActive false), `grantedStepCanBeUsedWithoutStartingStride` (grantStep then moveWithinStride(1) true), `interruptEndsStride`.
- [ ] **Step 2:** `./gradlew.bat -PcoreOnly :core:test --console=plain` → FAIL.
- [ ] **Step 3:** Implement as specified. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add explicit stride start to the movement budget`.

### Task 2: Server – Stride action, enforcement, range data

**Files:**
- Modify: `core/.../BasicActions.java` (+`BasicActionsTest`): add `stride` as the FIRST entry (cost 1, SELF, no roll), `fabric/.../combat/ActionService.java`, `Enforcement.java`, `CombatService.java` (sync of stride state), `NpcWalk.java`, `CombatMessages.java`, `CombatSyncPayload.java` if the remaining-blocks field needs no change (it already carries `strideBlocksLeft`), both lang files

**Interfaces:**
- Consumes: Task 1 `startStride`, `strideActive`, `moveWithinStride`.
- Behaviour:
  - `ActionService` handler for `stride`: `CombatService.budget().startStride(turn)`; refused with message `pf2craft.combat.stride_active` when already striding, `noActions` when none left; public chat line like other actions; no interrupt of the stride marker (`stride` itself must not call `interrupt()`), other actions still interrupt the stride as today.
  - `Enforcement.moveCurrent`: if `!strideActive()` the current player is held on the block like `hold` (anchor = last accepted position) with a rate-limited hint `pf2craft.combat.choose_stride` ("Choose Stride in the action menu first"); with a stride active, movement goes through `moveWithinStride` instead of `tryMove`; at the border further movement is undone by teleporting back (existing mechanism) with the rate-limited hint `pf2craft.combat.stride_end`. A granted `step` (existing) still allows exactly one block without Stride.
  - NPC walking via the staff (`NpcWalk`) keeps `payWalk` (implicit stride) – unchanged.
  - GM controlling an NPC (Task 4) uses the same rules via `NpcControl`.
- [ ] **Step 1:** Extend `BasicActionsTest` (stride present, first, cost 1, SELF) → FAIL, then implement everything above with lang keys in both files.
- [ ] **Step 2:** Build → SUCCESS, no new warnings.
- [ ] **Step 3:** Bot check on a dedicated server (scratchpad `bot`, patched mineflayer): player's turn, walk 3 blocks without Stride → returned to start, no action spent, hint shown; `/pf2 combat action stride` then walk 6 blocks → stops at about 5, actions 3→2; `/pf2 combat action stride` again → refused while active ("already striding"); after reaching the border a second `stride` works and costs a second action.
- [ ] **Step 4:** Commit `Make Stride an explicit action`.

### Task 3: Client – Stride in menu, range display, toggle

**Files:**
- Modify: `ActionMenuScreen.java` (stride row first, greyed when active or no actions), `Pf2CraftClient.java` (new key `key.pf2craft.range_toggle`, default `GLFW_KEY_H`, plus Stride quick key `key.pf2craft.stride` default `GLFW_KEY_G` sending `ActionRequestPayload(actor, "stride", -1)`), `ClientActions.java` (config key `rangeDisplay`: true/false, default true), `CombatHud.java` (hint line when no stride active in own turn), both lang files
- Create: `fabric/src/client/java/de/pf2craft/client/RangeOverlay.java`

**Interfaces:**
- Consumes: `ClientCombat` (stride blocks left from the sync, `isTurnOf`), `BasicActions`.
- `RangeOverlay`: draws, while it is the own turn (or the controlled NPC's turn) and the overlay toggle is on, an outline on the ground of the area reachable: without an active stride a preview of `blocksPerStride` (needs speed from the own sheet / NPC data: use the value carried by the sync or the own sheet speed), with an active stride the remaining blocks; shape = square/circle of block radius around the anchor block (the player's block at stride start). Rendering technique is the builder's choice (world-render event with lines on block top faces, or dust particles); must not render when the toggle is off or no combat. Toggle key flips and shows an action-bar message (lang keys), persists via `ClientActions`.
- [ ] **Step 1:** Implement; both lang files. **Step 2:** Build → SUCCESS; `runClient` smoke start ("Sound engine started", no exceptions), dedicated server smoke start. **Step 3:** Commit `Add stride key, range display and toggle`.

### Task 4: Server – NPC take-over (NpcControl)

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/npc/NpcControl.java`, `fabric/src/main/java/de/pf2craft/network/TakeOverPayload.java` (client→server: `boolean take`), `fabric/src/main/java/de/pf2craft/network/ControlInputPayload.java` (client→server each client tick while controlling: `float forward, float sideways, boolean jump, boolean sprint, float yaw, float pitch`), `fabric/src/main/java/de/pf2craft/network/ControlStatePayload.java` (server→client: `UUID npcEntityUuid` or empty for released), register all in `Pf2Networking`
- Modify: `CombatService.java` (turn end / combat end / removal releases), `GmRoles` usage, `NpcEntity.java` if a control flag/no-AI mode is needed, `Pf2Commands.java` (`/pf2 npc control [release]` fallback), `CombatMessages.java`, lang files

**Interfaces:**
- `NpcControl` (static, in memory): `boolean take(ServerPlayerEntity gm)` — validates GM role, a selected NPC (`GmSelection`), in combat only if that NPC is the current combatant, within 128 blocks and same world; stores GM→NPC, switches NPC AI off, sets the GM to a protected state (invulnerable, no movement: reuse the hold/anchor, mark `GmRoles` so Enforcement lets the GM's own body stay), sends `ControlStatePayload(npcUuid)`; `void release(ServerPlayerEntity gm)`; `void releaseAll()`; `boolean controlling(UUID gm)`; `Optional<NpcEntity> controlled(UUID gm)`; `void input(ServerPlayerEntity gm, ControlInputPayload in)` — validates floats (finite, clamp −1..1, yaw/pitch ranges), applies the NPC's yaw/pitch and moves it with its own movement speed through vanilla `travel`/velocity (jump only if on ground), then applies the SAME rules as players: during combat each accepted step goes through `CombatService.budget().moveWithinStride(distance)`; without a stride or beyond the border the NPC is moved back to the last accepted position (a hold equivalent for NPCs), rate-limited hints go to the GM; outside combat no limits.
- Attack: the existing attack flow (`CommandStaffItem`/`AttackService` path for NPC attackers) must be reachable by the controlled NPC: a left-click payload already exists for players via item use? — builder: reuse `AttackService`'s NPC attack entry with the NPC as attacker and the NPC's crosshair target (ray cast from the NPC's eyes within its attack range) when the GM sends a new `ControlActionPayload(String kind)` with kind `attack` (add and register it in this task); the V menu uses the existing `ActionRequestPayload` with `actorId` = NPC uuid (GM may act for the current NPC already).
- Releasing: the GM presses the key again (TakeOverPayload(false)), the turn ends (hook in `CombatService.nextTurn`), combat ends/finish/removal of the NPC, GM disconnect, GM loses role → `release`/`releaseAll`; NPC AI restored.
- [ ] **Step 1:** Implement server logic and payloads; lang keys in both files.
- [ ] **Step 2:** Build → SUCCESS, no new warnings.
- [ ] **Step 3:** Bot check (GM bot): select an NPC, take over (via command fallback or payload), send `ControlInputPayload` forward for a few ticks outside combat → NPC position changes; in combat on the NPC's turn without Stride → no movement beyond its block and a hint; after `stride` movement works up to the limit; release works; turn end releases; non-GM `TakeOverPayload` refused.
- [ ] **Step 4:** Commit `Add server side NPC take-over`.

### Task 5: Client – camera, input, keys

**Files:**
- Modify: `Pf2CraftClient.java` (key `key.pf2craft.take_over`, default `GLFW_KEY_J`; receiver for `ControlStatePayload`; per-tick input sender), `CombatHud.java` / `ClientCombat.java` (use the controlled NPC as the actor for the V menu and the actions/stride display), lang files
- Create: `fabric/src/client/java/de/pf2craft/client/ClientControl.java`, optional mixin if the player's own movement must be suppressed (cancel the local player's movement input and attack/use while controlling)

**Interfaces:**
- `ClientControl`: `Optional<UUID> controlled()`, `onState(ControlStatePayload)`: sets `client.setCameraEntity(npcEntity)` (found by UUID in the client world; retry for a few ticks if not loaded) or restores the player; each tick while controlling it reads the vanilla key states (`client.options.forwardKey` … `jumpKey`, sprint) and the camera yaw/pitch (the local player's yaw/pitch continue to be turned by the mouse and are sent as the NPC's), sends `ControlInputPayload`, and zeroes the local player's own movement (so the GM body stays); left click sends `ControlActionPayload("attack")` instead of the staff/vanilla attack; F5 works as usual; HUD shows "Controlling: <name>" (lang keys).
- Key pressed → `TakeOverPayload(!controlling)`. Esc/disconnect restores the camera.
- [ ] **Step 1:** Implement; both lang files. **Step 2:** Build → SUCCESS, `runClient` smoke start, dedicated server smoke start. **Step 3:** Commit `Add client camera and input for NPC take-over`.

### Task 6: Dialogue – /npc say, chat as NPC, speech bubbles

**Files:**
- Create: `fabric/src/main/java/de/pf2craft/network/NpcSpeechPayload.java` (server→client: `UUID npcEntityUuid, String text` ≤ 120 chars), `fabric/src/main/java/de/pf2craft/npc/NpcSpeech.java`, `fabric/src/client/java/de/pf2craft/client/SpeechBubbles.java`
- Modify: `Pf2Commands.java` (`/pf2 npc say <text>`; also alias `/npc say` if the command tree allows a second root — otherwise only `/pf2 npc say`), `Pf2Networking.java`, chat interception in `NpcControl`/`NpcSpeech` via Fabric `ServerMessageEvents.ALLOW_CHAT_MESSAGE` (while a GM controls an NPC the message is cancelled and re-broadcast as `<NPC name> text`), lang files

**Interfaces:**
- `NpcSpeech.say(ServerPlayerEntity gm, NpcEntity npc, String text)`: GM-only; sanitizes (strip `§` codes, control characters, collapse whitespace), truncates to 120, broadcasts chat line `"%s: %s"` to all players and `NpcSpeechPayload` to players within 64 blocks.
- `SpeechBubbles` (client): stores up to one bubble per entity UUID for ~5 s (100 ticks), renders text over the NPC's head (billboard, wrapped to ~24 chars/line, max 4 lines, background pane) via `WorldRenderEvents` or an entity-renderer feature on the NPC renderer; bubbles disappear with the entity or on disconnect.
- [ ] **Step 1:** Implement; lang keys in both files. **Step 2:** Build → SUCCESS. **Step 3:** Bot check: GM runs `/pf2 npc say Hallo` with a selected NPC → all bots see chat line `Name: Hallo` and receive the speech payload; non-GM refused; 300-char text truncated; `§c` stripped. **Step 4:** `runClient` smoke start. **Step 5:** Commit `Add NPC dialogue and speech bubbles`.

### Task 7: Docs

**Files:** `README.md` (German: Schreiten wählen, Reichweitenanzeige und Tasten G/H, Übernehmen mit J, Steuerung, Sprechblasen und `/pf2 npc say`, Config-Schlüssel `rangeDisplay`, Bekannte Grenzen), `CLAUDE.md` (structure: `NpcControl`, `NpcSpeech`, client `ClientControl`/`RangeOverlay`/`SpeechBubbles`; roadmap Phase 4 built, in-game test pending; honest known limits).

- [ ] **Step 1:** Update both. **Step 2:** `./gradlew.bat -PcoreOnly :core:test` and `build` → SUCCESS. **Step 3:** Commit `Document stride, NPC take-over and speech bubbles`.
