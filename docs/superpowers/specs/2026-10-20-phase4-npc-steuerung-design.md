# Phase 4 – NPC-Steuerung, Schreiten als Aktion, Sprechblasen (Design)

Aufbauend auf Phase 3 (Teil 3–5: Kampfkern, Aktionsmenü, Zustände). Entscheidungen stammen aus dem
Gespräch mit dem Nutzer; „(Annahme)“ markiert meine Festlegungen. Drei Teile, in dieser Reihenfolge baubar.

## Ziel

Der Kampf soll sich direkter anfühlen: Der GM steuert NPCs wie Spieler statt per Stab-Klicks, Bewegung wird
bewusst gewählt statt versehentlich bezahlt, und NPCs können mit Sprechblasen sprechen.

## Nicht-Ziele

- Keine KI/Automatik für NPCs (sie handeln nur auf Befehl des GM).
- Keine neue Kampfregel; Reichweite, Aktionen, Zustände bleiben wie in Phase 3.
- Kein taktischer Kartenmodus (bleibt optional später).

## Teil A – Schreiten wird eine bewusst gewählte Aktion

Problem heute: Wer sich über den eigenen Block hinaus bewegt, startet automatisch eine Schreiten-Aktion; ein
Fehler oder zu weites Laufen kostet ungewollt eine zweite Aktion.

1. Im Kampf darf sich ein Kämpfer nur über den eigenen Block hinaus bewegen, nachdem er **„Schreiten“**
   gewählt hat (Aktionsmenü Taste V oder eigene Schnelltaste, Annahme). Die Aktion kostet sofort 1 Aktion.
2. Mit gewähltem Schreiten darf er bis zu `Speed/5` Blöcke laufen (Speed 25 → 5 Blöcke). An der Grenze
   wird er festgehalten; es startet **keine** zweite Strecke automatisch.
3. Eine weitere Strecke braucht erneut „Schreiten“ (neue Aktion). Mehrere Strecken pro Zug sind möglich,
   solange Aktionen übrig sind.
4. Der Schritt-um-einen-Block („Step“, Teil 4) und alle Zustandsregeln (Verlangsamt, Liegend …) bleiben
   unverändert gültig.
5. **Reichweitenanzeige:** Ein auf dem Boden sichtbarer Rahmen (Partikel oder Umrandung, Umsetzung dem
   Bauenden überlassen) zeigt, wie weit man mit einer Strecke käme. Sie ist **umschaltbar** (Taste, Annahme:
   eigene Taste, Standard aus/an in `config/pf2craft-client.json`) und zeigt auch **ohne** gewähltes
   Schreiten die mögliche Strecke als Vorschau. Nach Wahl von Schreiten zeigt sie den Restweg.
6. Gilt auch für NPCs unter GM-Kontrolle (Teil B) und beim Befehlsstab-Laufen (der Befehl „hinlaufen“ wählt
   Schreiten implizit, damit der Stab weiter funktioniert).

## Teil B – NPC übernehmen

1. Eine **Übernehmen-Taste** (Annahme: eigene Taste, in den Minecraft-Steuerungen belegbar) lässt den GM
   einen NPC steuern wie einen Spieler: WASD, Linksklick (Angriff), V (Aktionsmenü), Schreiten wie Teil A.
2. Ansicht: **Ich-Perspektive aus dem NPC** (Kamera-Entity wie Zuschauermodus), F5 wie gewohnt. Der Körper
   des GM bleibt stehen und ist geschützt (kein Schaden, keine Bewegung).
3. Wann: im Kampf nur der NPC, dessen Zug läuft; außerhalb des Kampfes jeder ausgewählte NPC.
4. Ende: dieselbe Taste gibt ab; das Ende des NPC-Zuges beendet die Übernahme automatisch.
5. Server autoritativ: Bewegungs- und Angriffsgrenzen wie bei Spielern, Eingaben des GM werden dem NPC
   zugeordnet und serverseitig geprüft. Der Befehlsstab bleibt als Alternative erhalten.
6. Während der Übernahme sind die Aktionen/Bewegungsanzeige des NPC im HUD sichtbar (wie für Spieler).

## Teil C – Dialoge und Sprechblasen

1. Während der Übernahme erscheint jeder Chat-Text des GM als Rede des NPC („Ogre Warrior: …“) statt unter
   seinem Namen.
2. Ohne Übernahme: `/npc say <Text>` lässt den ausgewählten NPC sprechen (GM, Berechtigung wie andere GM-Befehle).
3. Zusätzlich erscheint die Rede als **Sprechblase über dem Kopf des NPC**, für alle Spieler in der Nähe sichtbar
   (Annahme: ca. 5 Sekunden, Länge begrenzt auf ~120 Zeichen, Umbruch), clientseitig gerendert aus einem
   neuen Payload.
4. Neue Texte in `en_us.json` und `de_de.json`; README und CLAUDE.md aktualisieren.

## Randbedingungen

- Regeln (Strecke, Schreiten-Zustand) in `core.combat` (`MovementBudget` erweitern) mit JUnit-Tests zuerst.
- Server autoritativ; Client-Code nur in `src/client`; Payloads in `Pf2Networking.registerPayloads()`.
- Kampfzustand bleibt nur im Speicher.

## Dem Bauenden überlassen

Darstellung der Reichweitenanzeige, Technik der Kamera-Übernahme (Camera Entity), Tastenbelegung,
Aufteilung in Tasks, Aussehen der Sprechblase.
