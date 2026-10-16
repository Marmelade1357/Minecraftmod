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
| Charaktere (geplant) | Import aus **Pathbuilder 2e** (JSON-Export) | Spart den Nachbau der Charaktererschaffung. Muss noch mit dem Nutzer bestätigt werden. |
| Inhalte (geplant) | Datengetrieben als JSON (Monster, Zauber, Gegenstände) | Die Gruppe soll selbst Inhalte ergänzen können. |
| Lizenz | Mechanik nach ORC-Lizenz umsetzen | **Keine Regeltexte von AoN massenhaft kopieren.** Kurze eigene Zusammenfassungen sind in Ordnung. |

## Projektstruktur

```
core/                       Reines Java ohne Minecraft-Abhängigkeit. Regelkern + JUnit-5-Tests.
  de.pf2craft.core.dice     DiceExpression (Parser, d/W-Notation), RollResult, DieRoller
  de.pf2craft.core.check    DegreeOfSuccess (Erfolgsgrade inkl. nat. 20/1)
fabric/                     Die Mod (Fabric Loom, splitEnvironmentSourceSets)
  src/main/java/de/pf2craft       gemeinsamer und Server-Code
    Pf2Craft                      ModInitializer, MOD_ID = "pf2craft", Pf2Craft.id(path)
    GmRoles                       GM-Rolle über den Command-Tag
    network/                      Payloads (CustomPayload-Records) + Pf2Networking
    roll/                         RollService (serverseitig würfeln/verteilen), RollMessages (Chat-Text)
    command/                      Pf2Commands (/roll, /gmroll, /pf2 gm …)
  src/client/java/de/pf2craft/client  nur Client: Pf2CraftClient, DiceScreen, RollHud
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

- Der `fabric`-Teil wurde in der Cloud-Umgebung geschrieben, in der
  `maven.fabricmc.net`, `libraries.minecraft.net` und `piston-meta.mojang.com` gesperrt
  waren. **Er wurde noch nie kompiliert oder gestartet.** Beim ersten Build mit
  Compile-Fehlern rechnen, vor allem bei Yarn-Namen und Fabric-API-Signaturen.
  Diese zuerst beheben.
- `core` ist getestet (23 Tests, alle grün).

## Fahrplan

1. ✅ **Phase 1:** Regelkern-Grundlagen (Würfel, Erfolgsgrade), Würfelfenster, HUD-Anzeige,
   GM-Rolle, `/roll`, `/gmroll`. *Noch offen: erster echter Build und Ingame-Test.*
2. **Phase 2 – Charaktere:**
   - Charakterbogen als Screen: Attribute, Übungsgrade, TP, RK, Rettungswürfe, Fertigkeiten.
   - Klick auf eine Fertigkeit oder einen Rettungswurf würfelt über den bestehenden `RollService`.
   - Import aus Pathbuilder 2e (JSON).
   - Serverseitige Speicherung (PersistentState o. Ä.).
3. **Phase 3 – Kampfmodus:**
   - GM startet und beendet einen Kampf, Initiative mit Zugreihenfolge im HUD.
   - 3 Aktionen + 1 Reaktion pro Zug, Mehrfachangriffsmalus.
   - Angriff gegen RK mit Erfolgsgrad, Schaden, TP.
   - Zustände (frightened, off-guard, …).
   - Raster: 1 Block = 5 ft, Bewegung nach Speed. Wer nicht dran ist, kann nicht handeln.
4. **Phase 4 – NPCs:**
   - Eigene Entity mit Werteblock aus JSON.
   - GM-Werkzeug zum Spawnen.
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
