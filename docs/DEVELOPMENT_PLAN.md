# WARP GAME — DEVELOPMENT PLAN

**Version:** 1.1.5  
**Current stage:** 0 — Foundation and device validation

## Stage 0 — Foundation and validation

- [x] Create repository documentation structure.
- [x] Establish project rules and task handoff format.
- [x] Record initial technical architecture.
- [x] Add Android project skeleton.
- [x] Add GitHub Actions debug build workflow.
- [x] Pin prototype build toolchain.
- [x] Complete a successful cloud build.
- [x] Upload the debug APK artifact.
- [ ] Download and install the APK on the target phone.
- [ ] Confirm the app launches without crashing.
- [ ] Record baseline device performance.

**Exit condition:** A repeatable cloud build produces an APK that installs and launches on the target Android phone.

## Stage 1 — Android application shell

- [x] Create the main navigation structure.
- [x] Add initial catalog screen.
- [x] Add settings screen.
- [x] Add persistent SQLite database foundation for installed games.
- [x] Add structured logging and initialization error handling.
- [x] Create isolated internal storage directories for games, saves, cache and database.
- [ ] Verify lifecycle and basic offline startup.

**Exit condition:** A stable minimal app launches offline and navigates between initial screens.

## Stage 2 — Game runtime proof of concept

- [x] Add pinned AndroidX WebKit 1.18.0 and use WebViewAssetLoader for local content.
- [x] Add a local HTML5 canvas test game in a restricted WebView.
- [ ] Verify touch input, screen adaptation and lifecycle behavior on the target phone.
- [ ] Test SDK bridge restrictions.
- [ ] Measure memory, frame rate and heat on target devices.

**Exit condition:** A local test game launches offline with controlled host communication.

## Stage 3 — Game package and local management

- [x] Implement manifest parsing and validation.
- [x] Implement ZIP path and content validation with size limits.
- [x] Implement SHA-256 verification against trusted metadata.
- [x] Implement temporary package staging and atomic installation/update rollback.
- [x] Register installed games in local storage.
- [x] Implement launch and removal.
- [x] Keep saves separate from installation files.
- [x] Add a bundled test package that exercises installation and runtime launch.
- [ ] Verify install, update, launch, removal and save preservation on the target phone.

**Exit condition:** A test package can be safely installed, launched, removed and reinstalled without accidental save deletion.

## Stage 4 — Catalog and downloads

- [ ] Define catalog data format and API.
- [ ] Implement catalog browsing and search.
- [ ] Implement download progress, retry and cancellation.
- [ ] Handle offline and server errors.
- [ ] Verify package integrity before installation.

**Exit condition:** A catalog game can be downloaded and installed reliably.

## Stage 5 — SDK and save manager

- [ ] Implement the versioned WarpGameSDK.
- [ ] Implement save and load operations.
- [ ] Implement game settings access.
- [ ] Implement vibration, fullscreen and exit requests.
- [ ] Test isolation between games.

**Exit condition:** A game can save and restore progress through the documented SDK.

## Stage 6 — First 2D game

- [ ] Pin Phaser version after runtime validation.
- [ ] Create a small complete game.
- [ ] Package all dependencies locally.
- [ ] Test touch, save/load and offline launch.
- [ ] Measure performance on target and weaker devices.

**Exit condition:** First 2D game passes the platform acceptance tests.

## Stage 7 — 3D runtime validation and first 3D game

- [ ] Compare Babylon.js/WebGL performance on target devices.
- [ ] Test graphics limits, memory, heat and device compatibility.
- [ ] Decide whether the selected runtime meets platform requirements.
- [ ] Pin versions and document limitations.
- [ ] Build a small 3D test game.

**Exit condition:** 3D support is demonstrated on real devices, not assumed from desktop tests.

## Stage 8 — Release readiness

- [ ] Security review.
- [ ] Installation, update and rollback tests.
- [ ] Save migration and backup tests.
- [ ] Offline regression tests.
- [ ] Performance tests on target and weaker devices.
- [ ] Release build and documentation.

## Working rule

Complete and verify each stage before treating the next stage as unblocked. MAIN CONTROL may split a stage into dedicated task chats.
