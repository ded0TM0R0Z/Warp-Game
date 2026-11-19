# WARP GAME — REMOTE GAME DISTRIBUTION

**Architecture:** Static catalog + GitHub Releases package storage  
**Catalog endpoint:** `https://raw.githubusercontent.com/ded0TM0R0Z/Warp-Game/main/catalog/catalog.json`  
**Package storage:** Public GitHub Release assets in this repository

## 1. How the system works

1. Game source code lives in `games/<game_id>/` in the repository.
2. A game is built and packaged as a ZIP archive outside the Android application.
3. The `Publish Game Package` GitHub Actions workflow validates the manifest and files.
4. The workflow creates or updates a GitHub Release and uploads the ZIP as a release asset.
5. The workflow calculates SHA-256 and exact byte size, then updates `catalog/catalog.json`.
6. WARP GAME requests the remote catalog over HTTPS.
7. When the user selects Install or Update, the app downloads the ZIP from the release asset URL, verifies its size and SHA-256, validates the package manifest and archive, then installs it into private app storage.
8. Installed games launch locally and remain playable offline.

The Android APK contains the platform runtime and SDK, not the published game library. Games are separate downloadable packages.

## 2. Hosting choice

GitHub Releases is the initial distribution server/CDN. It provides versioned public binary assets, HTTPS delivery and redirect-based CDN downloads. The catalog is a small static JSON file served from GitHub's raw content host.

This avoids maintaining a VPS, database, API server, domain, TLS certificates or billing system for the first game releases.

The hosting layer is replaceable: the catalog format, ZIP format, SHA-256 validation and installer are independent of GitHub. A dedicated API/object-storage backend can be introduced later without changing the game package contract.

## 3. Publish a game

Create a directory:

```text
games/<game_id>/
├── manifest.json
├── index.html
├── scripts/
├── styles/
├── assets/
└── libraries/
```

The `game_id` must match the permanent ID in the manifest and use:
`[A-Za-z0-9][A-Za-z0-9._-]{2,127}`.

The manifest must include:
- `game_id`, `name`, `description`, `version`
- `engine`, `engine_version`, `game_type`
- `entry_point`, `orientation`
- `offline_supported: true`
- `minimum_platform_version`

The publisher currently accepts engines `html5`, `phaser`, `babylonjs` and `custom`, game types `2d` and `3d`, and semantic versions in `MAJOR.MINOR.PATCH` form.

After the game source is merged into `main`:

1. Open GitHub repository → Actions.
2. Select **Publish Game Package**.
3. Choose **Run workflow** on `main`.
4. Enter the exact `game_id` and manifest `version`.
5. Run the workflow and wait for successful completion.

The workflow validates the package, creates the release asset, writes the verified download URL/checksum/size to the catalog and commits the catalog update.

A failed workflow must be investigated before retrying. If the release upload succeeded but catalog publishing failed, rerunning the same ID/version safely replaces the same release asset and retries the catalog update.

## 4. Package limits

The publisher enforces:
- ZIP package: 1 byte to 256 MiB.
- Expanded package: at most 512 MiB.
- Single expanded file: at most 128 MiB.
- Files: at most 10,000.
- `manifest.json`: at most 64 KiB.
- Safe relative paths only; symbolic links are rejected.
- The manifest entry point must exist in the package.
- Offline startup is mandatory.

The Android installer independently validates the package and trusted catalog metadata. The publisher is not a replacement for client-side verification.

## 5. Security

- Catalog and package URLs must use HTTPS.
- The Android client trusts only the configured WARP GAME repository's release assets and the expected GitHub release CDN redirect hosts.
- Every package is checked against catalog SHA-256 and byte size before installation.
- ZIP paths are checked before extraction.
- Games run in the restricted local WebView runtime and use the explicit WarpGameSDK bridge.
- The app does not expose arbitrary filesystem access to games.

Do not put credentials, signing keys, API tokens or private data in game packages or the public repository.

## 6. Current service limits

This is a static distribution service, not a full multiplayer backend.

Included:
- Public read-only catalog.
- Remote package hosting and delivery.
- Versioned releases and updates.
- Package integrity metadata.
- Offline launch after installation.

Not included:
- User accounts and authentication.
- User-uploaded games or moderation.
- Ratings, reviews, comments or recommendations.
- Payments, purchases or subscriptions.
- Cloud saves or cross-device synchronization.
- Multiplayer game servers or realtime APIs.
- Private or paid package access.

These require a separate backend and security model. They are deliberately outside the first game publishing milestone.

## 7. Acceptance checklist

- [ ] Publish a small 2D game and verify its catalog entry appears in the app.
- [ ] Install it from the remote release asset.
- [ ] Confirm SHA-256 and package size validation.
- [ ] Launch after disconnecting the internet.
- [ ] Publish a new version and update the installed game.
- [ ] Confirm saves survive an update.
- [ ] Confirm the old game remains installed if a new package fails validation.
- [ ] Confirm no game ZIP or engine library is bundled in the production APK.
