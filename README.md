# PF2 Craft

Pathfinder 2e als Pen-&-Paper-Runde in Minecraft spielen, gedacht für Multiplayer-Server.
Ein Spieler ist Spielleiter (GM) und steuert NPCs, die anderen spielen ihre Charaktere.

**Plattform:** Fabric · Minecraft 1.21.1 · Java 21. Server **und** alle Spieler brauchen die Mod
(plus [Fabric API](https://modrinth.com/mod/fabric-api)).

## Aktueller Stand (Phase 1 bis 3, Teil 3)

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
| `/pf2 items <Spieler>` | Waffen und Schild eines Spielers aus dem gespeicherten Bogen neu ausgeben (Spielleiter oder OP) |
| `/pf2 hp <Spieler> <Menge>` | TP eines Spielers ändern, negativ = Schaden (Spielleiter oder OP) |
| `/pf2 tools` | Befehlsstab erhalten (nur Spieler mit GM-Rolle, OP allein genügt nicht) |
| `/pf2 npc spawn <id>` | NPC aus den Monsterdaten neben dir erzeugen, z. B. `goblin-warrior` (Spielleiter oder OP) |
| `/pf2 npc info` | Werte des ausgewählten (sonst angesehenen, max. 16 Blöcke) NPCs im Chat (Spielleiter oder OP) |
| `/pf2 npc remove` | ausgewählten (sonst angesehenen) NPC entfernen (Spielleiter oder OP) |
| `/pf2 npc hp <Wert>` | aktuelle TP des ausgewählten NPCs setzen (Spielleiter oder OP) |
| `/pf2 combat start` | Kampf starten: alle Charaktere und NPCs im Umkreis von 32 Blöcken, Initiative wird gewürfelt (Spielleiter oder OP) |
| `/pf2 combat status` | Runde, wer dran ist und die Reihenfolge anzeigen (Spielleiter oder OP) |
| `/pf2 combat next` | Zug überspringen (Spielleiter oder OP) |
| `/pf2 combat end` | Kampf beenden (Spielleiter oder OP) |
| `/pf2 combat add` | alle Charaktere und NPCs im Umkreis von 32 Blöcken zum laufenden Kampf hinzufügen (Spielleiter oder OP) |
| `/pf2 combat remove [<Ziel>]` | angesehenen NPC (oder das angegebene Ziel) aus dem Kampf nehmen (Spielleiter oder OP) |
| `/pf2 combat anchor <Spieler> [<Position>]` | Kämpfer an der aktuellen bzw. angegebenen Position neu verankern, z. B. nach einem `/tp` (Spielleiter oder OP) |
| `/pf2 combat endturn` | Zug beenden (Kämpfer, der dran ist; der Spielleiter auch den Zug eines NPCs) |
| `/pf2 combat attack <1-3> [<Angriff 0-7>]` | Ersatz für den Linksklick. Spieler: den angesehenen NPC (bis 16 Blöcke) mit der Waffe in der Hand angreifen. Spielleiter: der **ausgewählte NPC, der gerade dran ist,** greift das angesehene Wesen an (optional mit dem Monsterangriff 0-7) |

`/pf2 tools` gibt dem Spielleiter außerdem die **Kampfglocke**.

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

### Waffen und Ausrüstung

Beim Import aus Pathbuilder bekommt jeder Spieler seine Waffen und seinen Schild als Minecraft-Gegenstände
(Fäuste/unbewaffnete Angriffe ausgenommen).
- **Aussehen nach Schadenswürfel:** W4 Holz, W6 Gold, W8 Diamant, W10/W12 Netherit; die Form richtet sich
  nach der Waffengruppe (Schwert, Axt, Spitzhacke, Bogen, Armbrust, Dreizack, Stock). Schilde sind der Vanilla-Schild.
- **Tooltip:** Name wie „+1 Striking Rapier“, graue Zeilen mit Schaden (z. B. „1W6 Hieb“), Eigenschaften,
  Runen und Besitzer. Die Gegenstände nutzen sich nicht ab, können aber verloren gehen; der Spielleiter
  gibt sie mit `/pf2 items <Spieler>` neu aus (ohne neuen Import, Stufenaufstiege bleiben erhalten).
- **Neu importieren** ersetzt diese Gegenstände, **Charakter zurücksetzen** entfernt sie. Ist das Inventar voll,
  fallen sie vor die Füße des Spielers (mit Hinweis im Chat).
- **Eigene Gegenstände:** Dateien in `config/pf2craft/items/{weapons,armor,shields}.json` auf dem Server
  überschreiben die eingebauten Werte (Zuordnung über den Namen).

Lizenz- und Urheberhinweise stehen in [NOTICE.md](NOTICE.md).

### Befehlsstab (NPCs, nur Spielleiter)
Die NPCs stammen aus den Monsterdaten (Monster Core, 492 Kreaturen). `/pf2 tools` gibt dem Spielleiter den
**Befehlsstab** (er wird auch bei `/pf2 gm add` ausgegeben). Rechtsklick:
- **in die Luft:** Fenster „NPC hinzufügen“ (Monster suchen, anklicken, der NPC erscheint neben dir).
- **auf einen NPC:** wählt ihn aus, solange kein Angriff möglich ist (Anzeige über der Hotbar). **Linksklick** auf einen NPC wählt ihn immer aus. **Schleichen + Rechtsklick:** Werte-Fenster mit
  Bearbeitung der Werte, Umbenennen, „Als Vorlage speichern“ und „Entfernen“.
- **auf einen Block** (NPC ausgewählt): der NPC läuft dorthin. Im Kampf nur, wenn der NPC dran ist; das Laufen kostet Aktionen (siehe Kampf).

**Vorlagen:** Gespeicherte Vorlagen liegen in `config/pf2craft/monsters/` auf dem Server und erscheinen in der
Monstersuche. Gespeicherte Vorlagen überschreiben nie ein eingebautes Monster: Ist die ID vergeben, wird `-custom`,
`-custom-2` … angehängt. (Eine von Hand in den Ordner gelegte Datei mit der ID eines eingebauten Monsters ersetzt es
dagegen.) Alle `*.json`-Dateien in diesem Ordner werden geladen.

**Ersatzbefehle:** `/pf2 npc spawn <id>` (ohne Tab-Vervollständigung der IDs; besser das Stab-Fenster benutzen),
`/pf2 npc info` und `/pf2 npc remove` wirken auf den NPC, den du ansiehst (bis 16 Blöcke), sonst auf den
ausgewählten; `/pf2 npc hp <Wert>` setzt die TP des ausgewählten NPCs. Pro Welt sind höchstens 200 NPCs erlaubt.

**Besiegte NPCs:** Bei 0 TP legt sich ein NPC auf den Boden und scheidet aus der Kampfreihenfolge aus.

Hinweis: Blockklicks in der Spawn-Schutzzone des Servers (`spawn-protection`) erreichen die Mod bei Spielern ohne OP nicht.

### Kampf

**Starten (Spielleiter):** `/pf2 tools` gibt die **Kampfglocke**. Rechtsklick in die Luft öffnet das Kampf-Fenster
mit allen Charakteren und NPCs im Umkreis von 32 Blöcken. Teilnehmer an- oder abhaken, bei Bedarf über
„+ NPC hinzufügen…“ einen Gegner erzeugen (danach die Glocke erneut benutzen), dann **Initiative würfeln**.
Die Initiative ist W20 + Wahrnehmung; bei Gleichstand kommt der NPC vor dem Spieler, danach entscheidet die Wahrnehmung.
Im laufenden Kampf zeigt das Fenster die Teilnehmer: **Zug überspringen**, **Kampf beenden** (zweiter Klick bestätigt),
✕ nimmt einen Teilnehmer heraus, nicht beteiligte lassen sich hinzufügen.

**Anzeige:** Die Initiative-Leiste oben zeigt die Reihenfolge (wer dran ist, größer links) und die Runde. Spieler sehen bei
NPCs nur „unverletzt“, „verletzt“ oder „schwer verletzt“, der Spielleiter die Zahlen. Eine Zeile zeigt Aktionen (◆),
Reaktion und Bewegung (Blöcke); ist man dran, erscheint „Du bist dran“. Statt der Herzen und des Hungers zeigt die
Mod bei Spielern mit Charakter „TP: x/y“. Vanilla-Schaden, -Tod und -Hunger sind für alle Spieler abgeschaltet
(außer `/kill` und dem Void).

**Dein Zug:** 3 Aktionen und 1 Reaktion pro Zug. Wer nicht dran ist, ist eingefroren: Bewegung wird zurückgesetzt, Blöcke
abbauen, setzen, benutzen und Interaktionen mit Entities sind gesperrt. Bewegung wird in Blöcken gezählt (1 Block = 5 ft):
der erste Schritt beginnt ein Schreiten (1 Aktion) von Geschwindigkeit/5 Blöcken, danach ein weiteres, solange Aktionen übrig sind.
Mit **Enter** (änderbar unter Steuerung → PF2 Craft) beendest du den Zug früher; der Spielleiter beendet damit auch die Züge der NPCs.
Sind alle 3 Aktionen aufgebraucht (und ein begonnenes Schreiten zu Ende gelaufen), endet der Zug etwa 1 Sekunde später von selbst.
Ein Angriff unterbricht ein Schreiten: der Rest der Strecke verfällt, weitere Schritte beginnen ein neues Schreiten (1 Aktion).
Wartende Kämpfer können sich außerdem nicht bewegen oder springen (Tempo und Sprungkraft sind auf 0 gesetzt).

**Angriff:** Waffe deines Charakters in die Haupthand nehmen und auf deinem Zug einen NPC **links anklicken**. Im Fenster wählst du
den **1., 2. oder 3. Angriff** (der Mehrfachangriffsmalus ist eingerechnet, der passende Vorschlag ist hervorgehoben, jeder
kann gewählt werden, aber nicht weniger als deine tatsächliche Zahl an Angriffen in diesem Zug). Das Ergebnis („Treffer“, „kritisch“, „verfehlt“ …) und der verursachte Schaden gehen an alle;
RK, Wurf und TP des Ziels sieht nur der Spielleiter. Der Schaden wird nach Art berechnet (kritisch verdoppelt) mit Resistenzen,
Schwächen und Immunitäten; Runen wie flaming, frost, shock, corrosive und thundering geben +1W6. Es gelten Reichweiten
für Nah- und Fernkampf. Damit der Klick auch weit entfernte Ziele erreicht, erhalten Kämpfer und Spielleiter während eines Kampfes
eine vorübergehende Interaktionsreichweite von +12 Blöcken (wird nicht gespeichert, endet mit dem Kampf); ob ein Angriff möglich ist, entscheidet allein der Server. Unbekannte Verteidigungsarten (z. B. Kaltes Eisen) werden dem Spielleiter gemeldet, aber nicht angewendet.

**NPC-Züge (Spielleiter):** Ist ein NPC dran, wähle ihn mit dem Befehlsstab. Rechtsklick auf einen Block lässt ihn laufen
(in Schreiten bezahlt), Rechtsklick auf ein Ziel öffnet das Angriffsfenster mit den Angriffen des Monsters.

**0 TP:** Ein NPC gilt als besiegt (legt sich hin, scheidet aus). Ein Spieler wird inaktiv (eingefroren und übersprungen);
Sterbend und Wiederherstellungswurf kommen mit Teil 5. Der Kampf endet nur automatisch, wenn niemand mehr aktiv ist;
sonst erhalten Spielleiter den Hinweis „Der Kampf kann beendet werden“.

**Ersatzbefehle:** siehe die `/pf2 combat …`-Zeilen in der Befehlstabelle. Der Kampf wird nicht gespeichert und endet beim Serverneustart.
Einen Kämpfer an eine andere Stelle setzt der Spielleiter mit `/pf2 combat anchor`; ein `/tp` allein wird zurückgesetzt.

### Bekannte Grenzen
- Taste **C** kollidiert mit der Vanilla-Belegung „Hotbar-Aktivator speichern“; eine der beiden Tasten unter Steuerung umbelegen.
- Talente werden nur mit Namen geführt, ihre Effekte werden nicht berechnet.
- Auswahlbasierte Klassenverbesserungen (z. B. Doktrinen, Waffenmeisterschaft, Wissensränge) muss der Spielleiter setzen.
- Zauber und Inventar gibt es noch nicht.
- Kampf: keine Reaktionen (Reaktiver Schlag, Schild blocken), kein Aktionsmenü (Schild heben, Umstoßen … kommt mit Teil 4),
  keine Zustände, kein Sterbend/Wiederherstellungswurf (Teil 5), keine Zauber.
- Es gibt keine Sichtlinienprüfung; Munition und Nachladen werden nicht erzwungen.
- Der Kampf endet bei einem Serverneustart; ein `/tp` eines Kämpfers wird zurückgesetzt (außer mit `/pf2 combat anchor`).
- Das Einfrieren wurde nur mit Test-Bots geprüft; auf echten Clients kann es ruckeln (Rubber-Banding).
- Die Bezeichnungen der Würfe im Chat sind englisch.
- Steigt der INT-Modifikator, muss der Spielleiter die zusätzliche trainierte Fertigkeit von Hand setzen.
- Die geführte Charaktererschaffung im Spiel fehlt noch; Charaktere kommen aus Pathbuilder.
- NPCs handeln nicht von selbst, der Spielleiter steuert sie mit dem Befehlsstab.
- Das NPC-Modell ist ein Platzhalter (Dorfbewohner).
- Angriffsboni (Strikes) lassen sich im Werte-Fenster noch nicht ändern.
- Zusatzeffekte wie Ergreifen (Grab) oder anhaltender Schaden sind nur Namen bzw. Hinweise, ohne Regelwirkung.

## Projektstruktur

```
core/    Regelkern in reinem Java (Würfel, Erfolgsgrade, Charaktere, Klassen-Aufstieg, Gegenstandskatalog, Kampf …), ohne Minecraft, mit Unit-Tests
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
3. Kampfmodus (in Teilen):
   - ✅ Teil 1: Waffen und Schilde als Gegenstände
   - ✅ Teil 2: NPCs mit Monster-Core-Werten und Befehlsstab (gebaut, Ingame-Test steht aus)
   - ✅ Teil 3: Kampfkern (Initiative, 3 Aktionen + Reaktion, Angriffe, Mehrfachangriffsmalus; gebaut, Ingame-Test steht aus)
   - Teil 4: Aktionsmenü
   - Teil 5: Zustände
4. NPC-Steuerung im Kampf und „Übernehmen“ durch den GM
5. Zauber, Gegenstände, mehr Inhalte, physische Würfel

## Lizenzhinweis

Pathfinder ist eine Marke von Paizo Inc. Die Spielmechanik von Pathfinder 2e (Remaster) steht unter
der ORC-Lizenz. Regeltexte von Archives of Nethys werden nicht kopiert.
Die Mod ist kostenlos und nicht kommerziell; Details und Pflichthinweise (Paizo Community Use Policy, ORC) in [NOTICE.md](NOTICE.md).
