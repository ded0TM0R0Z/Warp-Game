# WARP GAME — ENGINE SUPPORT

**Version:** 1.0.0  
**Status:** Engine registry and offline package validation implemented

## 1. Supported package engines

| Manifest engine | Version | Required package library | Use |
|---|---|---|---|
| `html5` | Declared by package | None | Native Canvas, WebGL or browser APIs |
| `phaser` | `3.90.0` | `libraries/phaser.min.js` | 2D games |
| `babylonjs` | `9.37.1` | `libraries/babylon.js` | 3D games using WebGL |
| `custom` | Declared by package | Included by publisher | Other self-contained frameworks |

Phaser 3.90.0 is the latest Phaser 3 release listed in the official Phaser 3 archive. Phaser 4 is a separate major version and is not used by this initial Phaser 3 contract. <https://phaser.io/download/phaser3>

Babylon.js 9.37.1 is pinned to the official upstream release published on 18 November 2026. The UMD distribution is used so games can load it through a local script tag without a bundler. <https://github.com/BabylonJS/Babylon.js/releases>

## 2. Offline packaging

Engine libraries must be included inside the game ZIP. The runtime blocks network requests, so a game must never depend on a CDN at launch.

Phaser package example:

```html
<script src="libraries/phaser.min.js"></script>
<script src="scripts/game.js"></script>
```

Babylon.js package example:

```html
<script src="libraries/babylon.js"></script>
<script src="scripts/game.js"></script>
```

All engine files, models, textures, fonts, audio and other resources must be included in the package.

## 3. Runtime behavior

- Game files are served from the private installed-game directory through `WebViewAssetLoader`.
- The runtime supports local JavaScript, CSS, images, audio, video, WASM, glTF and common texture formats.
- Each game uses its own local HTTPS origin.
- Network access and external navigation remain blocked.
- 3D rendering requires WebGL support and hardware acceleration on the Android device.

## 4. Version policy

The package manifest must declare the exact engine version. The installer rejects Phaser and Babylon.js packages whose declared version does not match the registry or whose required local engine file is missing. This validates package structure and version declarations; it does not by itself prove that a real Phaser or Babylon.js game renders correctly on a phone. Device-level WebGL, touch, performance and thermal tests remain required.

Engine upgrades are handled by updating the registry and validating compatibility before publishing games built for the new version.
