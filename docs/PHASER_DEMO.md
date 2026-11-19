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

The original prototype files are archived under `dev-tools/legacy-demos/phaser_demo/`. They are excluded from the Android APK. This prototype is retained as a reference for the first remotely published Phaser game.

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

The prototype was used for earlier runtime development. Its old in-app installation checklist is historical; production validation must be performed against a remotely published game.

1. Publish the game package through the `Publish Game Package` workflow.
2. Refresh the remote Catalog in WARP GAME.
3. Install the game from its remote catalog card.
4. Open the installed game from Library.
5. Confirm the game renders without a blank screen or console/runtime error.
6. Move the player with touch and collect at least five crystals.
7. Exit to the platform menu and relaunch the game.
8. Confirm the saved best score remains.
9. Disable network access and repeat launch/gameplay.
10. Observe touch latency, frame pacing, memory use and device temperature.

A successful GitHub Actions build confirms Kotlin/Android compilation only. It does not complete this device checklist.
