# WARP GAME — JAVASCRIPT SDK

**Version:** 1.0.0  
**Status:** Initial native bridge and JavaScript wrapper implemented

## 1. Loading the SDK

Installed games can load the platform wrapper from their local WebView origin:

`<script src="/sdk/warp-game-sdk.js"></script>`

The wrapper exposes `window.WarpGameSDK`. It communicates with a restricted native bridge; games must not call the internal `WarpGameNative` object directly.

## 2. API

| Method | Result | Purpose |
|---|---|---|
| `getGameInfo()` | Object | Installed game and platform metadata |
| `saveData(key, value)` | Boolean | Save a JSON-serializable value |
| `loadData(key)` | Value or null | Load a saved value |
| `getSettings()` | Object | Read game-specific settings |
| `setSettings(settings)` | Boolean | Save a JSON settings object |
| `vibrate(durationMs)` | Boolean | Request a short vibration |
| `setFullscreen(enabled)` | Undefined | Toggle immersive display |
| `exitGame()` | Undefined | Close the current game |

Example:

```javascript
const info = WarpGameSDK.getGameInfo();

WarpGameSDK.saveData("player-progress", {
    level: 4,
    score: 1250
});

const progress = WarpGameSDK.loadData("player-progress");
WarpGameSDK.setSettings({ music: false, quality: "low" });
WarpGameSDK.vibrate(30);
```

## 3. Storage limits and isolation

- Save data is stored under the current game's private save directory.
- Different games cannot address each other's save directories through this API.
- One key may store up to 1 MiB.
- Total save data and settings per game may use up to 10 MiB.
- Settings JSON is limited to 64 KiB.
- Keys must match `[A-Za-z0-9][A-Za-z0-9._-]{0,63}`.
- The reserved key `game_settings` cannot be used with `saveData`.
- Removing a game does not remove its save data.

## 4. Native capabilities

The bridge exposes only game metadata, bounded save/settings operations, vibration, fullscreen and exit. It does not expose arbitrary file access, Android intents, contacts, accounts or unrestricted network access.

Game WebViews continue to block network requests and external navigation. SDK calls that affect Android UI are dispatched to the UI thread.

## 5. Current limitations

- The API is synchronous.
- Save-data migration between game versions is not automatic.
- Orientation control, achievements, cloud saves, payments and multiplayer APIs are not implemented.
- Device-level behavior still requires testing on the target phone.
