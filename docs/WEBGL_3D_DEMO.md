# WEBGL 3D PROTOTYPE — ROTATING CUBE

## Purpose

A small native WebGL 1 proof of concept used to check whether the Android WebView can create a WebGL context and render a local 3D scene.

It uses raw WebGL, not Babylon.js. It does not prove Babylon.js compatibility.

## Archive location

The original source files are preserved under:

`dev-tools/legacy-demos/webgl3d_demo/`

They are excluded from the Android APK. A production 3D game must be created under `games/<game_id>/` and published as a remote release package.

## Technical reference

- Runtime: WebGL 1
- Game type: 3D
- Orientation: landscape
- Shader and geometry assets: local
- Touch drag: cube rotation
- SDK use: persisted touch counter
- Network dependency: none during gameplay

## Production 3D acceptance

1. Include the exact local engine runtime required by the manifest.
2. For Babylon.js packages, include the supported local `libraries/babylon.js` runtime.
3. Publish the game through the `Publish Game Package` workflow.
4. Install it from the remote Catalog.
5. Verify rendering, touch, lifecycle, saves and offline launch on physical Android devices.
6. Measure sustained frame pacing, memory, heat and battery use on a mid-range phone and a higher-performance phone.

A successful Android build or the archived raw-WebGL prototype does not establish production 3D engine compatibility.
