# Phase 2 – Charaktere: Design

Stand: 2026-10-17 · Status: Entwurf, wartet auf Freigabe durch den Nutzer

## Worum es geht und warum

Bisher kann man in PF2 Craft nur würfeln. Damit die Gruppe wirklich Pathfinder 2e in Minecraft
spielen kann, braucht jeder Spieler seinen **Charakter in der Mod**: Werte sehen, per Klick auf
Fertigkeiten und Rettungswürfe würfeln, aufsteigen. Der GM muss jederzeit eingreifen können.

Das Ziel des Nutzers: Wer neu auf den Server kommt, legt zuerst seinen Charakter an und spielt
danach komplett in Minecraft. Pathbuilder 2e ist dabei die Referenz, sowohl als Importquelle als auch
als Vorbild für das Spielgefühl.

Phase 2 ist der erste von zwei Schritten:

- **Phase 2 (dieses Dokument):** Import aus Pathbuilder, Charakterbogen, Speicherung, Würfeln per Klick,
  GM-Bearbeitung und **automatisches Hochstufen im Spiel**.
- **Später (Schritt B):** geführte Erschaffung im Spiel (Abstammung, Hintergrund, Klasse, Talente
  auswählen). Phase 2 soll so gebaut sein, dass Schritt B darauf aufsetzt.

## Ziele

1. **Erster Join ohne Charakter:** Der Spieler kommt in den **Zuschauermodus** (Spectator). Er kann zusehen,
   aber nicht mitspielen. **GMs sind ausgenommen.** Erst mit einem Charakter wird er spielbereit.
2. **Import aus Pathbuilder:** Der Spieler gibt die Export-ID aus Pathbuilder ein („Menu → Export JSON“,
   6 Ziffern). Der **Server** lädt `https://pathbuilder2e.com/json.php?id=<ID>`, prüft die Daten und speichert
   den Charakter.
3. **Ein neuer Import ersetzt** den bisherigen Charakter vollständig. **Der Spieler darf jederzeit selbst
   importieren**, ohne Freigabe (Entscheidung des Nutzers; die Gruppe vertraut sich hier). Geprüft wird nur,
   ob die Daten vollständig und lesbar sind, nicht, ob der Build regelkonform ist. Jeder Import wird im
   Server-Log festgehalten und allen GMs im Chat gemeldet („X hat Charakter Y, Stufe Z importiert“).
4. **Charakterbogen als Fenster mit Reitern** in schlichter Minecraft-Optik (Vanilla-Stil, keine eigene
   Pergament-Optik). Entschieden: Variante „B mit Reitern“.
   - Kopfzeile: Name, Abstammung, Klasse, Stufe, RK, TP aktuell/maximal.
   - **Übersicht:** sechs Attribut-Modifikatoren, Rettungswürfe, Wahrnehmung, Geschwindigkeit
     (Fuß und Blöcke, 1 Block = 5 ft), Angriffe mit Angriffsbonus und Schaden. Alles, was man im Kampf
     braucht, steht hier.
   - **Fertigkeiten:** alle Fertigkeiten und Wissensfertigkeiten (Lores) mit Übungsgrad und Bonus.
   - **Talente:** Liste der Talente und Klassenmerkmale (nur Namen).
5. **Klick zum Würfeln:** Ein Klick auf Fertigkeit, Rettungswurf, Wahrnehmung oder Angriff würfelt
   W20 + Bonus über den bestehenden `RollService` (serverseitig, der Bonus wird **auf dem Server** aus dem
   gespeicherten Charakter bestimmt, nicht vom Client übernommen).
6. **GM-Rechte:** Der GM kann den Bogen jedes Spielers öffnen und **Werte ändern**.
7. **Automatisches Hochstufen im Spiel**, ohne Pathbuilder. Die Mod rechnet die neue Stufe selbst aus,
   einschließlich klassenspezifischer Übungsgrad-Steigerungen. **Der GM gibt den Aufstieg frei**, und zwar im
   Charakterfenster des Spielers (dort, wo er auch die TP anpasst). Danach trifft der Spieler im eigenen Bogen
   seine Auswahl und schließt den Aufstieg ab. Ohne Freigabe kann der Spieler nicht aufsteigen.
8. **Klassen-Fortschritt als Daten** für **alle Klassen, die Pathbuilder anbietet**, also die Remaster-Klassen
   und die Klassen ohne Remaster-Fassung (z. B. Gunslinger, Inventor, Magus, Summoner); für diese gilt der
   neueste veröffentlichte Stand. Recherchiert aus Archives of Nethys, gegengeprüft mit den Daten des
   Foundry-PF2e-Systems. Der Nutzer wollte ausdrücklich „alle“. Das sind über 25 Dateien und damit
   der größte Aufwandsposten; sinnvoll ist, mit den Player-Core-Klassen zu beginnen. Eine Klasse ohne
   Datei kann trotzdem importiert werden, nur das automatische Hochstufen ist dann gesperrt
   (Meldung „für diese Klasse noch nicht verfügbar“).

## Nicht-Ziele (Phase 2)

- Geführte Erschaffung im Spiel (Schritt B).
- Talente mit Wirkung oder Voraussetzungsprüfung. Talente werden beim Aufstieg **per Namen** eingetragen;
  ihre Wirkung (z. B. „Toughness“ → mehr TP) setzt der GM bei Bedarf von Hand.
- Zauber, Inventar/Ausrüstung verwalten, Zustände, Begleiter/Vertraute, Kampfmodus.
- Doppelklasse (Dual Class), Varianten wie Free Archetype.
- Mehrere Charaktere pro Spieler.
- Übersetzung von Inhaltsnamen (Klassen, Talente, Waffen). Sie bleiben, wie Pathbuilder sie liefert (englisch);
  nur die Oberfläche ist zweisprachig.

## Anti-Ziele (das wäre ein Fehlschlag, auch wenn es technisch läuft)

- **Falsche Werte.** Ein importierter Charakter muss im Bogen dieselben Werte zeigen wie in Pathbuilder.
- Ein Spieler kann über manipulierte Pakete seine Werte ändern, ohne Freigabe aufsteigen oder mit
  falschem Bonus würfeln. (Der offizielle Weg „neu importieren“ ist bewusst erlaubt, siehe Ziel 3.)
- Der Server hängt, während Pathbuilder langsam antwortet.
- Regeltexte von AoN werden massenhaft übernommen (Lizenzregel aus CLAUDE.md).

## Rahmenbedingungen

- Architekturregeln aus `CLAUDE.md` gelten: Regellogik in `core` (ohne Minecraft, mit Tests),
  Server autoritativ, Client-Code nur in `src/client`, Payloads in `Pf2Networking`, Texte in beiden
  Sprachdateien, `translatableWithFallback`.
- Der HTTP-Abruf bei Pathbuilder läuft **asynchron** (nicht auf dem Server-Haupt-Thread), mit Timeout
  und Größenlimit. Das Ergebnis wird auf dem Haupt-Thread angewendet.
- Pathbuilder hat keine offizielle, dokumentierte API. Das Format ist aus echten Exporten bekannt
  (siehe Anhang). Die Mod muss mit fehlenden Feldern und unbekannten Klassen robust umgehen.

## Wie es funktioniert (vom Nutzer entschieden bzw. abgestimmt)

### Ablauf beim Join

1. Spieler verbindet sich. Hat er keinen Charakter und ist kein GM → Spielmodus Zuschauer, Hinweis
   im Chat, und auf dem Client öffnet sich das Fenster „Charakter importieren“.
2. Spieler gibt die Pathbuilder-ID ein → Server lädt, prüft, speichert.
3. Erfolg → Spieler wird in den normalen Spielmodus gesetzt, der Bogen öffnet sich.
4. Fehler (ungültige ID, Pathbuilder nicht erreichbar, unbekannte Klasse …) → verständliche Meldung,
   Spieler bleibt Zuschauer und kann es erneut versuchen.
5. Beim späteren Join mit vorhandenem Charakter passiert nichts Besonderes.

### Datenmodell (Kern der Sache)

Der Charakter wird **aus Bausteinen** gespeichert, nicht als fertige Endwerte, damit Hochstufen und
GM-Änderungen alles korrekt neu berechnen können:

- Stufe, Name, Abstammung, Erbe (Heritage), Hintergrund, Klasse, Schlüsselattribut, Größe
- Attributwerte (Pathbuilder liefert Werte wie 18; Modifikator = (Wert − 10) / 2, abgerundet)
- Übungsgrade für Wahrnehmung, Rettungswürfe, Klassen-SG, Rüstungs- und Waffenkategorien, jede Fertigkeit
  und jede Lore (Pathbuilder: 0/2/4/6/8 = untrainiert/trainiert/Experte/Meister/legendär)
- TP-Bausteine: Abstammungs-TP, Klassen-TP pro Stufe, Bonus-TP (flach), Bonus-TP pro Stufe
- Geschwindigkeit + Bonus
- RK-Bausteine: Gegenstandsbonus der Rüstung, Attributbonus-Grenze bzw. die Werte, die Pathbuilder liefert
- Angriffe (Name, Kategorie, Würfel, Schadensart, Angriffs- und Schadensbonus)
- Talente und Klassenmerkmale (Namen, mit Stufe)
- **Aktuelle TP** (änderbar im Spiel)

**Abgeleitete Werte** (Bonus einer Fertigkeit, maximale TP, Rettungswürfe …) berechnet `core`.
Formel: Attributmodifikator + Übungsbonus (untrainiert 0, sonst Stufe + 2/4/6/8) + eventuelle Boni.
Maximale TP = Abstammungs-TP + Bonus-TP + Stufe × (Klassen-TP + KON-Mod + Bonus-TP pro Stufe).

**Prüfstein:** Für jeden Beispielexport müssen die von `core` berechneten Werte mit den Werten in
Pathbuilder übereinstimmen (z. B. Fighter Stufe 5: RK 23, TP 75, Athletik +13, Reflex +9).

### Hochstufen im Spiel

Stufe +1 läuft geführt im Bogen ab. Automatisch:

- Alle Übungsboni steigen mit der Stufe; TP steigen um Klassen-TP + KON-Mod (+ Bonus pro Stufe).
- **Klassen-Fortschritt aus den Klassendaten:** Übungsgrad-Steigerungen und Klassenmerkmale dieser Stufe
  (z. B. Fighter Stufe 3: Zähigkeit wird Experte) werden angewendet.

Der Spieler wählt nur, was die Regeln ihm zu wählen geben:

- **Fertigkeitssteigerung** auf den Stufen, auf denen die Klasse eine bekommt (meist ab 3 jede zweite Stufe;
  manche Klassen mehr, z. B. Rogue). Die Auswahl hält die PF2-Grenzen ein (Meister erst ab Stufe 7,
  legendär ab Stufe 15).
- **Attributsteigerungen** auf Stufe 5, 10, 15, 20: vier verschiedene Attribute; ab Wert 18 zählt eine
  Steigerung nur halb (Remaster: „partial boost“).
- **Talente** auf den Stufen, auf denen es welche gibt (Klasse, Abstammung, Fertigkeit, Allgemein):
  freier Name.

Die Stufen für Fertigkeitssteigerungen, Talente und Klassenmerkmale kommen aus den **Klassendaten**, nicht
aus festem Code.

### Klassendaten

- Eine **JSON-Datei pro Klasse** mit dem Fortschritt Stufe 1–20 (Klassen-TP, Schlüsselattribut-Optionen,
  Anfangs-Übungsgrade, Steigerungen pro Stufe, Merkmale nur mit Namen, Stufen für Talente und
  Fertigkeitssteigerungen). Keine Regeltexte.
- Ausgeliefert in der Mod-Jar. Der Aufbau soll so sein, dass die Gruppe später selbst Klassen ergänzen kann
  (vgl. Entscheidung „Inhalte datengetrieben als JSON“).
- Ein **Test pro Klasse**, soweit Beispielexporte vorliegen: Stufe 1 aus dem Export → in `core` hochstufen →
  Ergebnis mit dem Export derselben Klasse auf höherer Stufe vergleichen, wo möglich.

### GM

- Kann den Bogen jedes Online-Spielers öffnen und dort Werte ändern (mindestens aktuelle TP,
  Übungsgrade, Attribute).
- **Aufstieg freigeben** per Knopf in diesem Fenster. Der Server merkt sich die offene Freigabe am Charakter
  (übersteht Neustarts). Der Spieler sieht im Bogen einen Hinweis „Aufstieg verfügbar“ und führt ihn aus.
  Die Stufe ändert der GM nicht direkt als Zahl, sondern nur über diese Freigabe; Fehler korrigiert er über die
  einzelnen Werte oder der Spieler importiert neu. Eine Stufe zurück gibt es in Phase 2 nicht.
- Alle Änderungen gehen als Anfrage an den Server, der die GM-Rolle prüft.

## Entscheidungen, die ich (Claude) getroffen habe – bitte prüfen

- Taste **C** öffnet den eigenen Bogen (änderbar wie R).
- **`/pf2 hp <Spieler> <+/-Zahl>`** für schnelle TP-Änderungen durch den GM, zusätzlich zum Bogen.
- Klick-Würfe im Bogen haben wie das Würfelfenster einen **„Verdeckt“-Schalter**.
- Nach dem Import wird der Spieler in den **Überlebensmodus** gesetzt.
- Speicherung über die **Fabric Data Attachment API** am Spieler (wird mit den Spielerdaten gespeichert,
  wie die GM-Rolle). Alternative für den Builder: `PersistentState`. Wichtig ist nur: serverseitig,
  übersteht Neustarts, Charakter auch ändern können, wenn der Spieler offline ist, ist **kein** Ziel.
- GM-Befehl **`/pf2 character reset <Spieler>`** löscht einen Charakter (Spieler wird wieder Zuschauer).
- Rüstungsklasse wird beim Hochstufen über Übungsgrad + Stufe neu berechnet, Gegenstands- und
  Attributanteil bleiben aus dem Import.
- **Bedeutung von „dieselben Werte wie Pathbuilder“:** Gegenstandsboni werden nicht modelliert, aber dort,
  wo Pathbuilder sie als Baustein liefert, übernommen: `acItemBonus` für die RK, sowie bei Angriffen der
  Gegenstandsanteil (= Pathbuilder-`attack` minus Attribut minus Übungsbonus), der beim Hochstufen
  gleich bleibt. Fertigkeiten und Rettungswürfe = Attribut + Übung (Pathbuilder liefert dort ohnehin keine
  Gesamtwerte). Die Paritätstests prüfen genau diese Werte.
- **Spielmodus:** Die Mod setzt nur Spieler **ohne Charakter** (und ohne GM-Rolle) auf Zuschauer und nach
  einem erfolgreichen **Erst**-Import auf Überleben. Spieler mit Charakter fasst sie beim Join nicht an, damit
  vom GM gesetzte Modi (z. B. Abenteuer) erhalten bleiben.

## Dem Builder überlassen

- Paketaufbau und Klassennamen, Payload-Zuschnitt (ein großer Sync oder mehrere kleine).
- Genaue Pixel-Anordnung im Bogen, solange die Aufteilung oben gilt und es bei GUI-Größe 3 auf 1080p lesbar ist.
- Welche JSON-Bibliothek (Gson ist in Minecraft enthalten).
- Reihenfolge der Umsetzung.

## Anhang: Pathbuilder-Exportformat (beobachtet)

`GET https://pathbuilder2e.com/json.php?id=<ID>` → `{ "success": true, "build": { … } }`. Relevante Felder in `build`:

| Feld | Inhalt |
|---|---|
| `name`, `class`, `level`, `ancestry`, `heritage`, `background`, `keyability`, `size` | Grunddaten |
| `abilities` | `str`, `dex`, `con`, `int`, `wis`, `cha` als Werte (z. B. 18), dazu `breakdown` |
| `attributes` | `ancestryhp`, `classhp`, `bonushp`, `bonushpPerLevel`, `speed`, `speedBonus` |
| `proficiencies` | Zahlen 0/2/4/6/8 für `perception`, `fortitude`, `reflex`, `will`, `classDC`, Rüstungs-/Waffenkategorien, alle Fertigkeiten |
| `lores` | Liste `[Name, Rang]` |
| `acTotal` | `acProfBonus`, `acAbilityBonus`, `acItemBonus`, `acTotal`, `shieldBonus` |
| `weapons` | `name`, `prof`, `die`, `damageType`, `attack`, `damageBonus`, `runes`, … |
| `feats` | Liste `[Name, Zusatz, Typ, Stufe]` |
| `specials` | Klassenmerkmale und Abstammungsfähigkeiten als Namen |
| `dualClass`, `spellCasters`, `pets`, `familiars`, `equipment`, `money` | in Phase 2 ignoriert |

Beispielexporte zum Testen gibt es im MIT-lizenzierten Projekt
[MrPrimate/pathmuncher](https://github.com/MrPrimate/pathmuncher) unter `tests/fixtures/pathbuilder/`
(Lizenzhinweis beim Übernehmen beilegen).
