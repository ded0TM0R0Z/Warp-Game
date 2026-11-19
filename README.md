# WARP GAME

WARP GAME — Android gaming platform with a game catalog, downloadable games, local installation and offline execution.

The platform is designed to support both 2D and 3D games, with development and testing performed primarily from an Android phone.

## Project status

**Current stage:** Offline Phaser 2D and native WebGL 3D demos added; Android build and physical-device validation in progress.  
**Implemented:** Catalog foundation, package installer, isolated offline WebView runtime, Game SDK, engine registry and main-menu Back navigation.

## Documentation

- [Technical standard](docs/TECHNICAL_STANDARD.md)
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

## Build

Current app version: **0.2.0**. Android debug builds run through GitHub Actions and use a permanent signing key. The latest confirmed main-branch build is [Android Build #45](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44571054178). New changes must pass their pull-request build before merge. See [Build environment](docs/BUILD_ENVIRONMENT.md) for verification details.

## Core principles

1. Android is the target platform.
2. Both 2D and 3D games are required platform capabilities.
3. Installed games must launch offline.
4. Game files and save data are stored separately.
5. Modules communicate through documented, versioned interfaces.
6. Shared architecture changes require approval in the MAIN CONTROL chat.
7. A successful cloud build is not proof of device compatibility.

See the documentation above before making changes.
