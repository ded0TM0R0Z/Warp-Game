# WARP GAME — TECHNICAL STANDARD

**Version:** 1.0.0  
**Status:** Initial architecture specification  
**Authority:** MAIN CONTROL

This document preserves the initial technical standard approved for the project. Unresolved implementation choices are tracked separately and must not be guessed by individual development chats.

## 1. Project purpose

WARP GAME is an Android gaming platform with an integrated game catalog, downloadable games, local installation, offline execution and independent game packages.

The platform must support both 2D and 3D games. Development and testing are performed primarily using an Android smartphone.

## 2. Technology stack

### Main application
- Kotlin
- Android SDK
- AndroidX
- Android WebView

### 2D runtime
- HTML5
- CSS
- JavaScript
- Phaser 3

### 3D runtime
- HTML5
- JavaScript
- Babylon.js
- WebGL

### Data and storage
- JSON
- SQLite
- SHA-256
- ZIP game packages

### Development infrastructure
- GitHub
- Git
- Markdown documentation

All library versions must be explicitly pinned before they are used in a release build. Do not invent or silently upgrade versions.

## 3. Main architecture

The application consists of independent modules:

- **Core** — application lifecycle and shared configuration.
- **Catalog** — game catalog and search.
- **Download Manager** — downloading and installation.
- **Game Manager** — game registration, launch and removal.
- **Game Runtime** — execution of installed games.
- **Save Manager** — game progress and local saves.
- **Settings** — user preferences.
- **UI** — application interface.
- **Game SDK** — communication between games and the platform.

Modules must communicate through documented interfaces.

## 4. Game package

Every game must contain:
- `manifest.json`
- `index.html`
- `scripts/`
- `styles/`
- `assets/`
- `libraries/`

All runtime dependencies must be included in the game package. External CDN dependencies are prohibited for required game functionality.

Required manifest fields:
- `game_id`
- `name`
- `version`
- `engine`
- `engine_version`
- `game_type`
- `entry_point`
- `orientation`
- `offline_supported`
- `minimum_platform_version`

Game identifiers must be unique and permanent. The detailed schema is maintained in [GAME PACKAGE SPEC](GAME_PACKAGE_SPEC.md).

## 5. Game SDK

The common SDK is named `WarpGameSDK`.

Initial public API:
- `getGameInfo()`
- `saveData()`
- `loadData()`
- `getSettings()`
- `setSettings()`
- `vibrate()`
- `setFullscreen()`
- `exitGame()`

All asynchronous operations must return Promises.

The SDK must use versioning. Breaking API changes require a new major version.

Games must not directly access private application files.

## 6. Installation system

Installation sequence:

1. Retrieve catalog metadata.
2. Check platform compatibility.
3. Check available storage.
4. Download package into temporary storage.
5. Verify SHA-256 checksum.
6. Validate archive structure.
7. Validate manifest.
8. Extract files.
9. Atomically move package into permanent storage.
10. Register installed game in local database.

Incomplete installations must never be marked as installed.

## 7. Removal and updates

Game removal must delete the selected game's installation files. Save data must be stored separately.

The user must be able to:
- Remove a game without deleting saves.
- Delete saves separately.
- Reinstall a game.
- Update a game.
- Launch a previously installed game offline.

Updates must preserve compatible saves.

## 8. Storage structure

Private application storage contains:
- `games/`
- `saves/`
- `cache/`
- `database/`

Each game receives an isolated directory. Game files and save data must never share the same deletion scope.

## 9. Offline requirements

Downloaded games must launch without internet access.

The following must work offline:
- Game startup.
- Local assets.
- Game logic.
- Local saves.
- Loading existing progress.
- Basic platform navigation.

Network-dependent features must degrade gracefully when offline.

## 10. Security

- Use AndroidX WebViewAssetLoader for local content.
- Do not grant unrestricted filesystem access to WebView.
- Do not expose unrestricted native JavaScript interfaces.
- Validate game package checksums.
- Validate manifests and archive paths.
- Restrict communication between game and Android host.
- Do not execute untrusted packages.
- Keep each game's data isolated.

## 11. Naming conventions

- Files and directories: `snake_case`.
- Classes: `PascalCase`.
- Functions: `camelCase`.
- Variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- JSON keys: `snake_case`.
- Encoding: UTF-8.
- Indentation: 4 spaces.

## 12. Quality requirements

Every game must be tested for:
- Android compatibility.
- Touch input.
- Screen adaptation.
- Performance.
- Offline startup.
- Save/load correctness.
- Installation and removal.
- Memory usage.
- Error handling.

3D games require additional GPU, temperature and WebGL compatibility testing.

## 13. Development rules

- One chat handles one assigned development task.
- Main architecture is controlled by MAIN CONTROL.
- No unapproved changes to shared interfaces.
- Every task must have acceptance criteria.
- Every task must report changed files.
- Every task must report test results.
- Existing working modules must not be overwritten without review.
- Git commits must be used to preserve working versions.

## 14. Development roadmap

1. Finalize project infrastructure.
2. Implement Android application shell.
3. Implement catalog.
4. Implement download manager.
5. Implement local game installation.
6. Implement runtime.
7. Implement WarpGameSDK.
8. Implement save manager.
9. Implement backend catalog.
10. Create and test first 2D game.
11. Create and test first 3D game.

## 15. Architecture change policy

Any change affecting shared APIs, package format, storage layout, runtime or security must be approved by MAIN CONTROL.

This document is the initial technical authority for WARP GAME.

---

## Open technical decisions

These are intentionally unresolved; do not treat them as approved implementation choices:

1. Exact Android SDK, Gradle, Kotlin and library versions.
2. Phone-based build and debugging environment.
3. Android project structure and dependency management.
4. Runtime performance and compatibility limits on target devices.
5. Catalog backend and game hosting.
6. Detailed SDK bridge security model.
7. Update rollback and save migration protocol.

Resolve each decision through a documented test or research task, then update this standard with a version change.
