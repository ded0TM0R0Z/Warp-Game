# WARP GAME — BUILD ENVIRONMENT

**Version:** 1.0.1  
**Status:** First cloud APK build succeeded; physical-device installation remains pending.

## 1. Decision

Use GitHub Actions as the primary Android build environment.

Reason:
- Project development is performed from an Android phone.
- The Android source remains in the GitHub repository.
- GitHub-hosted runners compile the APK in the cloud.
- The user downloads the APK artifact to the phone and installs it for testing.
- No Android IDE or local Gradle installation is required for the basic edit-build-test cycle.

The repository is public. GitHub documents standard GitHub-hosted runner usage as free for public repositories. Confirm current account and repository billing settings before enabling paid add-ons or larger runners.

## 2. Pinned prototype toolchain

| Component | Version | Purpose |
|---|---:|---|
| Android Gradle Plugin | 8.13.0 | Android build plugin |
| Gradle | 8.13 | Build system |
| Kotlin Gradle Plugin | 2.3.0 | Kotlin compilation |
| JDK | 17 | Build runtime |
| Android compile SDK | 36 | Compile platform |
| Android build tools | 36.0.0 | APK packaging |
| Minimum Android SDK | 26 | Initial Android 8.0 minimum |

Compatibility basis:
- AGP 8.13 requires Gradle 8.13.
- Kotlin Gradle Plugin 2.3.0 supports AGP through 8.13.0 and Gradle through 9.0.
- AGP 8.x requires JDK 17.

These versions are pinned for the initial prototype. Do not upgrade them independently. Change them only after a successful compatibility check and an approved documentation update.

## 3. Automatic build

Workflow: `.github/workflows/android_build.yml`

Triggers:
- Push to `main` affecting Android source/build files or the workflow.
- Pull request targeting `main` affecting Android source/build files or the workflow.
- Manual workflow dispatch from GitHub Actions.

Build command:
```bash
gradle --no-daemon --stacktrace assembleDebug
```

Artifact:
- Name: `warp-game-debug-apk`
- Output: `app/build/outputs/apk/debug/*.apk`
- Retention: 7 days.

## 4. Successful builds

First successful build:
- Workflow run: [Android Build #8](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44480683282)
- Commit: `67ee53f6be6e953a85d389a6ff710abc988610a7`
- Artifact size: 867,894 bytes.

Latest successful build:
- Workflow run: [Android Build #13](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44481143736)
- Commit: `1a62a02697f40c428fae3e04beab56ac95da93ed`
- Result: successful.
- Artifact: `warp-game-debug-apk`
- Artifact size: 867,894 bytes.
- Artifact SHA-256: `1ded965ae6aba722f9cd6cb2509cd7d0af6c3b3abefea39965c3d985b1ac714e`
- Artifact expiry: 2026-11-26.

The first workflow attempt exposed an obsolete Android SDK package request, which was removed. A later build was blocked by a temporary Maven Central HTTP 429 response; rerunning the same build succeeded. The Kotlin compiler configuration was also updated to the typed `compilerOptions` DSL.

## 5. Downloading the APK on Android

1. Open the repository on GitHub.
2. Open the **Actions** tab.
3. Select the latest successful **Android Build** run.
4. In **Artifacts**, download `warp-game-debug-apk`.
5. Extract the ZIP archive.
6. Install the APK on the phone.
7. If Android asks, allow installation from the browser or file manager used to open the APK.

Do not install an artifact from a failed or untrusted workflow run.

## 6. Required validation

The cloud build is confirmed. The build environment is not fully validated until:
- [x] GitHub Actions completes successfully.
- [x] The APK artifact is uploaded and downloadable.
- [ ] The APK installs on the user's Android phone.
- [ ] The app launches without crashing.
- [ ] Device model and Android version are recorded.

## 7. Limitations

- A successful cloud build does not prove correct behavior on the physical phone.
- Device performance, WebView behavior, thermal load and offline behavior require real-device tests.
- Debug APKs are for development, not public release.
- Release signing keys must never be committed to the repository.
