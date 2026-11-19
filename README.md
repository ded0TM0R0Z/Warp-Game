# WARP GAME

WARP GAME — Android gaming platform with a game catalog, downloadable games, local installation and offline execution.

The platform targets modern mid-range Android phones and above, supports both 2D and 3D games, and scales graphics down for compatibility without imposing a low-end performance ceiling.

## Project status

**Current stage:** Persistent serial download queue and Downloads screen are implemented in PR #13; physical-device validation remains pending.  
**Implemented:** Catalog discovery, package installer, isolated offline WebView runtime, Game SDK, engine registry, main-menu Back navigation and persistent download queue.

## Documentation

- [Technical standard](docs/TECHNICAL_STANDARD.md)
- [Device targets and graphics scaling](docs/DEVICE_TARGETS.md)
- [Project rules](docs/PROJECT_RULES.md)
- [Task template for development chats](docs/CHAT_TASK_TEMPLATE.md)
- [Phone development workflow](docs/PHONE_WORKFLOW.md)
- [Build environment](docs/BUILD_ENVIRONMENT.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Game package specification](docs/GAME_PACKAGE_SPEC.md)
- [Catalog backend](docs/CATALOG_BACKEND.md)
- [Game SDK](docs/GAME_SDK_SPEC.md)
- [Engine support](docs/ENGINE_SUPPORT.md)
- [Phaser demo and acceptance checks](docs/PHASER_DEMO.md)
- [WebGL 3D demo and acceptance checks](docs/WEBGL_3D_DEMO.md)
- [Catalog search and download controls](docs/CATALOG_SEARCH_DOWNLOADS.md)
- [Persistent download queue](docs/DOWNLOAD_QUEUE.md)

## Build

Current app version: **0.2.0**. Android debug builds run through GitHub Actions and use a permanent signing key. The latest confirmed main-branch build is [Android Build #73](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44589070645). The Phaser and WebGL demo packages are included in the latest main-branch build. PR #13 adds the persistent download queue and requires a successful pull-request build before merge. New changes must pass their pull-request build before merge. See [Build environment](docs/BUILD_ENVIRONMENT.md) for verification details.

## Core principles

1. Android is the target platform.
2. Both 2D and 3D games are required platform capabilities.
3. Installed games must launch offline.
4. Game files and save data are stored separately.
5. Modules communicate through documented, versioned interfaces.
6. Shared architecture changes require approval in the MAIN CONTROL chat.
7. A successful cloud build is not proof of device compatibility.

See the documentation above before making changes.
