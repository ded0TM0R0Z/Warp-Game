# WARP GAME — DEVELOPMENT PLAN

**Version:** 1.3.0  
**Current stage:** 7 — Catalog discovery, downloads and 3D runtime validation

## Stage 0 — Foundation and validation

- [x] Create repository documentation structure and project rules.
- [x] Record technical architecture and task handoff format.
- [x] Add Android project skeleton and GitHub Actions build workflow.
- [x] Pin prototype build toolchain.
- [x] Complete successful cloud builds and upload APK artifacts.
- [ ] Download and install the APK on the target phone.
- [ ] Confirm the app launches without crashing.
- [ ] Record baseline device performance.

**Exit condition:** A repeatable cloud build produces an APK that installs and launches on the target Android phone.

## Stage 1 — Android application shell

- [x] Create the main navigation structure, catalog and settings screens.
- [x] Add persistent SQLite database foundation for installed games.
- [x] Add structured logging and initialization error handling.
- [x] Create isolated internal storage directories for games, saves, cache and database.
- [x] Make the in-app Back control return to the main menu.
- [x] Make Android Back close the game runtime instead of navigating WebView history.
- [ ] Verify lifecycle and basic offline startup on the target phone.

**Exit condition:** A stable minimal app launches offline and navigates between initial screens.

## Stage 2 — Game runtime proof of concept

- [x] Add pinned AndroidX WebKit 1.18.0 and use WebViewAssetLoader for local content.
- [x] Add a local HTML5 canvas test game in a restricted WebView.
- [x] Disable runtime network access and isolate each game's local HTTPS origin.
- [ ] Verify touch input, screen adaptation and lifecycle behavior on the target phone.
- [ ] Test SDK bridge restrictions on-device.
- [ ] Measure memory, frame rate and heat on target and weaker devices.

**Exit condition:** A local test game launches offline with controlled host communication.

## Stage 3 — Game package and local management

- [x] Implement manifest parsing and validation.
- [x] Implement ZIP path and content validation with size limits.
- [x] Implement SHA-256 verification against trusted metadata.
- [x] Implement temporary package staging and atomic installation/update rollback.
- [x] Register installed games in local storage.
- [x] Implement launch and removal.
- [x] Keep saves separate from installation files.
- [x] Add a bundled HTML5 test package that exercises installation and runtime launch.
- [ ] Verify install, update, launch, removal and save preservation on the target phone.

**Exit condition:** A test package can be safely installed, launched, removed and reinstalled without accidental save deletion.

## Stage 4 — Catalog and downloads

- [x] Define versioned static catalog data format and HTTPS source.
- [x] Implement catalog browsing and package download progress.
- [x] Handle offline catalog access through a local cache and show server errors.
- [x] Verify package size, SHA-256 and manifest metadata before installation.
- [x] Implement catalog search by game name, description, engine, type and ID.
- [x] Implement automatic retry for transient network failures and user cancellation during download.
- [ ] Verify search, retry, cancellation and update flows on the target phone.

**Exit condition:** A catalog game can be downloaded and installed reliably.

## Stage 5 — SDK and save manager

- [x] Implement versioned WarpGameSDK and native bridge.
- [x] Implement per-game save and load operations with quotas and atomic writes.
- [x] Implement per-game settings access.
- [x] Implement vibration, fullscreen and exit requests.
- [x] Keep saves separate from game package files.
- [ ] Verify save persistence, game isolation, vibration, fullscreen and exit behavior on the target phone.

**Exit condition:** A game can save and restore progress through the documented SDK.

## Stage 6 — First 2D game

- [x] Pin Phaser 3.90.0 and register the engine in the installer.
- [x] Include the official Phaser minified runtime locally; no CDN is required.
- [x] Create the small playable "Кристальный рейд" demo.
- [x] Package the manifest, game code, styles, engine and MIT license into an offline ZIP.
- [x] Add an in-app action to install the Phaser demo.
- [ ] Verify Phaser rendering, touch movement, save/load and offline launch on the target phone.
- [ ] Measure performance on the target phone and weaker devices.

**Exit condition:** The Phaser demo passes the platform acceptance tests on real Android devices.

## Stage 7 — 3D runtime validation and first 3D game

- [x] Register Babylon.js 9.37.1 and require its local offline runtime library in packages.
- [x] Add a small native WebGL 1 3D test package for device-level WebGL proof of concept.
- [ ] Add and validate a local Babylon.js 3D demo package.
- [ ] Compare Babylon.js/WebGL performance on target devices.
- [ ] Test graphics limits, memory, heat and device compatibility.
- [ ] Decide whether the selected runtime meets platform requirements.
- [ ] Document device-tested limitations.

**Exit condition:** 3D support is demonstrated on real devices, not assumed from desktop tests.

## Stage 8 — Catalog and download experience

- [x] Add 2D/3D category filters and engine filters to the remote catalog.
- [x] Add a game details view with package metadata and install/update action.
- [x] Add a dedicated downloads screen and persistent download state.
- [x] Implement a serial download queue with duplicate prevention, cancellation, retry and process-restart recovery.
- [ ] Implement pause/resume only after transfer state and partial-file integrity are designed.
- [ ] Publish real game packages and validate catalog search, filters and installs.

**Exit condition:** Users can discover, inspect, install and manage multiple published games reliably.

## Stage 9 — Release readiness

- [ ] Security review.
- [ ] Installation, update and rollback tests.
- [ ] Save migration and backup tests.
- [ ] Offline regression tests.
- [ ] Performance tests on target and weaker devices.
- [ ] Release build and documentation.

## Working rule

Complete and verify each stage before treating the next stage as unblocked. A successful cloud build does not replace testing on a physical Android device.
