# PF2 Craft

Pathfinder 2e als Pen-&-Paper-Runde in Minecraft spielen, gedacht für Multiplayer-Server.
Ein Spieler ist Spielleiter (GM) und steuert NPCs, die anderen spielen ihre Charaktere.

**Plattform:** Fabric · Minecraft 1.21.1 · Java 21. Server **und** alle Spieler brauchen die Mod
(plus [Fabric API](https://modrinth.com/mod/fabric-api)).

## Aktueller Stand (Phase 1)

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

## Projektstruktur

```
core/    Regelkern in reinem Java (Würfel, Erfolgsgrade …), ohne Minecraft, mit Unit-Tests
fabric/  die eigentliche Mod: Netzwerk, Befehle, Würfelfenster, HUD
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
2. Charakterbogen (Fenster), Import aus Pathbuilder 2e (JSON), Speicherung auf dem Server
3. Kampfmodus: Initiative, 3 Aktionen + Reaktion, Angriffe, Mehrfachangriffsmalus, Zustände
4. NPCs mit Werteblock, Steuerung und „Übernehmen“ durch den GM
5. Zauber, Gegenstände, mehr Inhalte, physische Würfel

## Lizenzhinweis

Pathfinder ist eine Marke von Paizo Inc. Die Spielmechanik von Pathfinder 2e (Remaster) steht unter
der ORC-Lizenz. Regeltexte von Archives of Nethys werden nicht kopiert.
