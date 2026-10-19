# Phase 3 Teil 5 – Zustände (Design)

Aufbauend auf `2026-10-18-phase3-kampf-design.md` und Teil 4 (Aktionsmenü). Entscheidungen stammen aus dem
Gespräch mit dem Nutzer; Punkte mit „(Annahme)“ sind meine Festlegungen.

## Ziel

Zustände (Conditions) aus Pathfinder 2e werden im Kampf **automatisch** berechnet, angezeigt und beendet.
Der GM kann alles per Klick überstimmen. Sterbend/Bewusstlos läuft automatisch, der Tod wird nur gemeldet.

## Nicht-Ziele

- Keine Zauber- oder Monsterfähigkeits-Effekte (kommen mit Phase 5); dafür setzt der GM Zustände von Hand.
- Keine Reaktionen, keine Sichtlinie, keine Persistenz über Serverneustart (Kampfzustand bleibt im Speicher).
- Keine Regeltexte von AoN kopieren: nur Namen und Zahlen, eigene Kurzbeschreibungen.

## Zustände (erste Ausbaustufe)

verängstigt (Wert), hilflos/off-guard, liegend, gepackt, verlangsamt (Wert), benommen (Wert), bewusstlos,
sterbend (Wert), verwundet (Wert). Weitere sind später leicht ergänzbar (datengetrieben im Regelkern).

## Verhalten

1. **Sofort wirksam:** Zustände aus Aktionen und Treffern gelten ohne Bestätigung (Umstoßen → liegend,
   Demoralisieren → verängstigt, Packen → gepackt). Der GM kann jeden Zustand jederzeit setzen, ändern
   und entfernen – bei Spielern und NPCs (Auswahl im Charakterbogen bzw. NPC-Fenster, zusätzlich Befehl).
2. **Automatische Wirkung (Regelkern, getestet):** Verängstigt/verlangsamt/benommen/Off-guard/liegend
   ändern Probenboni, RK und verfügbare Aktionen nach den Regeln (Status- bzw. Umstandsmali; Höchstwert
   je Typ zählt). Verängstigt sinkt am Ende des eigenen Zuges um 1.
3. **Freiwillig beendet:** „Aufstehen“ (1 Aktion) und „Befreien“ kommen ins Aktionsmenü; der Spieler
   entscheidet selbst, ob er aufsteht. Gepackt endet, wenn der Packer sich entfernt oder der Betroffene sich befreit.
4. **Sterbend (Spieler):** 0 TP → bewusstlos + sterbend 1 (kritischer Treffer: 2; mit verwundet +Wert).
   Im eigenen Zug würfelt der **Server automatisch** den Wiederherstellungswurf (einfacher Check SG 10 +
   Sterbend-Wert); **das Ergebnis sehen alle** im Chat. Heilung entfernt sterbend und gibt verwundet +1.
   Bei sterbend 4 meldet das Spiel „X wäre jetzt tot“ – **der GM entscheidet**, nichts wird gelöscht
   oder automatisch beendet.
5. **NPCs bei 0 TP:** gelten sofort als besiegt (am Boden, aus der Zugreihenfolge), keine Sterbend-Regeln;
   der GM kann sie per Klick wieder aufrichten.
6. **Anzeige:**
   - Eigener HP-Text bekommt die eigenen Zustände angehängt („HP: 42 · Verängstigt 1, Liegend“).
   - **Für alle sichtbar:** liegend, bewusstlos, sterbend (und besiegt). Alle übrigen sieht nur der
     Betroffene und der GM.
   - NPC-Zustände (nicht sichtbare) sieht nur der GM. Chat-Meldung beim Entstehen und Enden.
   - Zustände stehen zusätzlich ausführlich im Charakterbogen/NPC-Fenster.
7. Vanilla-Schaden/Tod bleiben aus (wie Teil 3); „bewusstlos“ wird über Enforcement erzwungen (keine Aktionen).

## Randbedingungen

- Regeln in `core.combat` (z. B. `Condition`, `ConditionSet`, `ConditionEffects`, `DyingRules`), JUnit-Tests zuerst.
- Server autoritativ; Clients erhalten nur gefilterte Sync-Daten (Sichtbarkeit pro Empfänger, wie HP-Text).
- Neue Texte in `en_us.json` und `de_de.json`; README und CLAUDE.md aktualisieren.
- Teil 4 `TurnState`, `CombatEffects`, `AttackService` und `ActionService` werden erweitert, nicht ersetzt.

## Dem Bauenden überlassen

Konkrete Datenstruktur, Netzwerkformat, Aufteilung in Tasks, Layout des Zustands-Auswahlmenüs für den GM.
