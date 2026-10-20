# CLAUDE.md – PF2 Craft

Projektdokumentation für Claude Code. Bitte vor jeder Aufgabe lesen.

## Ziel des Projekts

Eine Minecraft-Mod, mit der eine Gruppe **Pathfinder 2e** (Pen & Paper) auf einem
**Multiplayer-Server** spielt:

- Ein Spieler ist **Spielleiter (GM)** und steuert die **NPCs**.
- Die anderen Spieler spielen ihre Charaktere.
- Regelgrundlage: Archives of Nethys, https://2e.aonprd.com/ (Pathfinder 2e Remaster).

Der Nutzer spricht Deutsch. Antworten, Commit-Erklärungen im Chat und die Doku sind
deutsch. **Code, Kommentare, Bezeichner und Commit-Messages sind englisch.**

## Getroffene Entscheidungen

| Thema | Entscheidung | Begründung |
|---|---|---|
| Plattform | **Fabric**, Minecraft **1.21.1**, Java 21 | Eigene GUIs/HUD/Entities nötig → Mod statt Server-Plugin. Server und alle Spieler installieren die Mod. |
| Würfeln | **Würfelfenster** (Taste R) als Hauptweg | Vom Nutzer gewählt. `/roll` und `/gmroll` sind nur Fallback. |
| Würfellogik | **Immer serverseitig** | Gegen Schummeln. Der Client schickt nur den Wunsch (`RollRequestPayload`), der Server parst neu und würfelt. |
| GM-Rolle | Command-Tag `pf2craft_gm` am Spieler | Wird mit den Spielerdaten gespeichert. Vergeben mit `/pf2 gm add <Spieler>` (OP-Level 2). OPs sind **nicht** automatisch GM. |
| Verdeckte Würfe | Sehen nur der Würfelnde und alle GMs | Alle anderen erhalten „X würfelt verdeckt“. |
| Sprachen | Englisch + Deutsch (`en_us.json`, `de_de.json`) | Bei neuen Texten **immer beide** Dateien pflegen. In der deutschen Version heißen Würfel „W“ (W20), in der englischen „d“. |
| Charaktere | Import aus **Pathbuilder 2e** (JSON-Export, 6-stellige ID), Stufenaufstieg im Spiel nach Freigabe durch den GM | Bestätigt. Spart den Nachbau der Charaktererschaffung; geführte Erschaffung im Spiel ist als „Schritt B“ offen. |
| Inhalte (geplant) | Datengetrieben als JSON (Monster, Zauber, Gegenstände) | Die Gruppe soll selbst Inhalte ergänzen können. |
| Lizenz | Mechanik nach ORC-Lizenz umsetzen; Golarion-Eigennamen über die Paizo Community Use Policy (kostenlos, nicht kommerziell, Pflichthinweis, keine Paizo-Logos/-Bilder) | **Keine Regeltexte von AoN massenhaft kopieren.** Kurze eigene Zusammenfassungen sind in Ordnung. Pflichttexte stehen in `NOTICE.md`; die Mod darf nie Geld kosten. |

## Projektstruktur

```
core/                       Reines Java ohne Minecraft-Abhängigkeit. Regelkern + JUnit-5-Tests.
  de.pf2craft.core.dice     DiceExpression (Parser, d/W-Notation), RollResult, DieRoller
  de.pf2craft.core.check    DegreeOfSuccess (Erfolgsgrade inkl. nat. 20/1)
  de.pf2craft.core.character   Charakterbogen-Modell, CharacterMath, PathbuilderImporter, CharacterJson, CheckId, GmEdit, PathbuilderId/-Response
  de.pf2craft.core.progression ClassProgression (+ Parser), ClassRegistry, LevelUp
  de.pf2craft.core.item        Gegenstandskatalog (Waffen/Rüstung/Schilde, Parser), ItemVisual (Optik nach Schadenswürfel), ItemStamp
  de.pf2craft.core.combat   Kampfkern: Encounter (Initiative, Zugreihenfolge), TurnState, MovementBudget, AttackMath,
                            DamageResolver/Defenses/RuneDamage, RangeRules, HealthLabel,
                            Condition/ConditionSet/ConditionEffects (Zustände, Mali), DyingRules (Sterbend, Wiederherstellungswurf),
                            BasicActions/BasicAction/ActionKind/ActionDcs (Grundaktionen, SG), CombatEffects (zeitweilige RK-Boni)
  de.pf2craft.core.monster     Monster-Core-Daten (492 Kreaturen, Parser/Katalog), NpcStats, NpcEdit (GM-Änderungen)
  src/main/resources/pf2craft/monsters/  monster-core.json (nur Namen und Zahlen)
  src/main/resources/pf2craft/items/  Player-Core-Waffen, -Rüstungen, -Schilde (nur Namen und Zahlen)
  src/main/resources/pf2craft/classes/  Klassendaten (29 Klassen + index.json); Testdaten: 261 Pathbuilder-Fixtures
                            (MIT, MrPrimate/pathmuncher) in core/src/test/resources/pathbuilder/
fabric/                     Die Mod (Fabric Loom, splitEnvironmentSourceSets)
  src/main/java/de/pf2craft       gemeinsamer und Server-Code
    Pf2Craft                      ModInitializer, MOD_ID = "pf2craft", Pf2Craft.id(path)
    GmRoles                       GM-Rolle über den Command-Tag
    network/                      Payloads (CustomPayload-Records) + Pf2Networking
    roll/                         RollService (serverseitig würfeln/verteilen), RollMessages (Chat-Text)
    character/                    CharacterStore (Data Attachment), ClassData (Jar + config/pf2craft/classes/*.json),
                                  PathbuilderFetcher, CharacterService (Import/Reset/Aufstieg), SheetService (Bogen, TP, GM-Edits)
    item/                         Pf2ItemFactory (Vanilla-Items für Waffen/Schild), CharacterItems (geben/ersetzen/entfernen),
                                  ItemData (Jar + config/pf2craft/items/*.json), Pf2Components, WeaponRef/ShieldRef
    npc/                          NpcEntity (Dorfbewohner-basiert, DriveMoveControl nur bei Übernahme), NpcService, NpcNetwork, GmSelection, CommandStaffItem
                                  NpcControl (GM übernimmt NPC: Eingabe, Bewegungsregeln, Loslassen), NpcSpeech (Dialog/`/pf2 npc say`, Chat als NPC),
                                  (Befehlsstab), MonsterDataLoader (Jar + config/pf2craft/monsters/*.json; Vorlagen
                                  überschreiben nie Eingebautes, ID bekommt `-custom`, `-custom-2` …), Pf2Entities
    combat/                       CombatService (Kampfzustand, Züge), ConditionService (Zustände je Kämpfer im Speicher, Chat, GM-Edit),
                                  DyingService (Bewusstlos/Sterbend, automatischer Wiederherstellungswurf, „wäre tot“ nur an GMs), AttackService, Enforcement (Einfrieren),
                                  NpcWalk, CombatItem (Kampfglocke), CombatMessages,
                                  ActionService (Grundaktionen, wartende Zielaktionen, Schild per Rechtsklick)
    command/                      Pf2Commands (/roll, /gmroll, /pf2 gm|hp|sheet|items|tools|npc|character …)
  src/client/java/de/pf2craft/client  nur Client: Pf2CraftClient, DiceScreen, RollHud, ImportScreen,
                                  CharacterScreen (Reiter, Verdeckt, GM-Knöpfe), ConditionEditScreen/ClientConditions (Zustandsliste), LevelUpScreen, ClientSheets,
                                  ActionMenuScreen (Taste V), ClientActions, NpcSpawnScreen, NpcSheetScreen (der NPC-Renderer wird in Pf2CraftClient registriert),
                                  ClientControl (Kamera/Eingabe bei Übernahme, Taste J), RangeOverlay (Reichweitenkreis, Taste H), SpeechBubbles; Taste G = Schreiten
  src/client/resources/pf2craft.client.mixins.json  Client-Mixins (InGameHudMixin: TP statt Herzen/Hunger; ChatHudMixin; KeyboardInputMixin und MinecraftClientMixin für die Übernahme)
  src/main/resources/assets/pf2craft/lang/  en_us.json, de_de.json
```

`fabric/build.gradle` kompiliert `core/src/main/java` direkt mit in die Mod-Jar
(`sourceSets.main.java.srcDir`). Es gibt **keine** Abhängigkeit auf das `core`-Projekt.

## Architekturregeln

1. **Regellogik gehört in `core`**, nicht in `fabric`. Dort ist sie ohne Minecraft
   unit-testbar. `core` darf nichts aus `net.minecraft`/`net.fabricmc` importieren.
2. **Server ist autoritativ.** Clients senden nur Anfragen. Der Server validiert alles
   (Längen, Grenzen, Berechtigungen) und ändert dann den Spielzustand.
3. **Client-only-Code** (Screens, HUD, KeyBindings, `MinecraftClient`) nur in
   `src/client`. Sonst stürzt der Dedicated Server ab.
4. Netzwerk: neue Payloads als `record … implements CustomPayload` mit `ID` und `CODEC`,
   registriert in `Pf2Networking.registerPayloads()` (läuft auf beiden Seiten).
   Fabric führt die Handler auf dem Haupt-Thread aus.
5. Nachrichten an Spieler mit `Text.translatableWithFallback(key, englishFallback, …)`,
   damit auch Clients ohne Mod sinnvollen Text sehen.
6. Neue Texte: Schlüssel in **beiden** Sprachdateien eintragen.
7. Kampfzustand lebt nur serverseitig im Speicher und wird nicht gespeichert.
8. Code-Stil: an den bestehenden Code anpassen (4 Leerzeichen, wenige, aber
   aussagekräftige Kommentare, `final`-Utility-Klassen mit privatem Konstruktor).

## Bauen und Testen

```bash
./gradlew -PcoreOnly :core:test   # nur Regelkern (lädt kein Minecraft herunter)
./gradlew build                   # komplette Mod → fabric/build/libs/
./gradlew :fabric:runClient       # Testclient
./gradlew :fabric:runServer       # Testserver (beim ersten Start eula=true in fabric/run/eula.txt setzen)
```

- Vor jedem Commit: `./gradlew -PcoreOnly :core:test`. Falls Fabric erreichbar ist,
  zusätzlich `./gradlew build`.
- Neue Regellogik in `core` **immer mit Tests**.
- Versionen stehen in `gradle.properties`. Bei Build-Fehlern zuerst prüfen, ob
  `fabric_version`, `yarn_mappings`, `loader_version` und `loom_version` zu
  Minecraft 1.21.1 passen (https://fabricmc.net/develop).
- Optionales Ingame-Testen ohne Bildschirm: Dedicated Server starten
  (`online-mode=false` in `server.properties`) und mit einem **Mineflayer**-Bot
  (npm-Paket `mineflayer`) verbinden, um Befehle, Chat und Payloads zu prüfen.

### Bekannter Stand / offene Risiken

- Lokale Entwicklung unter Windows mit Eclipse Temurin JDK 21
  (`C:\Program Files\Eclipse Adoptium\jdk-21…`). Falls `java` nicht im PATH ist,
  `JAVA_HOME` auf diesen Ordner setzen.
- `./gradlew build` läuft ohne Fehler, `runClient` startet. Ingame im Einzelspieler
  getestet: Würfelfenster, `/roll`, verdeckte Würfe.
- Noch nicht getestet: Mehrspieler (Sichtbarkeit verdeckter Würfe für Nicht-GMs).
- Phase 2 (Charaktere) ist gebaut, per Unit-Tests in `core` abgedeckt und vom Nutzer im
  Einzelspieler getestet (Import, Bogen, Würfe, GM-Ansicht, Aufstieg, Reset): funktioniert.
- Bekannte Grenzen Phase 2: Taste C kollidiert mit Vanilla „Save Hotbar Activator“;
  Talente nur als Name (keine Effekte); auswahlbasierte Klassenverbesserungen (Doktrinen,
  Waffenmeisterschaft, Wissensränge) setzt der GM; keine Zauber/Inventar; Wurfbezeichnungen
  im Chat englisch; steigt der INT-Modifikator, muss der GM die zusätzliche trainierte Fertigkeit von Hand setzen.
- Phase 3 Teil 1 (Gegenstände) vom Nutzer im Einzelspieler getestet. Teil 2 (NPCs, Befehlsstab) ist gebaut
  und per Unit-Tests in `core` abgedeckt; vom Nutzer im Einzelspieler getestet (spawnen, auswählen, Werte, hinlaufen, hinlegen): funktioniert.
- Phase 3 Teil 3 (Kampfkern: Kampfglocke, Initiative, Aktionen/Bewegung, Einfrieren, Angriffe, NPC-Züge) ist gebaut und per
  Unit-Tests in `core` abgedeckt; vom Nutzer im Einzelspieler getestet (Kampfstart, Züge, automatisches Zugende, Angriffe,
  GM-Befehlsstab inkl. Fernreichweite). Mehrspieler mit echten Spielern und das Einfrieren auf echten Clients
  (Rubber-Banding möglich) sind ungetestet.
- Phase 3 Teil 4 (Aktionsmenü: Taste V, Grundaktionen Schreiten/Schild/Deckung/Suchen/Einschüchtern/Umstoßen/Ergreifen,
  wartende Zielaktionen, Schild per Rechtsklick, NPC-Aktionen, `/pf2 combat action`) ist gebaut und per Unit-Tests in `core`
  sowie Bot-Tests auf dem Dedicated Server abgedeckt; der Ingame-Test des Nutzers steht aus. Abbrechen nur mit V oder
  `/pf2 combat action cancel` (nicht Esc). Teil 4 ist in den Feature-Branch gemerged; Ingame-Test steht aus.
- Phase 4 (Schreiten als gewählte Aktion mit Taste G und Reichweitenkreis Taste H/`rangeDisplay`; NPC übernehmen mit Taste J bzw. `/pf2 npc control [release]`;
  Chat als NPC bei Übernahme, `/pf2 npc say`, Sprechblasen ~5 s, `SpeechText` in `core`) ist gebaut, per Unit-Tests in `core` und Bot-Tests auf dem Dedicated Server geprüft;
  der Ingame-Test des Nutzers steht aus. Kamera, Mauseingabe, Sprechblasen und Reichweitenkreis wurden nie auf einem echten Bildschirm gesehen.
  Bekannte Grenzen: keine Schreiten-Vorschau für einen übernommenen NPC (nur verbleibende Blöcke nach dem Schreiten); Kamera-Rotation kann hinterherhinken (Entity-Tracking);
  Mausrad/Zahlentasten ändern weiter den GM-Hotbar-Slot; Schreiten kann das Limit um bis zu 1 Block überschreiten; Übernahme bricht einen bezahlten Klick-Lauf ab (ohne Erstattung);
  Kreis um die aktuelle Position auf flachem Boden; Loslassen bei Disconnect/GM-Rollenverlust nicht getestet; Vorzeichen von A/D ungeprüft (ggf. in `ClientControl.tick` invertieren);
  kein zweiter `/npc`-Befehlsbaum; Bewegung ohne gewähltes Schreiten löst `onMoved` nicht aus (Deckung bleibt bis zum echten Schreiten).
- Bekannte Grenzen Teil 3/4: keine Reaktionen, kein Schild-Block, kein Zauber-/Gegenstandsmenü, keine Zauber, keine Sichtlinienprüfung,
  Munition/Nachladen nicht erzwungen, Kampf endet beim Serverneustart, `/tp` eines Kämpfers wird zurückgesetzt (außer
  `/pf2 combat anchor`).
- Phase 3 Teil 5 (Zustände: frightened, off_guard, prone, grabbed, slowed, stunned, unconscious, dying, wounded; Aufstehen/Befreien im Menü;
  Sterbend mit automatischem Wiederherstellungswurf, für alle sichtbar; „wäre tot“ nur an GMs; GM-Bearbeitung über `ConditionEditScreen` und
  `/pf2 combat condition <player|npc> <id> <value>`; HP-Text `HP: x/y · Zustände`) ist gebaut, per Unit-Tests in `core` und Bot-Tests abgedeckt;
  der Ingame-Test des Nutzers steht aus, die GUI wurde noch nicht visuell geprüft.
- Polish-Runde (nach Teil 5): `ActionMenuScreen` graut Aufstehen/Befreien aus; Schaden bei 0 TP erhöht Sterbend (`DyingRules.damageWhileDying`,
  Hook in `AttackService`); Disconnect eines Bewusstlosen behält seine Zustände bis Kampfende (`CombatService.onDisconnect/onJoin`);
  reine Bewusstlosen-Reihenfolge wartet 3 s pro Zug (`DOWNED_ONLY_DELAY`); `Enforcement` hält Bewusstlose auch auf dem eigenen Zug fest;
  Verwundet-Zeile beim Stabilisieren; Kampf-Chat kleiner (`ChatHudMixin`, Schlüssel `combatChatScale`/`combatChatHeight` in `config/pf2craft-client.json`)
  und Kampf-HUD rechts unten (`hudAnchor`: right/center/left). Diese Punkte sind gebaut und kompiliert, aber nicht ingame vom Nutzer geprüft
  (Chat-Mixin und HUD-Layout nur per Start und Koordinaten-Überlegung).
- Bekannte Grenzen Teil 5: keine Zauber-/Monsterfähigkeits-Zustände; Zustände nur im Speicher (Serverneustart beendet den Kampf);
  nur Verängstigt hat einen numerischen Statusmalus (wirkt auf Angriffe, RK, Aktionsproben und Bogenwürfe im Kampf);
  Verlangsamt/Benommen, Sterbend 4 und kritischer Niederschlag nicht per Bot getestet; Befreien scheitert, wenn die Packer-Entity fehlt, obwohl GRABBED bleibt.
- GM-Fernreichweite (`GmReach`): temporäre Attribut-Modifier (+96, `pf2craft:gm_far_reach`; Minecraft begrenzt die Reichweite-Attribute auf 64, effektiv gelten also 64 Blöcke) auf Entity-/Block-Reichweite, solange ein GM Befehlsstab/Kampfglocke hält; `CombatService.RADIUS` = 96, `NpcNetwork.MAX_DISTANCE` = 128. Per Bot geprüft (siehe `.superpowers/sdd/gm-far-reach-report.md`); das Fadenkreuz-Zielen eines echten Clients auf Distanz ist ungetestet.
- Bekannte Grenzen Teil 2: NPCs handeln nicht von selbst und sind noch nicht im Kampf nutzbar; Platzhaltermodell
  (Dorfbewohner); Strike-Boni im Werte-Fenster nicht editierbar; Zusatzeffekte (Grab, anhaltender Schaden)
  nur als Namen/Hinweise; Spawn-Schutz kann Blockklicks von Nicht-OP-GMs verschlucken.

## Fahrplan

1. ✅ **Phase 1:** Regelkern-Grundlagen (Würfel, Erfolgsgrade), Würfelfenster, HUD-Anzeige,
   GM-Rolle, `/roll`, `/gmroll`. *Noch offen: Test mit mehreren Spielern.*
2. ✅ **Phase 2 – Charaktere:** Charakterbogen (Taste C), Klick-zum-Würfeln über `RollService`,
   Pathbuilder-Import, Speicherung per Data Attachment, Stufenaufstieg nach GM-Freigabe, GM-Ansicht.
   *Noch offen: geführte Charaktererschaffung im Spiel („Schritt B“), Mehrspielertest,
   die bekannten Grenzen oben.*
3. **Phase 3 – Kampfmodus:**
   - ✅ **Teil 1 – Gegenstände:** Waffen und Schild aus dem Pathbuilder-Import als Vanilla-Items
     (Optik nach Schadenswürfel, Lore, Besitzer). *Vom Nutzer ingame getestet.*
   - ✅ **Teil 2 – NPCs:** Monster-Core-Daten, NPC-Entity, Befehlsstab (spawnen, auswählen, Werte, hinlaufen),
     `/pf2 npc …`, Vorlagen. *Vom Nutzer ingame getestet.*
   - ✅ **Teil 3 – Kampfkern:** gebaut. *Vom Nutzer im Einzelspieler getestet; Mehrspieler offen.*
   - ✅ **Teil 4 – Aktionsmenü:** gebaut und gemerged (Ingame-Test des Nutzers steht aus).
   - ✅ **Teil 5 – Zustände:** gebaut (Ingame-Test des Nutzers steht aus). Gesamtumfang Phase 3:
   - GM startet und beendet einen Kampf, Initiative mit Zugreihenfolge im HUD.
   - 3 Aktionen + 1 Reaktion pro Zug, Mehrfachangriffsmalus.
   - Angriff gegen RK mit Erfolgsgrad, Schaden, TP.
   - Zustände (frightened, off-guard, …) – seit Teil 5.
   - Raster: 1 Block = 5 ft, Bewegung nach Speed. Wer nicht dran ist, kann nicht handeln.
4. ✅ **Phase 4 – NPCs im Kampf:** gebaut (Ingame-Test des Nutzers steht aus).
   - ✅ Entity mit Werteblock und Spawn-Werkzeug (seit Phase 3 Teil 2).
   - ✅ Schreiten als gewählte Aktion (G) mit Reichweitenkreis (H); Steuerung im Kommandomodus (Befehlsstab) und per „Übernehmen“ (J, Kamera/Steuerung in den NPC).
   - ✅ Dialoge: Chat als NPC, `/pf2 npc say` (kein eigener `/npc`-Baum), Sprechblasen.
5. **Phase 5:** Zauber, Gegenstände, mehr Inhalte, physische Würfel (werfbares Item mit Rollanimation).

## Pathfinder-2e-Regeln, die schon feststehen

- **Probe:** W20 + Modifikator gegen SG (DC).
- **Erfolgsgrade:** ≥ SG+10 kritischer Erfolg, ≥ SG Erfolg, ≤ SG−10 kritischer Fehlschlag,
  sonst Fehlschlag. Eine natürliche 20 verbessert das Ergebnis um eine Stufe, eine natürliche 1
  verschlechtert es um eine Stufe. Umgesetzt in `DegreeOfSuccess`.
- **Übungsbonus:** untrainiert 0, sonst Stufe + 2 (trained), + 4 (expert), + 6 (master)
  oder + 8 (legendary).
- **Mehrfachangriffsmalus:** −5 / −10 (mit agile-Waffen −4 / −8).
- Im Zweifel die Regel auf https://2e.aonprd.com/ nachschlagen.

## Arbeitsweise

- Vor größeren Features kurz den Plan mit dem Nutzer abstimmen, besonders bei
  UI und Spielgefühl. Der Nutzer entscheidet über die Spielmechanik in Minecraft.
- Kleine, in sich geschlossene Commits mit englischen Messages.
- README.md (deutsch, für Spieler/Server-Admins) bei neuen Features oder Befehlen aktualisieren.
