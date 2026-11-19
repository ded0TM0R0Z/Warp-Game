# PHASER DEMO — КРИСТАЛЬНЫЙ РЕЙД

## Purpose

A small offline 2D game used to verify that Warp Game can install and run a real Phaser project rather than only a plain HTML page.

## Package

- Game ID: `com.warp.demo.phaser`
- Package version: `1.0.0`
- Engine: Phaser `3.90.0`
- Type: `2d`
- Orientation: landscape
- Offline: required
- Entry point: `index.html`
- Runtime: `libraries/phaser.min.js`

All runtime files are stored in `app/src/main/assets/phaser_demo/`. The app action **Установить 2D-игру Phaser** creates a ZIP, calculates its SHA-256 and passes it through the regular package installer. The installer checks the manifest, engine version, local Phaser library and entry point.

## Gameplay and SDK coverage

- Touch or drag on the game field to move the player.
- Collect the green crystal to increase the score.
- Best score is persisted using `WarpGameSDK.saveData`.
- The game restores the best score through `WarpGameSDK.loadData`.
- Crystal collection triggers a short vibration through the SDK.
- No network requests or remote assets are used.

## Upstream dependency

Phaser 3.90.0 is distributed under the MIT license. The exact minified upstream distribution is included in `libraries/phaser.min.js`; the license notice is included as `PHASER-LICENSE.txt`.

- Official release: https://phaser.io/download/release/v3.90.0
- Official source: https://github.com/phaserjs/phaser/tree/v3.90.0
- License: MIT

## Physical-device acceptance checklist

The following checks have not been marked complete until run on a real Android phone:

1. Install the APK and open Settings.
2. Tap **Установить 2D-игру Phaser**.
3. Open the installed game from Library.
4. Confirm the game renders without a blank screen or console/runtime error.
5. Move the player with touch and collect at least five crystals.
6. Exit to the platform menu and relaunch the game.
7. Confirm the saved best score remains.
8. Disable network access and repeat launch/gameplay.
9. Observe touch latency, frame pacing, memory use and device temperature.

A successful GitHub Actions build confirms Kotlin/Android compilation only. It does not complete this device checklist.
