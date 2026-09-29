# Auto Clicker (Fabric, MC 1.21.11)

Client-side mod: an auto clicker that **keeps clicking while Minecraft is not in focus**.
Press F6, tab into another window, and it keeps going.

## Controls

- **F6** – toggle the auto clicker on/off (chat confirms ON/OFF).
- **F7** – open (or close) the settings screen.
- Both keys are rebindable in Options → Controls, category "Auto Clicker".

There is deliberately **no HUD overlay** – the chat line on toggle is the only on-screen feedback.

## Settings (F7)

- **Click interval** – hours / minutes / secs / milliseconds (like every auto clicker).
  0 ms means "as fast as a client tick allows" (up to 20 clicks per tick, so 1000/s).
- **Random offset + -** – each click fires `interval + 0..N ms` later, which looks more human.
- **Mouse button** – Left (attack / break) or Right (use item / eat / fish).
- **Click type** – Single / Double / Triple, or **Hold** (the button stays pressed: attack every
  tick, or hold right-click for eating, fishing and bows).
- **Repeat** – until stopped, or N times.
- **Keep running when unfocused** – disables "Pause on lost focus" for the session.

Everything lives in `%APPDATA%/.minecraft/config/autoclicker.json`, is created on first start and
can be edited by hand (the mod reads it on launch).

## How it works (and why it keeps clicking when unfocused)

The mod does not move your cursor. It injects real key presses into Minecraft's own Attack/Use
bindings (`KeyBinding.setKeyPressed` + `KeyBinding.onKeyPressed`) – exactly what a physical mouse
click does. The client therefore sends the interaction packets itself, like it would for normal
clicking (no packet spoofing, no desync).

Why it survives tabbing out: Minecraft keeps ticking and processing input without window focus –
a physical mouse just sends its events to whichever window is focused. The only vanilla behaviour
that stops the client is the lost-focus pause: 500 ms after losing focus it opens the game menu,
and from then on no input is processed. This mod turns `pauseOnLostFocus` off while it runs and
restores the previous value when you switch it off.

Clicks only fire while no screen is open (the same rule the vanilla client uses for attack/use).
Minimising the window is not tested – prefer tabbing out, since minimising can throttle the tick
rate.

## Limitations

- It clicks in-game, not in other applications (the cursor is never moved).
- Hold-to-break blocks (left button held down) only works while the window is focused, because
  vanilla requires a grabbed cursor for continuous breaking. Holding right-click (eating, fishing,
  bows) does work unfocused.
- Server-side anti-cheat may consider an auto clicker suspicious. The mod bypasses nothing –
  use it on multiplayer servers at your own risk.

## Build

```bash
./gradlew build          # or build.bat / build.sh (uses the bundled gradle-9.7.1 locally)
# result: build/libs/autoclicker-1.0.1.jar
```

Drop the jar into `.minecraft/mods/` (Fabric Loader + Fabric API required), or run `install.bat`
to build and copy it in one step. Requires JDK 21.

## Self-test (dev)

```bash
./gradlew runClientGametest
```

The Fabric client gametest (`AutoClickerGameTest`) opens the settings screen, takes screenshots and
asserts in a test world that clicks keep firing with an **unfocused** window and that no game menu
pops up. It only runs with `-Dfabric.client.gametest`, so it is inert in a released jar.

## License

MIT – see [LICENSE](LICENSE).
