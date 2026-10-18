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
    npc/                          NpcEntity (Dorfbewohner-basiert), NpcService, NpcNetwork, GmSelection, CommandStaffItem
                                  (Befehlsstab), MonsterDataLoader (Jar + config/pf2craft/monsters/*.json; Vorlagen
                                  überschreiben nie Eingebautes, ID bekommt `-custom`, `-custom-2` …), Pf2Entities
    command/                      Pf2Commands (/roll, /gmroll, /pf2 gm|hp|sheet|items|tools|npc|character …)
  src/client/java/de/pf2craft/client  nur Client: Pf2CraftClient, DiceScreen, RollHud, ImportScreen,
                                  CharacterScreen (Reiter, Verdeckt, GM-Knöpfe), LevelUpScreen, ClientSheets,
                                  NpcSpawnScreen, NpcSheetScreen (+ NPC-Renderer)
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
7. Code-Stil: an den bestehenden Code anpassen (4 Leerzeichen, wenige, aber
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
  und per Unit-Tests in `core` abgedeckt; der Ingame-Test durch den Nutzer steht noch aus.
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
     `/pf2 npc …`, Vorlagen. *Gebaut; Ingame-Test durch den Nutzer steht noch aus.*
   - **Teil 3 – Kampfkern, Teil 4 – Aktionsmenü, Teil 5 – Zustände** (offen), umfassen:
   - GM startet und beendet einen Kampf, Initiative mit Zugreihenfolge im HUD.
   - 3 Aktionen + 1 Reaktion pro Zug, Mehrfachangriffsmalus.
   - Angriff gegen RK mit Erfolgsgrad, Schaden, TP.
   - Zustände (frightened, off-guard, …).
   - Raster: 1 Block = 5 ft, Bewegung nach Speed. Wer nicht dran ist, kann nicht handeln.
4. **Phase 4 – NPCs im Kampf:**
   - (Entity mit Werteblock und Spawn-Werkzeug sind seit Phase 3 Teil 2 gebaut.)
   - Steuerung im Kommandomodus (NPC anklicken → Ziel anklicken) und per „Übernehmen“ (Kamera/Steuerung in den NPC).
   - `/npc say` für Dialoge.
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
