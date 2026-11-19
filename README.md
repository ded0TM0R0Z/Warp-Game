# WARP GAME

WARP GAME — Android gaming platform with a game catalog, downloadable games, local installation and offline execution.

The platform is designed to support both 2D and 3D games, with development and testing performed primarily from an Android phone.

## Project status

**Stage:** Catalog, package manager, Game SDK and initial 2D/3D engine registry implemented  
**Current priority:** Verify the engine registry build, then validate Phaser/Babylon rendering, touch and SDK behavior on the target phone.

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

## Build

Current app version: **0.2.0**. Android debug builds run through GitHub Actions and use a verified permanent signing key. The latest APK is available from [Android Build #45](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44571054178). See [Build environment](docs/BUILD_ENVIRONMENT.md) for verification details and installation instructions.

## Core principles

1. Android is the target platform.
2. Both 2D and 3D games are supported by the platform architecture.
3. Installed games must launch offline.
4. Game files and save data are stored separately.
5. Modules communicate through documented, versioned interfaces.
6. Shared architecture changes require approval in the MAIN CONTROL chat.
7. Every task must include changed files and test results.

See the documentation above before making changes.
