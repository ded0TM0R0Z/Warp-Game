# WARP GAME — GAME PACKAGE SPECIFICATION

**Version:** 1.1.0  
**Status:** Initial implementation contract; archive limits and validation rules are defined below.

## 1. Purpose

A game package is a self-contained ZIP archive that can be validated, installed, launched offline and updated independently of the platform application.

## 2. Required package layout

```text
game_package/
├── manifest.json
├── index.html
├── scripts/
├── styles/
├── assets/
└── libraries/
```

The manifest entry point must resolve to a file inside the package. Required runtime assets and libraries must be stored locally. CDN access must not be required for core gameplay.

## 3. Required manifest fields

| Field | Purpose |
|---|---|
| `game_id` | Permanent unique identifier |
| `name` | Display name |
| `version` | Game package version |
| `engine` | Runtime engine identifier |
| `engine_version` | Exact engine version |
| `game_type` | `2d` or `3d` |
| `entry_point` | Local start file |
| `orientation` | Requested screen orientation |
| `offline_supported` | Whether offline launch is supported |
| `minimum_platform_version` | Minimum compatible WARP GAME version |

## 4. Proposed initial JSON example

This is an illustrative example, not a published production schema. The exact validation rules and accepted orientation values must be finalized before the installer is implemented.

```json
{
    "game_id": "com.example.sample_game",
    "name": "Sample Game",
    "version": "1.0.0",
    "engine": "phaser",
    "engine_version": "TO_BE_PINNED",
    "game_type": "2d",
    "entry_point": "index.html",
    "orientation": "landscape",
    "offline_supported": true,
    "minimum_platform_version": "1.0.0"
}
```

## 5. Validation requirements

The installer must reject a package when:
- The manifest is missing or invalid.
- A required field is missing or has an invalid type.
- The game identifier is invalid or conflicts with another installed game.
- The entry point does not exist.
- A referenced required file is absent.
- An archive path attempts to escape the extraction directory.
- The archive contains unsupported or unsafe entries.
- The package checksum does not match trusted catalog metadata.
- The package is incompatible with the installed platform version.

## 6. Archive safety

Initial platform limits:

| Limit | Maximum |
|---|---:|
| Compressed ZIP size | 256 MiB |
| Total expanded size | 512 MiB |
| Single expanded file | 128 MiB |
| Archive entries | 10,000 |
| `manifest.json` size | 64 KiB |
| Path length | 1,024 characters |
| Path segment length | 240 characters |

Validation rules:

- Verify the complete ZIP SHA-256 against trusted catalog metadata before extraction.
- Check available internal storage against the expanded package size plus a 16 MiB reserve.
- Normalize and validate every archive path before extraction.
- Reject absolute paths, backslashes, drive prefixes, control characters, empty path segments, `.`, and `..`.
- Reject duplicate archive paths.
- Resolve every extracted path canonically and keep it inside the temporary installation directory.
- Reject missing or invalid root `manifest.json`.
- Reject a missing manifest entry point.
- Do not overwrite files outside the game's temporary installation directory.
- Extract into a staging directory first.
- Promote the package to the installed location only after all checks pass.
- When updating, keep the previous installation until the new directory and database record are committed.
- Failed installation must restore the previous installation and leave no partially registered game.

Manifest compatibility rules:

- `game_id` must match `[A-Za-z0-9][A-Za-z0-9._-]{2,127}`.
- `game_type` must be `2d` or `3d`.
- `offline_supported` must be `true`; packages that cannot start offline are rejected.
- `minimum_platform_version` must contain three numeric components and must not exceed the installed platform version.
- `orientation` must be a non-empty string. Mapping it to Android orientation constants remains an open decision.
- The platform version is currently `0.2.0`.

## 7. Save data

Save data must be stored outside the installed package directory and scoped by permanent `game_id`.

Removing a game must not automatically remove its saves. Save deletion must be a separate user action.

## 8. Updates

An update must:
- Verify the new package before replacing the current version.
- Preserve saves by default.
- Avoid leaving a partially updated installation.
- Define compatibility and migration behavior before changing save formats.
- Support recovery to the previous working version where feasible.

The exact update and rollback transaction is an implementation requirement still to be designed and tested.

## 9. Versioning

- `version` identifies the game package release.
- `engine_version` identifies the exact runtime engine version used.
- `minimum_platform_version` declares the minimum platform version.
- Breaking changes to this package contract require a major specification version increase and MAIN CONTROL approval.
