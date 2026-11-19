# PHASER PROTOTYPE — КРИСТАЛЬНЫЙ РЕЙД

## Purpose

A small Phaser 3.90.0 prototype created to validate 2D rendering, touch controls and WarpGameSDK save access during platform development.

## Archive location

The prototype source and runtime files are preserved under:

`dev-tools/legacy-demos/phaser_demo/`

They are not part of the Android app source set and are not bundled in the APK. The first production Phaser game will be created under `games/<game_id>/`, packaged and distributed as a remote GitHub Release asset.

## Technical reference

- Engine: Phaser 3.90.0
- Game type: 2D
- Orientation: landscape
- Local engine bundle: `libraries/phaser.min.js`
- License: MIT (license notice retained in the archive)
- SDK use: per-game save/load and vibration
- Network dependency: none during gameplay

## Production acceptance

1. Place the completed game under `games/<game_id>/`.
2. Validate its `manifest.json` and local entry point.
3. Run Publisher Tests.
4. Publish it through the `Publish Game Package` workflow.
5. Refresh the remote Catalog and install the release package.
6. Verify checksum, install, launch, save/load and offline operation on a physical Android phone.

This document describes the archived prototype; it does not claim that a production Phaser game is currently published.
