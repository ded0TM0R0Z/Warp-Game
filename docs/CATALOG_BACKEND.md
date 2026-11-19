# WARP GAME — CATALOG BACKEND

**Version:** 1.0.0  
**Status:** Provisional MVP backend; catalog search and cancellable downloads implemented

## 1. Data source

The first catalog uses a static JSON file in the public WARP GAME repository:

`https://raw.githubusercontent.com/ded0TM0R0Z/Warp-Game/main/catalog/catalog.json`

Game packages are hosted as assets of GitHub Releases. This avoids operating a separate server during the initial platform build.

This is a provisional hosting choice. A dedicated backend can replace it without changing the game package format.

## 2. Catalog format

The root object contains:
- `catalog_version`: currently `1`.
- `games`: array of published game records.

Every game record contains:
- `game_id`
- `name`
- `description`
- `version`
- `engine`
- `engine_version`
- `game_type`
- `orientation`
- `package_url`
- `package_sha256`
- `package_size_bytes`
- `minimum_platform_version`
- `offline_supported`

The application rejects invalid identifiers, duplicate game IDs, unsupported game types, non-HTTPS URLs, untrusted package hosts, invalid checksums, oversized packages and games that require a newer platform version.

## 3. Package hosting

For each game release:

1. Build a ZIP that follows [GAME PACKAGE SPEC](GAME_PACKAGE_SPEC.md).
2. Calculate the SHA-256 checksum of the complete ZIP.
3. Publish the ZIP as a GitHub Release asset.
4. Add a catalog record with the exact asset URL, checksum and byte size.
5. Commit the updated `catalog/catalog.json`.

The initial package URL must use HTTPS and a trusted GitHub host. Redirects are followed only when the destination remains HTTPS and belongs to the allowlist used by the application.

## 4. Offline behavior

- The latest valid catalog is cached in private application storage.
- If the network is unavailable, the application uses the cached catalog.
- Installed games do not depend on the catalog to launch.
- Package checksums and manifest metadata are verified before installation.

## 5. Built-in demos

When the remote catalog is empty or unavailable, the app still shows three locally bundled demo packages in the Catalog screen: the Phaser 2D demo, native WebGL 3D test and HTML5 runtime test. These are installed through the same local package installer but are not entries in the remote JSON catalog and do not require a network connection.

## 6. Current limitations

- The initial catalog is public and read-only.
- Catalog search is local and matches game name, description, engine, game type and game ID.
- Downloads support user cancellation while network transfer is active.
- Transient network errors (HTTP 408/429/5xx and selected connection failures) are retried up to two times after the initial attempt.
- Cancellation is disabled once package verification succeeds and local installation begins.
- Pause and resume, accounts, ratings, reviews, payments and user submissions are not implemented.
- Retry and cancellation behavior still requires physical-device testing.
- GitHub Releases are an MVP hosting service, not the final large-scale game distribution backend.
