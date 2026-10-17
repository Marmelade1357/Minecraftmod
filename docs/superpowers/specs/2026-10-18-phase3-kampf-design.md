# Phase 3 – Kampf: Design

Stand: 2026-10-18 · Status: Entwurf, wartet auf Freigabe durch den Nutzer (Builder-Check eingearbeitet)

## Worum es geht und warum

Mit Phase 2 haben alle Spieler ihren Charakter in der Mod. Jetzt soll die Gruppe **Kämpfe wie am
Pathfinder-Tisch** in Minecraft spielen: Initiative, feste Zugreihenfolge, 3 Aktionen, Angriffe gegen RK
mit Erfolgsgrad, Schaden mit Resistenzen, Zustände. Der GM steuert die Gegner.

Leitgedanke des Nutzers: **„Der Kampf soll wirklich wie Pathfinder sein.“** Wer nicht dran ist, kann nicht
handeln; die Regeln werden vom Spiel durchgesetzt und berechnet, nicht nur angezeigt.

Dafür braucht es drei Dinge, die es noch nicht gibt: **Gegner** (NPCs mit Werten), **Waffen als Items** und
den **Kampfablauf** selbst. Phase 3 enthält deshalb auch den NPC-Kern, der im alten Fahrplan in Phase 4 stand.

## Ziele

### Teil 1 – Items
1. Die Mod liefert eine **Item-Liste** (Waffen, Rüstungen, Schilde aus dem Player Core) als JSON mit:
   Name, Schadenswürfel, Schadenstyp, Merkmale (z. B. agil, Finesse, Reichweite, Wurf), Gruppe, Hände,
   Kategorie (einfach/Kriegs-/fortgeschritten), bei Rüstungen RK-Bonus, GE-Grenze, Proben-Malus;
   bei Schilden Härte, TP, Bruchschwelle, RK-Bonus. Die Gruppe kann die Liste wie Klassen erweitern.
2. Beim Pathbuilder-Import (und jedem Neuimport) bekommt der Spieler **seine Waffen und Schilde als Items**
   ins Inventar, inklusive Runen (Potenz, Striking, Eigenschaftsrunen wie Flaming).
3. **Aussehen:** Vanilla-Items, Material nach Schadenswürfel – W4 Holz, W6 Gold, W8 Diamant, W10/W12
   Netherite *(Zuordnung von Claude vorgeschlagen, Nutzer kann ändern)*. Form nach Waffenart (Schwert, Axt,
   Bogen, Armbrust, Dreizack für Speere/Stangenwaffen, Stock für Keulen/Unbewaffnet); Schilde = Vanilla-Schild.
4. **Tooltip** beim Darüberfahren: Name, Schaden („1W8 Hieb“), Merkmale, Runen, kurze **eigene** Beschreibung.
5. Vanilla-Waffen ohne PF2-Daten zählen im Kampf nicht.
6. Eigene Skins und einzigartige Item-Fähigkeiten: später.

### Teil 2 – NPCs
1. **Monster Core** (Remaster) als mitgelieferte JSON-Daten: Name, Stufe, Merkmale, Größe, Wahrnehmung,
   RK, Rettungswürfe, TP, Geschwindigkeit, Angriffe (Name, Bonus, Schaden je Typ, Merkmale),
   **Resistenzen, Schwächen, Immunitäten**, Fertigkeiten, Namen der Fähigkeiten. **Keine Beschreibungstexte.**
2. Quelle für die Umwandlung: die offenen Daten des Foundry-PF2e-Systems (Monster Core), abgeglichen mit
   https://2e.aonprd.com/Monsters.aspx.
3. NPCs erscheinen in der Welt als **Villager** mit Namensschild (Platzhalter-Modell; eigene Modelle später).
   Sie tun außerhalb von Befehlen nichts (keine eigene KI, kein Handel).
4. Der GM fügt NPCs hinzu (im Kampf-Fenster „+ NPC hinzufügen…“, Auswahl aus der Monsterliste mit Suche).
   Der NPC erscheint neben dem GM.
5. Der GM kann die **Werte jedes NPCs anpassen** – nur für diesen NPC oder als eigene Vorlage gespeichert
   (Vorlage = JSON im Server-Config-Ordner, wie bei Klassen).
6. Der GM steuert NPCs **von außen** mit einem **Befehlsstab** (Item): NPC anklicken, dann Ziel anklicken –
   ein Feld (der NPC läuft dorthin, innerhalb seiner Bewegung) oder einen Gegner (Angriffsfenster wie bei Spielern).
7. NPC mit 0 TP ist **besiegt**: fällt um, nimmt nicht mehr teil, bleibt liegen, bis der GM ihn entfernt.

### Teil 3 – Kampfkern
1. **GM-Werkzeuge sind Items**, keine Knöpfe oder Befehle (Grundregel des Nutzers; Aussehen der Items später).
   Das **Kampf-Item** öffnet das Kampf-Fenster; im laufenden Kampf bietet es: Zug überspringen, Kampf beenden,
   Teilnehmer hinzufügen/entfernen.
2. **Kampf-Fenster** (abgestimmt, Entwurf `combat-start.html`): links Spieler in der Nähe, rechts NPCs in der
   Nähe, jeweils mit Haken; in der Mitte **„Initiative würfeln“**; unter den NPCs „+ NPC hinzufügen…“.
   *(Claude: „in der Nähe“ = 32 Blöcke um den GM.)*
3. **Initiative:** Der Server würfelt für alle Angehakten (Wahrnehmung; Spieler aus dem Bogen, NPCs aus den
   Monsterdaten). Gleichstand: NPCs vor Spielern (Remaster-Regel), sonst nach Wahrnehmungsbonus.
4. **Initiative-Leiste** oben am Bildschirm für alle Teilnehmer und den GM: links und größer, wer dran ist,
   dahinter die Nächsten; rückt nach jedem Zug weiter. Gegner zeigen den Spielern nur **„unverletzt“,
   „verletzt“, „schwer verletzt“** *(Claude: verletzt = unter 100 %, schwer verletzt = unter 50 %)*, nie Zahlen.
   Der GM sieht echte TP.
5. **Einfrieren:** Wer nicht dran ist, kann sich nicht bewegen und nicht handeln (keine Angriffe, kein Abbau,
   kein Bauen). Ausnahmen (Reaktionen wie Reactive Strike, besondere Fähigkeiten) kommen später.
6. **Zug:** 3 Aktionen + 1 Reaktion. Anzeige unten: Aktionen ◆◆◇, Reaktion, verbrauchte Blöcke der laufenden Bewegung.
7. **Bewegung mit WASD:** Die Mod zählt die Blöcke. Beim ersten Schritt beginnt eine **Schreiten**-Aktion
   (kostet 1 Aktion). Nach Geschwindigkeit/5 Blöcken (Fighter 25 ft = 5 Blöcke) ist sie verbraucht; weiterlaufen
   beginnt automatisch die nächste Schreiten-Aktion, solange Aktionen übrig sind; danach geht es nicht weiter.
   *(Claude: horizontal gezählt, diagonal = 1 Block; Springen/Fallen zählt nicht extra.)*
8. **Angriff mit Waffe:** Waffen-Item in der Hand, Gegner anklicken → Fenster: 1., 2. oder 3. Angriff
   (vom Server **vorausgewählt** nach Anzahl der Angriffe in diesem Zug, umstellbar) → Bestätigen.
   Server würfelt W20 + Angriffsbonus − Mehrfachangriffsmalus (−5/−10, agil −4/−8) gegen die RK des Ziels.
   Reichweite: Nahkampf nur in Reichweite (1 Block, mit *Reichweite*-Merkmal 2 Blöcke), Fernkampf bis
   Grundreichweite × 6 mit −2 je Inkrement.
9. **Ergebnis sofort für alle sichtbar:** kritischer Fehlschlag / Fehlschlag / Treffer / kritischer Treffer,
   ohne GM-Bestätigung. Natürliche 20/1 verschieben um eine Stufe (`DegreeOfSuccess`). Die RK wird den Spielern
   nicht gezeigt.
10. **Schaden automatisch:** bei Treffer würfeln, bei kritischem Treffer verdoppeln, nach Typen getrennt
    (z. B. 1W8+4 Hieb + 1W6 Feuer). **Resistenz** verringert, **Schwäche** erhöht, **Immunität** hebt Schaden
    dieses Typs auf – jeweils pro Typ. Danach TP abziehen.
11. **0 TP:** NPC besiegt (siehe Teil 2). Spieler: bewusstlos, **sterbend 1** (+ Wert „verwundet“), bei kritischem
    Treffer sterbend 2; Erholungswurf am Zugbeginn (Remaster-Regel). Sterbend 4 = tot (GM kann zurücksetzen).
12. **Zug beenden:** Taste (z. B. Enter) *(Claude)*. Kein automatisches Ende, damit Spieler nicht überrascht werden.
13. Der Kampf endet über das Kampf-Item des GM; alle werden freigegeben, Zustände mit Kampfende-Dauer enden.

### Teil 4 – Aktionsmenü
1. Taste **V** öffnet im eigenen Zug das **Aktionsmenü** mit Kategorien: Grundaktionen, Klassenfähigkeiten,
   Zauber (später), Gegenstände (später). Kosten als ◆; nicht bezahlbare Aktionen ausgegraut.
2. Grundaktionen in Phase 3: **Schritt** (1 Block ohne Reaktionen), **Hinlegen/Aufstehen**, **Schild heben**
   (nur mit Schild-Item in der Hand), **Demoralisieren**, **Umstoßen** (Trip), **Packen** (Grapple),
   **Befreien** (Escape), **Suchen**, **Deckung nehmen** (Take Cover). *(Claude: Verzögern/Delay ist kein Ziel.)*
   Fertigkeitsaktionen würfeln gegen den passenden SG des Ziels (z. B. Umstoßen: Athletik gegen Reflex-SG).
3. Braucht eine Aktion ein Ziel: Aktion wählen, dann Ziel anklicken.
4. **Favoriten** (Stern) stehen oben. *(Optionale Kampf-Leiste mit Favoriten: späterer Ausbau.)*
5. Schild heben funktioniert auch direkt über das Schild-Item in der Hand.

### Teil 5 – Zustände
1. Zustände in Phase 3: bewusstlos, sterbend 1–4, verwundet, liegend, festgehalten (grabbed),
   gefesselt (restrained), auf dem falschen Fuß (off-guard), verängstigt X, kränkelnd X, betäubt X,
   verlangsamt X, verborgen, unbemerkt, unsichtbar, geblendet.
2. **Die Mod schlägt Zustände vor, der GM entscheidet:** Entsteht ein Zustand aus einer Aktion (z. B. Umstoßen
   gelingt → liegend), bekommt der GM eine Rückfrage „Kobold 1 → liegend? ✔/✘“. Erst danach gilt er.
   Ausnahme: sterbend/bewusstlos bei 0 TP gelten sofort *(Claude: sonst hängt der Kampf an einer Rückfrage)*.
3. Aktive Zustände wirken automatisch: Boni/Mali auf Proben, RK, SG; Einschränkungen von Aktionen
   (z. B. liegend: nur kriechen; betäubt/verlangsamt: weniger Aktionen); Abklingen (verängstigt −1 am Zugende).
4. Der GM kann jeden Zustand jederzeit per Hand setzen/entfernen (über sein Item / den Bogen).
5. Zustände sind im Bogen und in der Initiative-Leiste als kleine Symbole sichtbar.

### TP und Minecraft-Gesundheit (vom Nutzer entschieden)
1. **Vanilla-Schaden ist grundsätzlich aus** – im und außerhalb des Kampfs (Mobs, Fallschaden, Lava, Ertrinken …).
   Schaden entsteht nur über die Pathfinder-Regeln. *(Claude: Ausnahme `/kill` und Leere/Void für Admins.)*
2. **Kein Vanilla-Tod und kein Respawn**: 0 TP führt zu bewusstlos/sterbend (Teil 3, Ziel 11), der Spieler liegt
   eingefroren am Boden; was danach passiert, entscheidet der GM.
3. **Die Herzen werden ausgeblendet** und durch eine Zahl ersetzt: **„TP: 70/85“** (aktuelle/maximale TP) an der Stelle
   der Herzen. *(Claude: Hunger ist ebenfalls aus und die Hungerleiste ausgeblendet, da Hunger sonst Schaden macht.)*
4. Für Spieler ohne Charakter (Zuschauer) und den GM gilt dasselbe; der GM sieht statt TP nichts *(Claude)*.

## Nicht-Ziele (Phase 3)

- Zauber, Zauberplätze, Fokuspunkte (Phase 5), Tränke/Schriftrollen benutzen.
- Reaktionen (Reactive Strike, Shield Block) und Aktivitäten außerhalb des eigenen Zugs.
- Eigene NPC-Modelle, Skins, einzigartige Item-Fähigkeiten, Crafting, Handel.
- „Übernehmen“ eines NPC (Kamera/Steuerung in den NPC) und `/npc say`.
- Talent-Wirkungen (Talente bleiben Namen), Flankieren mit Positionsprüfung *(Claude: kommt als Ausbau; der GM
  kann „auf dem falschen Fuß“ per Hand setzen)*, Deckung, Gelände, Höhenunterschiede.
- Legacy-Bestiaries (OGL) und Abenteuer-spezifische Monster.

## Anti-Ziele

- **Falsche Würfe oder Werte**: Angriffsbonus, MAP, RK-Vergleich, Schaden und Resistenzen müssen exakt den
  Regeln entsprechen.
- Ein Spieler kann außerhalb seines Zugs handeln oder sich bewegen, oder mehr als 3 Aktionen nutzen.
- Spieler sehen die TP-Zahlen oder RK der Gegner.
- Ein Kampf bleibt hängen (z. B. weil ein Spieler den Server verlässt oder der GM eine Rückfrage nicht beantwortet).
- Massenhaft kopierte Beschreibungstexte.

## Rahmenbedingungen

- Architekturregeln aus `CLAUDE.md`: Regellogik (Initiative, MAP, Erfolgsgrad, Schaden, Resistenzen, Zustände,
  Bewegungsbudget) in `core` mit Tests; Server autoritativ; Client nur Anzeige und Anfragen; beide Sprachdateien.
- Bestehende Bausteine nutzen: `RollService` (Würfe und Anzeige), `DegreeOfSuccess`, `CharacterSheet`/`CharacterMath`,
  `Strike` (inkl. `damageDice`), GM-Rolle.
- **Lizenz:** Mechanik aus Remaster-Büchern unter ORC; Golarion-Eigennamen über die **Paizo Community Use Policy**
  (Mod kostenlos, nicht kommerziell, vorgeschriebener Hinweistext, keine Paizo-Logos/Bilder). Vor dem Einbau den
  aktuellen Wortlaut auf paizo.com/licenses/communityuse prüfen und beide Hinweise (ORC, CUP) in README und
  `fabric.mod.json`-Beschreibung aufnehmen. Beschreibungen nur als kurze eigene Zusammenfassungen.
- Ein Kampf zur Zeit pro Server *(Claude)*.
- Spieler, die während des Kampfs den Server verlassen: ihr Zug wird übersprungen, sie bleiben in der Leiste
  (ausgegraut) und sind beim Wiederkommen wieder dabei *(Claude)*.

## Vom Nutzer entschieden

- Kampf wie am Pathfinder-Tisch; Initiative-Reihenfolge; wer nicht dran ist, ist eingefroren.
- GM steuert NPCs von außen (anklicken → Ziel anklicken), nicht durch Übernehmen.
- Bewegung mit WASD, Mod zählt Blöcke, Schreiten nach Geschwindigkeit.
- Werte aus Pathfinder-2e-Monsterdaten, vom GM anpassbar; Monster mitgeliefert (Monster Core reicht für den Anfang);
  Golarion-Namen dürfen verwendet werden (über die Community Use Policy).
- NPC-Modell vorerst Villager.
- Angriffsergebnis sofort ohne GM-Bestätigung; Resistenzen werden eingerechnet.
- Spieler sehen bei Gegnern nur „verletzt/schwer verletzt“, keine TP.
- GM-Werkzeuge über Items; Kampf-Fenster mit Spielern/NPCs zum Anhaken, „Initiative würfeln“ in der Mitte,
  „NPC hinzufügen“; Initiative-Leiste oben, aktueller links und größer.
- Angriff: Waffen-Item in der Hand, Gegner anklicken, Fenster 1./2./3. Angriff. Schild heben mit Schild in der Hand.
- Aktionsmenü mit Kategorien und Favoriten (Vorschlag angenommen).
- Item-Liste mitliefern; Aussehen der Waffe nach Schaden (Holz/Gold/Diamant/Netherite); Unterscheidung über Name,
  Schaden und Tooltip-Beschreibung.
- Zustände berechnet die Mod automatisch, der GM bestätigt, ob sie aktiv werden; Auswahl der Zustände abgesegnet.
- Reihenfolge der Umsetzung: Items → NPCs → Kampfkern → Aktionsmenü → Zustände; nach jedem Teil testet der Nutzer.

## Entscheidungen von Claude nach dem Builder-Check (bitte prüfen)

- **NPC-Züge** laufen über dieselbe Zug-Logik wie Spieler: 3 Aktionen, 1 Reaktion, Mehrfachangriffsmalus,
  Blockzählung beim Laufen. Der GM ist kein Kampfteilnehmer und daher **nie eingefroren**. Er beendet den Zug
  eines NPC mit derselben Taste wie Spieler (Enter) oder über das Kampf-Item. Ist ein NPC ausgewählt, öffnet V das
  Aktionsmenü **für diesen NPC**, damit auch Fertigkeitsaktionen gehen.
- **Zustände mit Regelwirkung, ohne Positionsprüfung:**
  - Verborgen/unbemerkt/unsichtbar/geblendet wirken auf Angriffe (glatter Wurf SG 11 bei verborgenem Ziel, Ziel
    aussuchen + SG 11 bei unbemerktem; geblendet: alles gilt als verborgen).
  - Ob jemand verborgen ist, entscheidet der GM per Hand.
  - Deckung nehmen gibt +2 Situationsbonus auf RK bis zur nächsten Bewegung, ohne Prüfung der Umgebung.
  - Packen merkt sich, wer wen festhält, damit Befreien und das Ende des Zustands funktionieren.
- **Daten, die nicht in die Liste passen:**
  - Pathbuilder-Waffen ohne Eintrag in der Item-Liste werden trotzdem als Item erzeugt, mit Name, Würfel, Typ
    und Merkmalen aus dem Export, Aussehen nach Schadenswürfel.
  - Nachladen und Munition werden in Phase 3 **nicht** erzwungen.
  - Zweihand-Würfel werden nicht gerechnet.
  - Monsterfähigkeiten mit Zusatzeffekt (Grab, Knockdown, anhaltender Schaden) erscheinen nur als Name und
    Hinweis „GM entscheidet“; automatisch ist nur der normale Schaden.
- **Waffen-Items:**
  - Der **Charakterbogen ist die Wahrheit**. Ein Item verweist auf den Angriff im Bogen und trägt eine Kopie der
    Werte nur für den Tooltip; der Server rechnet immer aus dem Bogen.
  - Neuimport entfernt die alten Mod-Waffen aus dem Inventar und gibt die neuen.
  - Ein fallengelassenes oder fremdes Mod-Item funktioniert nur beim Besitzer.
- **NPC-Namen:** Das Namensschild zeigt den Monsternamen, der GM kann den NPC umbenennen (z. B. „Kobold 1“).
  Alle NPCs stehen in der Leiste. Verbündete NPCs zeigen den Spielern ebenfalls nur verletzt/schwer verletzt.
- **Umsetzung in fünf Plänen**, einer pro Teil. Nach jedem Teil testet der Nutzer, dann folgt der nächste Plan.

## Dem Builder überlassen

- Paket- und Klassenstruktur, Payload-Zuschnitt, Datenformate (JSON-Aufbau) im Stil der Klassendaten.
- Umwandlungsskript Foundry → JSON (wird nicht mit ausgeliefert, nur die erzeugten Daten).
- Wie das Einfrieren technisch umgesetzt wird (Bewegung serverseitig zurücksetzen, Eingaben blockieren …),
  solange es serverseitig durchgesetzt ist.
- Pixelgenaues Layout von Fenstern, Leiste und Tooltips (lesbar bei GUI-Größe 3 auf 1080p).
