# PF2 Craft

Pathfinder 2e als Pen-&-Paper-Runde in Minecraft spielen, gedacht für Multiplayer-Server.
Ein Spieler ist Spielleiter (GM) und steuert NPCs, die anderen spielen ihre Charaktere.

**Plattform:** Fabric · Minecraft 1.21.1 · Java 21. Server **und** alle Spieler brauchen die Mod
(plus [Fabric API](https://modrinth.com/mod/fabric-api)).

## Aktueller Stand (Phase 1 und 2)

### Würfelfenster
- Taste **R** (änderbar unter Steuerung → PF2 Craft) öffnet das Würfelfenster.
- **Linksklick** auf W4 / W6 / W8 / W10 / W12 / W20 / W100 fügt einen Würfel hinzu, **Rechtsklick** entfernt einen.
- Modifikator mit **− / +** (mit Shift in 5er-Schritten), optionale Bezeichnung (z. B. „Athletik“).
- Schalter **Verdeckt**: Nur du und die Spielleiter sehen das Ergebnis, alle anderen nur „X würfelt verdeckt“.
- **Würfeln!** oder Enter. Der Pool bleibt bis zum nächsten Öffnen erhalten.

Gewürfelt wird immer auf dem Server, damit niemand schummeln kann. Das Ergebnis erscheint
groß oben auf dem Bildschirm (natürliche 20 golden, natürliche 1 rot) und im Chat.

### Befehle (Ersatz zum Fenster)
| Befehl | Beschreibung |
|---|---|
| `/roll 1d20+7` | offener Wurf (`W` geht auch: `/roll 2W6+3`) |
| `/gmroll 1d20+7` | verdeckter Wurf |
| `/pf2 gm add <Spieler>` | Spieler zum Spielleiter machen (benötigt OP) |
| `/pf2 gm remove <Spieler>` | Spielleiter-Rolle entziehen |
| `/pf2 gm list` | Spielleiter online anzeigen |
| `/pf2 character import <ID>` | eigenen Charakter aus Pathbuilder 2e importieren (ersetzt einen vorhandenen) |
| `/pf2 character reset <Spieler>` | Charakter eines Spielers löschen (Spielleiter oder OP) |
| `/pf2 sheet <Spieler>` | Charakterbogen eines Spielers ansehen (Spielleiter oder OP) |
| `/pf2 hp <Spieler> <Menge>` | TP eines Spielers ändern, negativ = Schaden (Spielleiter oder OP) |

„Spielleiter oder OP“ heißt: Spieler mit GM-Rolle oder Berechtigungsstufe 2.

### Charaktere

**Erster Beitritt:** Wer noch keinen Charakter hat, startet als Zuschauer und bekommt das
Import-Fenster. Spielleiter behalten ihren Spielmodus.

**Import aus Pathbuilder 2e:** In Pathbuilder Menü → Export → *Export JSON* wählen, die
angezeigte 6-stellige ID in das Fenster eintragen und *Importieren* klicken (oder
`/pf2 character import <ID>`). Ein erneuter Import ersetzt den Charakter jederzeit.
Der Charakter wird auf dem Server gespeichert.

**Charakterbogen:** Taste **C** (änderbar unter Steuerung → PF2 Craft) öffnet ihn.
- Reiter **Übersicht** (Attribute, RK, TP, Rettungswürfe, Klassen-SG, Angriffe),
  **Fertigkeiten** (inklusive Wissen) und **Talente**.
- Ein Klick auf eine Fertigkeit, einen Rettungswurf oder einen Angriff würfelt den Wurf
  (serverseitig, mit Modifikator). Der Schalter **Verdeckt** macht den Wurf verdeckt.
- **Stufenaufstieg:** Der Spielleiter gewährt ihn im Charakterbogen („Aufstieg gewähren“).
  Danach erscheint beim Spieler „Aufsteigen“ und öffnet das Aufstiegsfenster
  (Attributssteigerungen, Fertigkeitssteigerung, Talent per Name), das mit *Bestätigen* abgeschlossen wird.

**Neu importieren:** Der Knopf **Neu importieren** unten im eigenen Charakterbogen öffnet das Import-Fenster erneut.

**Spielleiter-Ansicht:** `/pf2 sheet <Spieler>` öffnet den Bogen eines Spielers mit
Zusatzknöpfen: TP ändern oder setzen, Attributswerte ±1, Aufstieg gewähren.
Bearbeiten setzt die **GM-Rolle** voraus: Wer nur OP ist, sieht fremde Bögen schreibgeschützt.
Ein Klick auf den Rangbuchstaben vor einer Fertigkeit oder einem Rettungswurf ändert den
Übungsgrad (Klick = eine Stufe höher, **Shift**+Klick = eine Stufe niedriger).

**Eigene Klassen und Anpassungen:** Die Klassendaten (29 Klassen) liegen als JSON in der Mod.
Dateien in `config/pf2craft/classes/*.json` auf dem Server überschreiben sie oder ergänzen eigene Klassen.

### Bekannte Grenzen
- Taste **C** kollidiert mit der Vanilla-Belegung „Hotbar-Aktivator speichern“; eine der beiden Tasten unter Steuerung umbelegen.
- Talente werden nur mit Namen geführt, ihre Effekte werden nicht berechnet.
- Auswahlbasierte Klassenverbesserungen (z. B. Doktrinen, Waffenmeisterschaft, Wissensränge) muss der Spielleiter setzen.
- Zauber und Inventar gibt es noch nicht.
- Die Bezeichnungen der Würfe im Chat sind englisch.
- Steigt der INT-Modifikator, muss der Spielleiter die zusätzliche trainierte Fertigkeit von Hand setzen.
- Die geführte Charaktererschaffung im Spiel fehlt noch; Charaktere kommen aus Pathbuilder.

## Projektstruktur

```
core/    Regelkern in reinem Java (Würfel, Erfolgsgrade, Charaktere, Klassen-Aufstieg …), ohne Minecraft, mit Unit-Tests
fabric/  die eigentliche Mod: Netzwerk, Befehle, Würfelfenster, HUD, Charakterbogen
```

Der Regelkern wird direkt in die Mod-Jar kompiliert.

## Bauen

```bash
./gradlew build                       # Mod bauen → fabric/build/libs/pf2craft-<version>.jar
./gradlew :fabric:runClient           # Testclient starten
./gradlew :fabric:runServer           # Testserver starten
./gradlew -PcoreOnly :core:test       # nur Regelkern testen (ohne Minecraft-Download)
```

## Fahrplan

1. ✅ Regelkern-Grundlagen, Würfelfenster, GM-Rolle, `/roll`
2. ✅ Charakterbogen (Fenster), Import aus Pathbuilder 2e (JSON), Speicherung auf dem Server, Stufenaufstieg
   (offen: geführte Charaktererschaffung im Spiel)
3. Kampfmodus: Initiative, 3 Aktionen + Reaktion, Angriffe, Mehrfachangriffsmalus, Zustände
4. NPCs mit Werteblock, Steuerung und „Übernehmen“ durch den GM
5. Zauber, Gegenstände, mehr Inhalte, physische Würfel

## Lizenzhinweis

Pathfinder ist eine Marke von Paizo Inc. Die Spielmechanik von Pathfinder 2e (Remaster) steht unter
der ORC-Lizenz. Regeltexte von Archives of Nethys werden nicht kopiert.
