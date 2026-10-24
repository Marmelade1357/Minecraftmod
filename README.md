# PF2 Craft

Pathfinder 2e als Pen-&-Paper-Runde in Minecraft spielen, gedacht für Multiplayer-Server.
Ein Spieler ist Spielleiter (GM) und steuert NPCs, die anderen spielen ihre Charaktere.

**Plattform:** Fabric · Minecraft 1.21.1 · Java 21. Server **und** alle Spieler brauchen die Mod
(plus [Fabric API](https://modrinth.com/mod/fabric-api)).

## Aktueller Stand (Phase 1 bis 5 Teil 1)

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
| `/pf2 rest <Spieler\|all>` | Rast: Zauberplätze und Fokuspunkte der Spieler auffüllen (Spielleiter oder OP) |
| `/pf2 spells reset <Spieler>` | Zauberplätze und Fokuspunkte eines Spielers zurücksetzen (Spielleiter oder OP) |
| `/pf2 spells grant <Spieler> <Zauber> <Rang>` | Spielleiter-/Testwerkzeug: Zauber (Katalog-ID oder Name, auch eigene) mit einem Rang ins Zauberbuch des Spielers legen (Spielleiter oder OP) |
| `/pf2 tools` | Befehlsstab erhalten (nur Spieler mit GM-Rolle, OP allein genügt nicht) |
| `/pf2 npc spawn <id>` | NPC aus den Monsterdaten neben dir erzeugen, z. B. `goblin-warrior` (Spielleiter oder OP) |
| `/pf2 npc info` | Werte des ausgewählten (sonst angesehenen, max. 16 Blöcke) NPCs im Chat (Spielleiter oder OP) |
| `/pf2 npc remove` | ausgewählten (sonst angesehenen) NPC entfernen (Spielleiter oder OP) |
| `/pf2 npc hp <Wert>` | aktuelle TP des ausgewählten NPCs setzen (Spielleiter oder OP) |
| `/pf2 npc say <Text>` | der ausgewählte NPC spricht: Chatzeile „Name: Text“ für alle und eine Sprechblase (nur Spielleiter) |
| `/pf2 npc control` / `/pf2 npc control release` | den ausgewählten NPC übernehmen bzw. wieder loslassen (Ersatz zur Taste J, nur Spielleiter) |
| `/pf2 combat start` | Kampf starten: alle Charaktere und NPCs im Umkreis von 32 Blöcken, Initiative wird gewürfelt (Spielleiter oder OP) |
| `/pf2 combat status` | Runde, wer dran ist und die Reihenfolge anzeigen (Spielleiter oder OP) |
| `/pf2 combat next` | Zug überspringen (Spielleiter oder OP) |
| `/pf2 combat end` | Kampf beenden (Spielleiter oder OP) |
| `/pf2 combat add` | alle Charaktere und NPCs im Umkreis von 32 Blöcken zum laufenden Kampf hinzufügen (Spielleiter oder OP) |
| `/pf2 combat remove [<Ziel>]` | angesehenen NPC (oder das angegebene Ziel) aus dem Kampf nehmen (Spielleiter oder OP) |
| `/pf2 combat anchor <Spieler> [<Position>]` | Kämpfer an der aktuellen bzw. angegebenen Position neu verankern, z. B. nach einem `/tp` (Spielleiter oder OP) |
| `/pf2 combat condition player <Spieler> <id> <0-4>` / `npc <id> <0-4>` | Zustand eines Kämpfers setzen (0 entfernt; Zustände ohne Wert nur 0 oder 1). `npc` nutzt den angesehenen bzw. ausgewählten NPC. Ids: `frightened`, `off_guard`, `prone`, `grabbed`, `slowed`, `stunned`, `unconscious`, `dying`, `wounded` (Spielleiter oder OP) |
| `/pf2 combat endturn` | Zug beenden (Kämpfer, der dran ist; der Spielleiter auch den Zug eines NPCs) |
| `/pf2 combat action <id>` | Grundaktion ausführen (`step`, `raise_shield`, `take_cover`, `seek`, `demoralize`, `trip`, `grapple`) oder `cancel`. Spieler: für sich selbst. Spielleiter: für den **ausgewählten NPC, der gerade dran ist**. Zielaktionen nutzen das angesehene Wesen, sonst warten sie auf einen Klick |
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
`/pf2 npc info` und `/pf2 npc remove` wirken auf den NPC, den du ansiehst (bis 16 Blöcke, mit Stab/Glocke in der Hand bis 64), sonst auf den
ausgewählten; `/pf2 npc hp <Wert>` setzt die TP des ausgewählten NPCs. Pro Welt sind höchstens 200 NPCs erlaubt.

**Besiegte NPCs:** Bei 0 TP legt sich ein NPC auf den Boden und scheidet aus der Kampfreihenfolge aus.

Hinweis: Blockklicks in der Spawn-Schutzzone des Servers (`spawn-protection`) erreichen die Mod bei Spielern ohne OP nicht.

**Fernreichweite:** Solange ein Spielleiter den Befehlsstab oder die Kampfglocke in der Hand hält, reichen seine Klicks
auf Blöcke und Kreaturen **64 Blöcke** weit (das ist das Maximum, das Minecraft für die Reichweite zulässt) (nur für Spielleiter; mit jedem anderen Item gilt die normale Reichweite,
Bauen und Abbauen bleiben unverändert). Ansehen/Auswählen, Hinlaufen und Angriffe gehen so aus der Ferne. Die
Befehle `/pf2 npc …` und der Ersatz-Angriff nutzen dieselbe Reichweite. Wie weit NPCs sichtbar sind, hängt auch von der
Sichtweite (`view-distance`) des Servers ab. Tipp: im Kreativmodus über dem Kampf fliegen.

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

**Dein Zug:** 3 Aktionen und 1 Reaktion pro Zug. Wer nicht dran ist, kann sich nur auf seinem Block bewegen (dem, auf dem sein Zug endete); verlässt er ihn, wird er zurückgesetzt. Blöcke
abbauen, setzen, benutzen und Interaktionen mit Entities sind gesperrt. Bewegung wird in Blöcken gezählt (1 Block = 5 ft):
Gehen ist erst erlaubt, nachdem du **Schreiten** gewählt hast (Taste **G** oder im Aktionsmenü, 1 Aktion): dann darfst du Geschwindigkeit/5 Blöcke laufen.
Ohne gewähltes Schreiten bleibst du auf deinem Block und bekommst einen Hinweis; ein zweites Schreiten ist erst möglich, wenn das erste aufgebraucht ist.
Das Ende der Strecke kann um bis zu einen Block überschritten werden, bevor der Server dich zurücksetzt.
Mit **Enter** (änderbar unter Steuerung → PF2 Craft) beendest du den Zug früher; der Spielleiter beendet damit auch die Züge der NPCs.
Sind alle 3 Aktionen aufgebraucht (und ein gewähltes Schreiten zu Ende gelaufen), endet der Zug etwa 1 Sekunde später von selbst.
Ein Angriff unterbricht ein Schreiten: der Rest der Strecke verfällt, danach musst du erneut Schreiten wählen.

**Reichweitenanzeige:** Auf deinem Zug zeichnet die Mod einen Kreis auf den Boden: vor dem Schreiten die Strecke, die ein Schreiten schaffen würde, während des Schreitens die noch verbleibenden Blöcke. Taste **H** schaltet die Anzeige an oder aus (gespeichert als `rangeDisplay` in `config/pf2craft-client.json`, Standard an; andere Schlüssel bleiben erhalten). Der Kreis ist um deine aktuelle Position gezeichnet und liegt flach auf Höhe deines Blocks.

**Angriff:** Waffe deines Charakters in die Haupthand nehmen und auf deinem Zug einen NPC **links anklicken**. Im Fenster wählst du
den **1., 2. oder 3. Angriff** (der Mehrfachangriffsmalus ist eingerechnet, der passende Vorschlag ist hervorgehoben, jeder
kann gewählt werden, aber nicht weniger als deine tatsächliche Zahl an Angriffen in diesem Zug). Das Ergebnis („Treffer“, „kritisch“, „verfehlt“ …) und der verursachte Schaden gehen an alle;
RK, Wurf und TP des Ziels sieht nur der Spielleiter. Der Schaden wird nach Art berechnet (kritisch verdoppelt) mit Resistenzen,
Schwächen und Immunitäten; Runen wie flaming, frost, shock, corrosive und thundering geben +1W6. Es gelten Reichweiten
für Nah- und Fernkampf. Damit der Klick auch weit entfernte Ziele erreicht, erhalten Kämpfer und Spielleiter während eines Kampfes
eine vorübergehende Interaktionsreichweite von +12 Blöcken (wird nicht gespeichert, endet mit dem Kampf); ob ein Angriff möglich ist, entscheidet allein der Server. Unbekannte Verteidigungsarten (z. B. Kaltes Eisen) werden dem Spielleiter gemeldet, aber nicht angewendet.

**NPC-Züge (Spielleiter):** Ist ein NPC dran, wähle ihn mit dem Befehlsstab. Rechtsklick auf einen Block lässt ihn laufen
(in Schreiten bezahlt), Rechtsklick auf ein Ziel öffnet das Angriffsfenster mit den Angriffen des Monsters.

**0 TP:** Ein NPC gilt als besiegt (legt sich hin, scheidet aus). Ein Spieler wird bewusstlos und sterbend (siehe „Zustände“) und bleibt in der Reihenfolge. Der Kampf endet nur automatisch, wenn niemand mehr aktiv ist;
sonst erhalten Spielleiter den Hinweis „Der Kampf kann beendet werden“.

**Kampf-Chat und Anzeige:** Solange ein Kampf läuft, ist der Chat kleiner (Textgröße ×0,7, geöffnete Höhe ×0,6; die Breite bleibt), damit Anzeige und Welt sichtbar bleiben.
Die Optionen im Spiel werden dabei nie verändert. Die Aktionszeile, der Hinweis auf die wartende Aktion und der eigene TP-Text stehen im Kampf **rechts unten** über der Hotbar.
Einstellbar in `config/pf2craft-client.json` (andere Schlüssel bleiben erhalten): `combatChatScale` (Standard 0.7), `combatChatHeight` (Standard 0.6), beide 0.3 bis 1.0,
und `hudAnchor` (`"right"` Standard, `"center"` oder `"left"`). Außerhalb des Kampfes bleibt der TP-Text, wo er war.

**Ersatzbefehle:** siehe die `/pf2 combat …`-Zeilen in der Befehlstabelle. Der Kampf wird nicht gespeichert und endet beim Serverneustart.
Einen Kämpfer an eine andere Stelle setzt der Spielleiter mit `/pf2 combat anchor`; ein `/tp` allein wird zurückgesetzt.

**Fernreichweite:** Mit der Kampfglocke oder dem Befehlsstab in der Hand reichen GM-Klicks 64 Blöcke weit (siehe Befehlsstab). Das Kampf-Fenster und `/pf2 combat start|add` erfassen Kämpfer im Umkreis von 96 Blöcken.

### Aktionsmenü

Auf deinem Zug öffnet Taste **V** (änderbar unter Steuerung → PF2 Craft) das Aktionsmenü. Links stehen die Kategorien
(Grundaktionen, Klassenfähigkeiten, Zauber, Gegenstände; Zauber und Gegenstände sind noch gesperrt, Klassenfähigkeiten leer), rechts die Aktionen der Kategorie.
Hinter jeder Aktion steht ihr Preis in Aktionen (◆). Der Stern vor dem Namen macht eine Aktion zum **Favoriten**; Favoriten stehen oben.
Nicht verfügbare Aktionen sind grau, ein Tooltip nennt den Grund (nicht dran, zu wenig Aktionen, kein Schild).

**Ziel anklicken:** Aktionen mit Ziel (Einschüchtern, Umstoßen, Ergreifen) warten nach dem Klick im Menü auf deinen **Linksklick** auf ein
Wesen. Ein ungültiges Ziel (zu weit weg, kein Kämpfer) zeigt nur einen Hinweis, die Aktion bleibt wartend. **Abbrechen** geht mit
**V** oder `/pf2 combat action cancel`; Esc bricht nicht ab. Endet der Zug oder reichen die Aktionen nicht mehr, verfällt die wartende Aktion.
Ein Spielleiter-Linksklick mit dem Befehlsstab auf ein ungültiges Ziel wählt wie gewohnt aus.

**Grundaktionen** (je 1 Aktion):
- **Schreiten** (Taste **G**): Geschwindigkeit/5 Blöcke laufen (siehe „Dein Zug“).
- **Schritt:** 1 Block gehen, ohne dass ein Schreiten beginnt.
- **Schild hochnehmen:** +RK-Bonus deines Schilds bis zum Beginn deines nächsten Zugs. Du musst den Schild deines Charakters in der Hand halten;
  ein **Rechtsklick mit dem Schild** auf deinem Zug ist dasselbe wie die Menüaktion (der Vanilla-Block ist abgeschaltet).
- **Deckung nehmen:** +2 RK, bis du dich bewegst oder dein nächster Zug beginnt.
- **Suchen:** Wahrnehmungsprobe; das Ergebnis sieht der Spielleiter.
- **Einschüchtern:** Einschüchtern gegen den Willens-SG des Ziels, bis 6 Blöcke.
- **Umstoßen:** Athletik gegen den Reflex-SG des Ziels, Nahkampf (1 Block); zählt für den Mehrfachangriffsmalus.
- **Ergreifen:** Athletik gegen den Zähigkeits-SG des Ziels, Nahkampf; zählt für den Mehrfachangriffsmalus.

Schild und Deckung sind **zeitweilige Effekte**: Sie erhöhen die RK in Angriffen, bis sie enden (siehe oben).

**Aufstehen** (1 Aktion, nur wenn liegend) und **Befreien** (1 Aktion, nur wenn gepackt) stehen ebenfalls bei den Grundaktionen.
Befreien ist Athletik oder Akrobatik (die bessere) gegen den Zähigkeits-SG des Packers; Erfolg beendet „Gepackt“, ein kritischer Erfolg gibt zusätzlich einen freien Schritt.
Greift die Aktion nicht (z. B. nicht liegend), lehnt der Server sie ab, ohne Aktionen zu verbrauchen. Das Menü graut diese beiden Aktionen aus, solange sie nicht möglich sind.

**NPC-Aktionen (Spielleiter):** Ist ein NPC dran und ausgewählt, öffnet **V** dasselbe Menü für ihn; ebenso gilt `/pf2 combat action <id>`.
NPCs können alle Grundaktionen außer **Schild hochnehmen** (keine Schilddaten, die Aktion wird abgelehnt). Beim Schritt wird nur die Aktion
bezahlt; der Spielleiter bekommt den Hinweis „darf 1 Block gehen“ und lässt den NPC selbst laufen.

### NPC übernehmen (Spielleiter)

Mit Taste **J** (änderbar unter Steuerung → PF2 Craft; Ersatz: `/pf2 npc control`, Loslassen mit `/pf2 npc control release`) übernimmt der Spielleiter den **ausgewählten NPC**.
Die Kamera wechselt in die **Ich-Perspektive** des NPCs; mit **F5** wechselt man wie gewohnt die Kameraansicht. Der eigene Körper bleibt stehen und ist geschützt.
- **Steuerung:** WASD, Springen und Sprinten bewegen den NPC, die Maus dreht ihn. Der **Linksklick** greift an (Strahl aus den Augen des NPCs, bis 64 Blöcke) und öffnet das Angriffsfenster wie beim Befehlsstab.
  **V** öffnet das Aktionsmenü des NPCs, **G** wählt Schreiten. Blöcke abbauen, setzen und benutzen ist währenddessen gesperrt.
- **Regeln:** Es lässt sich nur ein NPC pro Spielleiter übernehmen (nicht besiegt, in derselben Welt, unter 64 Blöcken Entfernung; entfernt sich der NPC später auf über 80 Blöcke, wird die Steuerung beendet). Im Kampf geht das nur, solange der NPC am Zug ist; es gelten dieselben Bewegungsregeln wie für Spieler
  (ohne gewähltes Schreiten bleibt der NPC auf seinem Block). Außerhalb des Kampfes gibt es keine Grenzen.
- **Loslassen** passiert bei erneutem Druck auf J, bei Zugwechsel, Kampfende, Entfernen oder Besiegen des NPCs, Verlust der GM-Rolle und beim Trennen der Verbindung.
- Übernehmen bricht einen bezahlten Klick-Lauf des NPCs ab; die bezahlten Aktionen werden nicht erstattet.

### Dialoge und Sprechblasen

Während du einen NPC übernommen hast, wird dein **Chat zur Rede des NPCs**: aus deiner Nachricht wird die Zeile „NPC-Name: Text“ für alle (Vanilla-Chat wirkt erst nach dem Loslassen wieder).
Ohne Übernahme geht dasselbe mit `/pf2 npc say <Text>` für den ausgewählten NPC. Zusätzlich erscheint für Spieler im Umkreis von 64 Blöcken etwa **5 Sekunden** lang eine Sprechblase über dem NPC
(höchstens 120 Zeichen, auf 4 Zeilen umgebrochen; Formatierungscodes werden entfernt). Die eigene Blase des übernommenen NPCs wird in der Ich-Perspektive nicht gezeigt.

### Zustände

Zustände gelten im Kampf **sofort und automatisch**; der Spielleiter kann jeden Zustand jederzeit überstimmen.

**Liste:** Verängstigt (Wert), Off-guard, Liegend, Gepackt, Verlangsamt (Wert), Benommen (Wert), Bewusstlos, Sterbend (Wert), Verwundet (Wert).

**Automatische Wirkung:**
- **Verängstigt N:** −N auf Angriffe, RK, Aktionsproben und die Würfe des eigenen Charakterbogens während des Kampfes (nur dieser Statusmalus ist numerisch umgesetzt).
  Sinkt am Ende des eigenen Zuges um 1. Erfolgreiches **Einschüchtern** gibt Verängstigt 1 (kritisch 2).
- **Off-guard, Liegend, Gepackt, Bewusstlos:** −2 RK (zählt nur einmal). **Liegend:** zusätzlich −2 auf Angriffe. Erfolgreiches **Umstoßen** macht das Ziel liegend;
  erfolgreiches **Ergreifen** macht es gepackt. Gepackt endet, wenn der Packer sich bewegt (oder das Ziel sich befreit).
- **Verlangsamt / Benommen:** nehmen zu Beginn des Zuges Aktionen weg (zusammen höchstens 3).
- **Bewusstlos / Sterbend:** keine Aktionen, keine Angriffe, kein Bewegen; der Zug endet von selbst.

**Sterbend:** Fällt ein Spieler auf 0 TP, wird er bewusstlos und sterbend 1 (bei kritischem Treffer 2; mit Verwundet kommt dessen Wert dazu).
Zu Beginn seines eigenen Zuges würfelt der **Server automatisch** den Wiederherstellungswurf (einfache Probe gegen SG 10 + Sterbend-Wert, natürliche 20/1 verschieben den Erfolgsgrad).
**Würfel, SG und Ergebnis sehen alle** im Chat. Stabilisiert er sich, bleibt er bewusstlos und würfelt nicht weiter; die Verwundet-Stufe wird als eigene Zeile für alle gemeldet.
Erleidet ein Bewusstloser erneut Schaden, steigt Sterbend um 1 (bei kritischem Treffer um 2, höchstens 4). Bewusstlose und Sterbende können sich nicht bewegen, auch nicht auf ihrem eigenen Zug.
Trennt ein Bewusstloser die Verbindung, bleiben seine Zustände für den Rest des Kampfes erhalten; nach dem Wiedereinstieg würfelt er weiter. Sind nur noch Bewusstlose aktiv, vergehen etwa 3 Sekunden pro Wurf. Heilung auf mehr als 0 TP beendet Bewusstlos/Sterbend und gibt Verwundet +1.
Erreicht Sterbend den Wert 4, meldet das Spiel **nur den Spielleitern** „X wäre jetzt tot“. Der Spielleiter entscheidet; nichts wird automatisch gelöscht oder beendet.
**NPCs** auf 0 TP gelten sofort als besiegt (am Boden, aus der Reihenfolge), ohne Sterbend-Regeln; der Spielleiter kann sie wieder aufrichten.

**Sichtbarkeit:** Liegend, Bewusstlos und Sterbend (und besiegt) sehen alle. Alle übrigen Zustände sieht nur der Betroffene und der Spielleiter; Zustände von NPCs (außer den öffentlichen) nur der Spielleiter.
Chat-Meldungen beim Entstehen und Enden folgen derselben Regel. Der eigene TP-Text hängt die eigenen Zustände an: `TP: 42/50 · Verängstigt 1, Liegend`. Die Initiative-Leiste zeigt die sichtbaren Zustände je Teilnehmer.

**Bearbeiten (Spielleiter):** Der Knopf **Zustände** im Charakterbogen (Spieler) bzw. im NPC-Fenster öffnet eine Liste mit + und − je Zustand. Spieler sehen ihre eigene Liste nur lesend.
Dasselbe geht per Befehl, siehe `/pf2 combat condition …`. Bewusstlos entfernen beendet auch Sterbend; Sterbend setzen macht bewusstlos.
Bearbeiten geht nur im laufenden Kampf und nur bei Kämpfern.

### Zauber

Zauber kommen mit dem Pathbuilder-Import in den Charakter (Zaubertradition, Attribut, Plätze pro Rang, Zauber je Rang, Fokuspunkte).
Ein erneuter Import setzt die verbrauchten Plätze zurück.

**Reiter Zauber** im Charakterbogen (Taste **C**): je Zauberliste Tradition, Typ, Zauberangriff und Zauber-SG
(Attribut plus Übungsrang, wie bei Fertigkeiten), dann die Zauber nach Rang mit den übrigen Plätzen und die Fokuspunkte.
Ein Knopf **Wirken** startet den Zauber; nicht wirkbare Zauber sind ausgegraut, der Mauszeiger nennt den Grund.
Der Spielleiter sieht bei fremden Bögen die Liste nur lesend plus den Knopf **Plätze zurücksetzen** (der Spieler muss online sein).

**Wirken:** Entweder über den Reiter oder im Aktionsmenü (Taste **V**) → Kategorie **Zauber**.
- **Ziele:** Zauber mit Zielen warten danach auf einen Klick. Das Wesen wird per Fadenkreuz angeklickt (Sichtlinie bis 64 Blöcke;
  Blöcke blockieren); der Server prüft die Reichweite des Zaubers selbst. Bei mehreren Zielen mehrere Wesen anklicken und mit **B** bestätigen.
  **V** (oder `/pf2 combat action cancel`) bricht ab. Ein Zauber auf dich selbst wirkt sofort.
- **Kosten im Kampf:** Der Zauber kostet seine Aktionen (meist 2) und ist nur auf deinem Zug möglich. Ohne Kampf sind Zauber frei wirkbar.
- **Auflösung:** Der Server würfelt automatisch: Zauberangriff gegen RK, Rettungswurf des Ziels gegen deinen Zauber-SG (mit Grundrettungswurf, wo vorgesehen),
  Heilung oder Schaden, mit Erfolgsgraden und Anpassung der TP.
- **Zaubertricks** (Rang 0) kosten keinen Platz und werden automatisch gesteigert.
- **Fokuszauber** kosten 1 Fokuspunkt.
- **Spontane Zauberer** wählen beim Wirken den Rang (Knopf im Reiter, nur Ränge mit freiem Platz); der Zauber wird entsprechend gesteigert.
- **Zauber ohne Daten** (nicht im Katalog): Die Mod verbraucht den Platz; die Wirkung entscheidet der Spielleiter.
- **Rast:** `/pf2 rest <Spieler|all>` füllt Zauberplätze und Fokuspunkte auf. `/pf2 spells reset <Spieler>` setzt einen einzelnen Spieler zurück.
- `/pf2 spells grant <Spieler> <Zauber> <Rang>` legt einen Zauber ins Buch (Spielleiter-/Testwerkzeug, funktioniert auch mit eigenen Zaubern).

**Startkatalog (25 Zauber, nur Namen und Zahlen):** Electric Arc, Force Barrage, Heal, Harm, Fireball, Breathe Fire, Ignition, Frostbite,
Caustic Blast, Needle Darts, Telekinetic Projectile, Divine Lance, Gale Blast, Slashing Gust, Thunderstrike, Chilling Spray, Daze, Fear,
Shield, Sure Strike, Bless, Lightning Bolt, Horizon Thunder Sphere, Spout, Puff of Poison.

**Eigene Zauber:** Dateien `config/pf2craft/spells/*.json` im Serverordner (beim Serverstart geladen, alphabetisch). Jede Datei ist ein JSON-Array;
Einträge mit gleicher `id` überschreiben Einträge des Startkatalogs oder früherer Dateien. Fehlerhafte Einträge werden mit einer Meldung im Log übersprungen.

```json
[
  {"id": "acid-splash", "name": "Acid Splash", "rank": 0, "actions": 2, "rangeFeet": 30, "maxTargets": 1,
   "kind": "ATTACK", "damageDice": "2d4", "damageType": "acid", "heightenDice": "1d4", "heightenStep": 1}
]
```

| Feld | Bedeutung |
|---|---|
| `id` (Pflicht) | eindeutige Kennung, z. B. `acid-splash` |
| `name` (Pflicht) | Anzeigename, wie im Zauberbuch (Pathbuilder) |
| `rank` | Zauberrang, 0 = Zaubertrick (0 bis 10) |
| `actions` | Aktionen 1 bis 3 (Standard 2) |
| `rangeFeet` | Reichweite in Fuß, 0 = Berührung/selbst |
| `maxTargets` | 0 = nur du selbst, 1 = ein Ziel, mehr = Mehrfachziel (Bestätigen mit B) |
| `kind` | `ATTACK` (Zauberangriff), `SAVE` (Rettungswurf), `AUTO` (trifft automatisch), `HEAL`, `UTILITY` (kein Wurf, Spielleiter entscheidet) |
| `save` | `fortitude`, `reflex` oder `will` (bei `SAVE`) |
| `basicSave` | `true` = Grundrettungswurf |
| `damageDice` | Würfel wie `2d4` (W4, W6, W8, W10, W12, W20) |
| `damageBonus` | fester Zusatzschaden bzw. feste Heilung |
| `damageType` | Schadensart (z. B. `fire`, `force`, `sonic`; bei `HEAL` leer) |
| `addAbilityMod` | `true` = Zauberattribut-Modifikator wird auf den Schaden addiert |
| `heightenDice` | zusätzliche Würfel je Steigerungsschritt |
| `heightenStep` | Ränge pro Schritt (1 oder 2), 0 = keine Steigerung |
| `heightenBonus` | zusätzlicher fester Schaden pro Schritt |
| `extraDice`, `extraType`, `extraHeightenDice` | zweite Schadenskomponente (z. B. Schall bei Thunderstrike) mit eigener Schadensart und Steigerung |

### Kollision
Kollision: NPCs und Spieler schieben sich nicht weg. Das gilt immer (nicht nur im Kampf), auch für liegende oder besiegte NPCs.

### Bekannte Grenzen
- Taste **C** kollidiert mit der Vanilla-Belegung „Hotbar-Aktivator speichern“; eine der beiden Tasten unter Steuerung umbelegen.
- Talente werden nur mit Namen geführt, ihre Effekte werden nicht berechnet.
- Auswahlbasierte Klassenverbesserungen (z. B. Doktrinen, Waffenmeisterschaft, Wissensränge) muss der Spielleiter setzen.
- Inventar gibt es noch nicht (Zauber: siehe unten).
- Kampf: keine Reaktionen (Reaktiver Schlag); der Schild-Block fehlt (der Schild wird gehoben, blockt aber noch nicht).
  Kein Gegenstandsmenü.
- Zustände: keine Zustände aus Zaubern oder Monsterfähigkeiten (der Spielleiter setzt sie von Hand); Zustände werden nicht
  über einen Serverneustart gespeichert (der Kampf endet dann ohnehin). Nur Verängstigt hat einen numerischen Statusmalus.
  Verlangsamt, Benommen, Sterbend 4 und kritischer Niederschlag sind nur per Unit-Test geprüft, nicht per Bot. Die Oberfläche (Zustandsliste, HUD) wurde noch nicht vom Nutzer angesehen.
- Es gibt keine Sichtlinienprüfung; Munition und Nachladen werden nicht erzwungen.
- Der Kampf endet bei einem Serverneustart; ein `/tp` eines Kämpfers wird zurückgesetzt (außer mit `/pf2 combat anchor`).
- Das Einfrieren wurde nur mit Test-Bots geprüft; auf echten Clients kann es ruckeln (Rubber-Banding).
- Die Bezeichnungen der Würfe im Chat sind englisch.
- Steigt der INT-Modifikator, muss der Spielleiter die zusätzliche trainierte Fertigkeit von Hand setzen.
- Die geführte Charaktererschaffung im Spiel fehlt noch; Charaktere kommen aus Pathbuilder.
- NPCs handeln nicht von selbst, der Spielleiter steuert sie mit dem Befehlsstab oder per Übernahme.
- NPC übernehmen (Phase 4, gebaut und per Bot auf dem Dedicated Server geprüft, **vom Nutzer noch nicht ingame getestet**): Kamera, Maussteuerung und Sprechblasen wurden noch nie auf einem echten Bildschirm gesehen.
  Die Kamera kann der Mausdrehung leicht hinterherhinken, weil die Drehung des NPCs über das Entity-Tracking ankommt. Ob A und D richtig herum laufen, ist ungeprüft.
  Das Mausrad und die Zahlentasten ändern weiterhin den Hotbar-Slot des Spielleiters. Für einen übernommenen NPC gibt es keine Reichweitenvorschau vor dem Schreiten (nur die verbleibenden Blöcke danach).
  Die Reichweitenanzeige zeigt einen flachen Kreis um die aktuelle Position, unabhängig vom Gelände. Ein Schreiten kann das Limit um bis zu einen Block überschreiten.
  Das Loslassen bei Verbindungsabbruch oder Verlust der GM-Rolle ist nicht getestet. Einen zweiten Befehlsbaum `/npc …` gibt es nicht, alles läuft unter `/pf2 npc …`.
- Zauber (Phase 5 Teil 1, gebaut, per Unit-Tests in `core` abgedeckt, Client und Dedicated Server starten; **vom Nutzer nicht ingame getestet, die Oberfläche wurde noch nicht auf einem echten Bildschirm gesehen**):
  - NPCs wirken keine Zauber.
  - Keine Flächenschablonen und keine Sichtlinienprüfung der Zauberwirkung; bei Flächenzaubern wählst du die Ziele von Hand.
  - Varianten mit 1 oder 3 Aktionen sind vereinfacht (Force Barrage, Heal, Harm, Horizon Thunder Sphere).
  - Zusatzwirkungen (anhaltender Schaden, Zustände wie Verängstigt durch Fear) werden nicht automatisch angewendet; der Spielleiter setzt sie von Hand.
  - Vorbereitete Zauberer führen Plätze pro Rang, nicht pro vorbereitetem Zauber.
  - Die Anzeige im Menü geht von mindestens 1 Aktion aus; die genauen Kosten prüft der Server.
  - Angeborene Zauber mit Rang über 0 werden nicht unterstützt.
  - Die Rangwahl spontaner Zauberer wird nicht gespeichert.
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
   - ✅ Teil 3: Kampfkern (Initiative, 3 Aktionen + Reaktion, Angriffe, Mehrfachangriffsmalus; im Einzelspieler getestet, Mehrspieler ungetestet)
   - ✅ Teil 4: Aktionsmenü (Taste V, Grundaktionen, Schild und Deckung; gebaut, Ingame-Test steht aus)
   - ✅ Teil 5: Zustände und Sterbend (gebaut, Ingame-Test steht aus)
4. ✅ NPC-Steuerung: Schreiten als gewählte Aktion mit Reichweitenanzeige, „Übernehmen“ eines NPCs durch den GM, Dialoge mit Sprechblasen (`/pf2 npc say`); gebaut, Ingame-Test steht aus
5. Zauber (✅ Teil 1: Zauberbuch, Reiter, Wirken mit automatischer Auflösung, Rast, eigene Zauber; gebaut, Ingame-Test steht aus), Gegenstände, mehr Inhalte, physische Würfel

## Lizenzhinweis

Pathfinder ist eine Marke von Paizo Inc. Die Spielmechanik von Pathfinder 2e (Remaster) steht unter
der ORC-Lizenz. Regeltexte von Archives of Nethys werden nicht kopiert.
Die Mod ist kostenlos und nicht kommerziell; Details und Pflichthinweise (Paizo Community Use Policy, ORC) in [NOTICE.md](NOTICE.md).
