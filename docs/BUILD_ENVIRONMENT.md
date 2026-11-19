# WARP GAME — BUILD ENVIRONMENT

**Version:** 1.0.0  
**Status:** Initial cloud build configuration committed; first successful build still must be confirmed.

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
- Push to `main`.
- Pull request targeting `main`.
- Manual workflow dispatch from GitHub Actions.

Build command:
```bash
gradle --no-daemon --stacktrace assembleDebug
```

Artifact:
- Name: `warp-game-debug-apk`
- Output: `app/build/outputs/apk/debug/*.apk`
- Retention: 7 days.

## 4. Downloading the APK on Android

1. Open the repository on GitHub.
2. Open the **Actions** tab.
3. Select the latest **Android Build** run.
4. Wait until the run is completed successfully.
5. In **Artifacts**, download `warp-game-debug-apk`.
6. Extract the ZIP archive.
7. Install the APK on the phone.
8. If Android asks, allow installation from the browser or file manager used to open the APK.

Do not install an artifact from a failed or untrusted workflow run.

## 5. Required validation

The cloud build is not considered validated until:
- [ ] GitHub Actions completes successfully.
- [ ] The APK artifact is downloadable.
- [ ] The APK installs on the user's Android phone.
- [ ] The app launches without crashing.
- [ ] Device model and Android version are recorded.

## 6. Limitations

- A successful cloud build does not prove correct behavior on the physical phone.
- Device performance, WebView behavior, thermal load and offline behavior require real-device tests.
- Debug APKs are for development, not public release.
- Release signing keys must never be committed to the repository.
