# WEBGL 3D DEMO — ROTATING CUBE

## Purpose

This package is a small native WebGL 1 proof of concept. It verifies that the restricted Android WebView can create a WebGL context, compile shaders, upload geometry and render a 3D scene locally.

It is deliberately separate from the Babylon.js engine contract. It does not prove Babylon.js compatibility.

## Package

- Game ID: `com.warp.demo.webgl3d`
- Package version: `1.0.0`
- Engine: `html5`
- Game type: `3d`
- Graphics API: WebGL 1
- Orientation: landscape
- Offline: required
- Entry point: `index.html`

All files are stored under `app/src/main/assets/webgl3d_demo/`. The app action **Установить 3D WebGL-тест** packages these files and uses the regular SHA-256 and manifest installer.

## Test coverage

- Creates a WebGL 1 context with low-power preference.
- Compiles and links local vertex and fragment shaders.
- Uploads cube vertices, colors and indices.
- Uses perspective and rotation matrices to render a rotating 3D object.
- Supports touch dragging to rotate the cube.
- Persists touch count through `WarpGameSDK`.
- Uses no CDN, network requests, external models or textures.

## Physical-device acceptance checklist

1. Install the APK and open Settings.
2. Tap **Установить 3D WebGL-тест**.
3. Launch the installed test from Library.
4. Confirm a colored 3D cube is visible and rotates.
5. Drag across the screen and confirm the cube responds.
6. Exit and relaunch; confirm the touch count is restored.
7. Disable network access and repeat the test.
8. Record WebGL renderer, frame pacing, memory use and device temperature.

## Limits

- This test uses raw WebGL 1, not Babylon.js.
- It does not test model loading, PBR materials, shadows, post-processing or Babylon's engine layer.
- Physical-device testing remains required before claiming production 3D compatibility.
