# Changelog

## [1.0.1]

- Removed the on-screen status overlay (the green text in the top-left corner) - it is gone entirely, there is no HUD element left.
- Settings screen is 22 px shorter after dropping that toggle, so it also fits small windows / high GUI scales.

## [1.0.0]

Initial release.

- Auto clicker that keeps clicking while the Minecraft window is not focused (tab out and it keeps running).
- Click interval in hours / minutes / seconds / milliseconds (4 boxes, like every auto clicker).
- Random offset (+0-N ms) on top of the interval.
- Mouse button: left (attack) or right (use item).
- Click type: Single / Double / Triple, plus Hold (button stays pressed - auto attack per tick, or hold right-click for eating / fishing / bows).
- Repeat: until stopped or N times.
- Settings screen (F7) and toggle (F6), both rebindable in Controls.
- On-screen status overlay, including a "window unfocused - still clicking" line.
- Everything configurable in `config/autoclicker.json` (created on first start, editable by hand).
- Automatically disables "Pause on lost focus" while running (restores the previous value when switched off), which is what would otherwise stop the clicking 500 ms after tabbing out.
