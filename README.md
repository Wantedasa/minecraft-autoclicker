# Auto Clicker (Fabric, MC 1.21.11)

Client-Mod: ein Auto-Clicker, der **weiterklickt, wenn Minecraft nicht im Vordergrund ist**.
Also: F6 an → in ein anderes Fenster tabben → er klickt weiter.

## Bedienung
- **F6** – Auto-Clicker an/aus (Chat sagt ON/OFF).
- **F7** – Einstellungs-Menue oeffnen (oder schliessen).
- Tasten sind unter `Steuerung` frei belegbar (Kategorie "Auto Clicker").

Im Spiel gibt es **kein** Overlay - den Status siehst du an der Chat-Zeile beim Umschalten.

## Einstellungen (F7)
- **Click interval** – hours / mins / secs / milliseconds (wie bei jedem Auto-Clicker).
  0 ms = so schnell wie ein Client-Tick erlaubt (max. 20 Klicks/Tick, also 1000/s).
- **Random offset + -** – jeder Klick kommt `interval + 0..N ms` spaeter (menschlicher).
- **Mouse button** – Left (Attacke / abbauen) oder Right (benutzen / essen / angeln).
- **Click type** – Single / Double / Triple, oder **Hold** (Button bleibt gedrueckt:
  Attacke jede Tick, bzw. Rechtsklick-Halten fuer Essen/Angeln/Bogen).
- **Repeat** – bis Stop oder N mal.
- **Keep running when unfocused** – schaltet fuer die Session "Pause on lost focus" aus.

Alles liegt in `%APPDATA%/.minecraft/config/autoclicker.json` und ist auch per Hand editierbar
(Mod liest sie beim Start; Datei wird automatisch angelegt).

## Wie es funktioniert (und warum es auch unfokussiert klickt)
Der Mod bewegt keinen Cursor, sondern schiebt echte Tasten-Presses in die Vanilla-Bindings
Attack/Use (`KeyBinding.setKeyPressed` + `KeyBinding.onKeyPressed`) - genau das, was der echte
Mausklick auch macht. Der Client schickt die Interaktions-Pakete also selbst, wie beim normalen
Klicken (kein Paket-Spoofing, kein Desync).

Warum das beim Tabben weiterlaeuft: Minecraft tickt und verarbeitet Input auch ohne Fensterfokus
- eine echte Maus schickt ihre Events aber an das Fenster, das gerade **im** Fokus ist.
Das Einzige, was den Vanilla-Client stoppt, ist der Fokusverlust-Pause: 500 ms nach Fokusverlust
oeffnet er das Spielmenue (und dann wird kein Input mehr verarbeitet). Der Mod setzt deshalb
beim Einschalten `pauseOnLostFocus` aus und beim Ausschalten wieder zurueck.

Wichtig: nur im **Inventar-/GUI-freien** Zustand wird geklickt (wie im Vanilla-Client auch),
und **nicht** minimiert-getestet - minimieren kann die Tickrate druecken, also lieber nur tabben.

## Einschraenkungen
- Klickt im Spiel, nicht in anderen Programmen (der Cursor wird nicht bewegt).
- Block-Abbauen per "gedrueckt halten" funktioniert nur bei fokussiertem Fenster
  (Vanilla verlangt dafuer den gegrabbten Cursor). Rechtsklick-Halten (essen/angeln) laeuft
  dagegen auch unfokussiert.
- Server-Anti-Cheat kann Auto-Klicker als verdaechtig einstufen. Der Mod umgeht nichts -
  auf fremden Servern auf eigene Verantwortung.

## Bauen
```bash
./gradlew build          # bzw. build.bat / build.sh  (nutzt mitgeliefertes gradle-9.7.1)
# Ergebnis: build/libs/autoclicker-1.0.1.jar
```
Jar nach `.minecraft/mods/` (Fabric Loader + Fabric API noetig). Direkt installieren: `install.bat`.
Der Mod zeigt bewusst **kein** HUD-Overlay - an/aus siehst du nur an der Chat-Zeile beim Umschalten.

## Selbsttest (dev)
```bash
./gradle-9.7.1/bin/gradle runClientGametest
```
Fabric-Client-Gametest (`AutoClickerGameTest`): oeffnet das Einstellungs-Menue, macht
Screenshots und prueft in einer Testwelt, dass bei **unfokussiertem** Fenster weiter geklickt
wird und kein Spielmenue aufpoppt. Laeuft nur mit `-Dfabric.client.gametest`.
