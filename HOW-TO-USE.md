# PF2 Craft – How to install & how to use

Kurzanleitung zum Installieren, Starten und Bedienen der Mod (Minecraft 1.21.1, Fabric, Java 21).

---

## 1. Installation – die Mod selbst bauen

Im Projektordner (`Minecraft mod`) in der **Windows PowerShell** (Java 21 muss installiert sein):

```powershell
cd "C:\Users\ouali\Desktop\Minecraft mod"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\gradlew.bat build
```

Fertig ist die Mod hier:

```
fabric\build\libs\pf2craft-0.1.0.jar
```

(Die Datei mit `-sources` im Namen brauchst du nicht.)

## 2. Starten – zwei Möglichkeiten

### A) Zum schnellen Testen direkt aus dem Projekt (ohne Launcher)

```powershell
cd "C:\Users\ouali\Desktop\Minecraft mod"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\gradlew.bat :fabric:runClient
```

Testserver (beim ersten Start `eula=true` in `fabric\run\eula.txt` setzen):

```powershell
.\gradlew.bat :fabric:runServer
```

### B) Mit dem normalen Minecraft-Launcher (so spielt die Gruppe)

1. **Fabric installieren:** Fabric-Installer von fabricmc.net laden, Minecraft **1.21.1**, Loader **0.16.14** oder neuer, für den Launcher installieren.
2. **Fabric API laden:** Version **0.116.6+1.21.1** (Modrinth oder CurseForge). Ohne sie startet die Mod nicht.
3. **Dateien kopieren** in den Ordner `%appdata%\.minecraft\mods` (oder den `mods`-Ordner deiner Launcher-Instanz):
   - `pf2craft-0.1.0.jar`
   - die Fabric-API-Datei
4. Im Launcher das Profil **fabric-loader-1.21.1** wählen und starten.

**Server:** Fabric-Server installieren, dieselben zwei JARs in den `mods`-Ordner des Servers legen, `eula=true` setzen. **Alle Spieler** brauchen dieselben beiden JARs.

**GM festlegen** (Konsole oder als OP im Spiel): `/pf2 gm add <Spieler>`

---

## 3. Tastenbelegung

Alle Tasten lassen sich unter *Optionen → Steuerung → PF2 Craft* ändern.

| Taste | Funktion | Wer |
|---|---|---|
| **R** | Würfelfenster öffnen | alle |
| **C** | Charakterbogen öffnen (Import, Würfe, Aufstieg) | alle |
| **V** | Aktionsmenü öffnen (bzw. wartende Aktion abbrechen) | im Kampf |
| **G** | Schreiten (Strecke starten) | im Kampf |
| **H** | Reichweitenanzeige umschalten | im Kampf |
| **Enter** | Zug beenden | im Kampf |
| **J** | NPC übernehmen / loslassen | GM |
| **F5** | Ansicht wechseln (z. B. dritte Person beim Übernehmen) | alle (Minecraft) |

Mausbedienung im Kampf:
- **Linksklick** mit der Waffe: Angriff (Fenster mit 1./2./3. Angriff öffnet sich)
- **Rechtsklick** mit dem eigenen Schild: Schild heben
- **Befehlsstab** (GM): Linksklick wählt einen NPC, Rechtsklick greift an, Klick auf den Boden lässt ihn laufen
- **Kampfglocke** (GM): Kampf starten und Teilnehmer wählen

## 4. Ablauf eines Zuges (Beispiel)

1. **Initiative:** Der GM startet den Kampf mit der Kampfglocke, die Initiative wird gewürfelt.
2. Du bist dran: oben steht **„Du bist dran“**, unten rechts siehst du Aktionen und Zustände.
3. **G drücken** (Schreiten): kostet 1 Aktion. Dann kannst du bis zu 5 Blöcke laufen (bei Speed 25). Der Ring am Boden zeigt die Reichweite (**H** schaltet ihn an/aus).
4. **Angreifen:** Waffe in die Hand, Gegner anklicken. Im Fenster wählst du den Angriff.
5. **V** für Sonderaktionen (Umstoßen, Einschüchtern, Aufstehen …). Bei Zielaktionen danach das Ziel anklicken.
6. Sind die drei Aktionen verbraucht, endet der Zug automatisch (oder **Enter**).

## 5. Beispiel: so sieht der Bildschirm im Kampf aus

```
+--------------------------------------------------------------+
|        [ Du 18 ]  [ Ogre ??? ]  [ Kobold ??? ]               |  <- Initiative-Leiste
|                       DU BIST DRAN                           |
|                                                              |
|                                                              |
|                                                              |
|   Chat (im Kampf kleiner)                                    |
|   Ogre greift an ...                  Aktion: Umstoßen -     |
|   Kobold: Schaden 6                    Ziel anklicken (V)    |  <- Hinweis, rechts
|                                        Aktionen: ◆ ◆ ◇       |  <- Aktionsleiste, rechts
|                                        TP: 42/50 · Verängstigt 1 |
|                          [ Hotbar ]                          |
+--------------------------------------------------------------+
```

Hinweise:
- Aktionsleiste, Hinweis und TP-Text stehen im Kampf **rechts unten**. Wo genau, stellst du in `config/pf2craft-client.json` ein (`hudAnchor`: `right`, `center`, `left`).
- Der Chat ist im Kampf kleiner (`combatChatScale`, `combatChatHeight` in derselben Datei).
- Fremde Gegner zeigen nur „verletzt / schwer verletzt“, keine genauen TP.

## 6. Wichtige Befehle

| Befehl | Wirkung |
|---|---|
| `/roll 1d20+5` bzw. `/roll 1W20+5` | würfeln (öffentlich) |
| `/gmroll …` | verdeckt würfeln (nur GM sieht es) |
| `/pf2 gm add <Spieler>` | Spielleiter-Rolle vergeben (OP-Level 2) |
| `/pf2 combat action <id>` | Aktion per Befehl (z. B. `stride`, `trip`, `cancel`) |
| `/pf2 combat condition <player\|npc> <id> <Wert>` | Zustand setzen (GM) |
| `/pf2 npc say <Text>` | den gewählten NPC sprechen lassen (Sprechblase + Chat) |
| `/pf2 npc control` / `release` | NPC übernehmen / loslassen (GM, Ersatz für J) |

Mehr Details stehen in der `README.md`.

---

*Der Entwicklungsstand und bekannte Grenzen stehen in `CLAUDE.md` und `README.md`. Kamera, Sprechblasen und Reichweitenanzeige sind noch nicht ingame geprüft.*
