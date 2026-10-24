# Phase 5 Teil 1 – Zauber (Design)

Der Nutzer wollte nach Phase 4 mit Phase 5 weitermachen und hat auf Rückfragen verzichtet („Fang an“). Alle
Festlegungen hier sind daher **meine Entscheidungen** (markiert, wo es eine echte Wahl war) und sollen vom
Nutzer beim Lesen geprüft werden. Teil 2 (Inventar/Gegenstände) und physische Würfel folgen später.

## Ziel

Zauberwirker können ihre Zauber aus dem Pathbuilder-Import im Spiel wirken: Zauberplätze und Fokuspunkte werden
gezählt, Zauberangriff und Zauber-SG berechnet, im Kampf kosten Zauber Aktionen, und häufige Kampfzauber werden
automatisch aufgelöst (Treffer/Rettungswurf, Schaden, Heilung) wie Waffenangriffe.

## Nicht-Ziele

- Keine vollständige Zauberdatenbank. Nur ein **Startkatalog** (~25 häufige Kampfzauber) mit Zahlen; alle
  anderen Zauber sind wirkbar, aber der GM löst sie selbst auf.
- Keine Flächen-Schablonen/Sichtlinie, keine Dauer-Effekte außer den bestehenden Zuständen, keine Reaktionszauber.
- Keine NPC-Zauber (Monster-Core-Daten enthalten keine Zauberlisten im Katalog) – Annahme, später möglich.
- Keine Zauberauswahl beim Stufenaufstieg im Spiel (neue Zauber kommen per Neuimport oder GM).
- Keine AoN-Regeltexte: nur Namen und Zahlen (ORC/Community Use Policy wie bisher).

## Verhalten

1. **Import:** Der Pathbuilder-Import liest `spellCasters` (Name, Tradition, vorbereitet/spontan/Fokus/angeboren,
   Attribut, Übungsrang, Plätze pro Rang, Zauber je Rang) und `focusPoints`. Gespeichert wird ein **Zauberbuch**
   neben dem Charakterbogen (eigenes Data Attachment, Annahme), inklusive verbrauchter Plätze.
2. **Werte:** Zauberangriff = Stufe + Übungsbonus + Attributmod; Zauber-SG = 10 + derselbe Wert. Zaubertricks und
   Fokuszauber wirken automatisch auf Rang ⌈Stufe/2⌉.
3. **Wirken:**
   - Zaubertrick: kostenlos (kein Platz). Fokuszauber: 1 Fokuspunkt. Sonst: 1 Platz des gewählten Rangs.
     Spontane Wirker dürfen einen Zauber auf höherem Rang wirken (höherer Platz); vorbereitete Wirker wirken
     ihn auf dem Rang, auf dem er vorbereitet ist (Annahme: Pathbuilder-Rang), Plätze werden pro Rang gezählt.
   - Im Kampf: nur im eigenen Zug, kostet die Aktionen des Zaubers (Katalog; unbekannte Zauber: 2), unterbricht
     Schreiten wie andere Aktionen, zählt **nicht** zum Mehrfachangriffsmalus (Regel).
   - Ziele: Einzelziel-Zauber → Ziel anklicken (wie wartende Aktionen in Teil 4, Abbrechen mit V).
     Mehrziel-/Flächenzauber → nacheinander Ziele anklicken (höchstens `maxTargets` des Katalogs, Annahme:
     Fläche = bis zu 10 Ziele), Bestätigen mit V. Selbst-Zauber ohne Klick.
4. **Auflösung (Katalogzauber):**
   - Zauberangriff gegen RK: Erfolgsgrad wie Waffenangriff; Kritisch = doppelter Schaden. Zustände/Mali aus Teil 5
     wirken (Verängstigt usw.).
   - Rettungswurf (Zähigkeit/Reflex/Wille) gegen Zauber-SG: der Server würfelt für jedes Ziel; bei „einfachem
     Rettungswurf“ krit. Erfolg 0, Erfolg halb, Fehlschlag voll, krit. Fehlschlag doppelt.
   - Heilung: Würfel + Bonus, nur auf Verbündete/sich selbst, hebt Sterbend auf wie normale Heilung.
   - Schaden geht durch Immunitäten/Schwächen/Resistenzen (`DamageResolver`) wie Waffenschaden.
   - Ergebnis im Chat wie bei Angriffen (offen; Werte der NPCs nur für GMs).
5. **Unbekannte Zauber:** werden gewirkt (Platz/Aktionen verbraucht), Chat „X wirkt <Zauber> (Rang n)“; der GM
   bekommt Zauberangriff und SG angezeigt und entscheidet selbst.
6. **Rast:** `/pf2 rest <Spieler|all>` (GM) füllt Plätze und Fokuspunkte auf. Keine automatische Zeitmessung.
7. **Bedienung:**
   - Charakterbogen (C) bekommt einen Reiter **„Zauber“**: Einträge, Zauber je Rang, freie Plätze, Fokuspunkte,
     Zauberangriff/SG, Knopf „Wirken“ (bei spontanen: Rangwahl).
   - Aktionsmenü (V), Kategorie **„Zauber“** wird aktiv und listet die wirkbaren Zauber mit Aktionskosten.
   - GM sieht im Bogen eines Spielers dessen Zauberbuch und kann verbrauchte Plätze zurücksetzen.
8. **Katalog als JSON** (`pf2craft/spells/spells.json` in `core`, erweiterbar über
   `config/pf2craft/spells/*.json` wie Monster/Gegenstände): id, Name, Rang, Aktionen, Reichweite (ft), Ziele,
   Art (attack/save/heal/utility), Rettungswurf + einfach?, Schadenswürfel und -art, Steigerung (pro Rang bzw.
   alle n Ränge zusätzliche Würfel). Zahlen werden für den Startkatalog von AoN (Remaster) abgelesen.

## Randbedingungen

- Regeln (Werte, Plätze, Steigerung, Auflösung) in `core` (neues Paket `de.pf2craft.core.spell`), Tests zuerst.
- Server autoritativ; Client-Code nur in `src/client`; neue Texte in beiden Sprachdateien; README/CLAUDE.md pflegen.
- Bestehende Charaktere ohne Zauberbuch funktionieren weiter (leeres Zauberbuch; Neuimport füllt es).

## Dem Bauenden überlassen

Speicherformat des Zauberbuchs, genaue GUI-Gestaltung, Auswahl der ~25 Startzauber (häufige Kampfzauber der
Remaster-Regeln, z. B. Elektrischer Bogen, Kraftgeschoss, Heilen, Schaden zufügen, Feuerball, Feuer atmen,
Entzünden, Ätzender Strahl, Nadelpfeile, Telekinetisches Geschoss, Benommenheit, Furcht).
