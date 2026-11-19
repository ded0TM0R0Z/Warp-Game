# WARP GAME — BUILD ENVIRONMENT

**Version:** 1.0.3  
**Status:** Stable signing key verified; local HTML5 runtime APK builds successfully. Physical-device runtime validation remains pending.

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
| AndroidX WebKit | 1.18.0 | Secure local WebView content |
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

Previous successful build:
- Workflow run: [Android Build #13](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44481143736)
- Commit: `1a62a02697f40c428fae3e04beab56ac95da93ed`
- Result: successful.
- Artifact: `warp-game-debug-apk`
- Artifact size: 867,894 bytes.
- Artifact SHA-256: `1ded965ae6aba722f9cd6cb2509cd7d0af6c3b3abefea39965c3d985b1ac714e`
- Artifact expiry: 2026-11-26.

The first workflow attempt exposed an obsolete Android SDK package request, which was removed. A later build was blocked by a temporary Maven Central HTTP 429 response; rerunning the same build succeeded. The Kotlin compiler configuration was also updated to the typed `compilerOptions` DSL.

## 5. Latest signed build

- Workflow run: [Android Build #33](https://github.com/ded0TM0R0Z/Warp-Game/actions/runs/44568310650)
- Commit: `377309e01f0a7267c5115fb88ed3041c4cd3ae2f`
- Result: successful.
- Artifact: `warp-game-debug-apk`
- Artifact ID: `14553965635`
- Artifact ZIP SHA-256: `8a22fcd0fb74c89e5bc626af28a01255aef98464fca50e58e1b4ca83d424f890`
- APK size: 1,394,582 bytes.
- APK SHA-256: `c1210ef43313a3eb9ce35494b28511c42925445c5a5c90122f330925f4378550`
- Signing certificate SHA-256: `feb23e40910a3faf207cedc2644397c4dc76d4ce347e50b95fdf2f9c74c24a07`
- Artifact expiry: 2026-11-26.

The workflow validated the permanent PKCS12 keystore. The APK v2 signing certificate was extracted and its SHA-256 fingerprint matched the certificate in the keystore.

If the phone still has an APK signed with the old temporary debug key, uninstall that version once before installing this stable-key build. Later builds signed with the same key can update it normally.

## 6. Downloading the APK on Android

1. Open the repository on GitHub.
2. Open the **Actions** tab.
3. Select the latest successful **Android Build** run.
4. In **Artifacts**, download `warp-game-debug-apk`.
5. Extract the ZIP archive.
6. Install the APK on the phone.
7. If Android asks, allow installation from the browser or file manager used to open the APK.

Do not install an artifact from a failed or untrusted workflow run.

## 7. Required validation

The cloud build is confirmed. The build environment is not fully validated until:
- [x] GitHub Actions completes successfully.
- [x] The APK artifact is uploaded and downloadable.
- [ ] The APK installs on the user's Android phone.
- [ ] The app launches without crashing.
- [ ] Device model and Android version are recorded.

## 8. Limitations

- A successful cloud build does not prove correct behavior on the physical phone.
- Device performance, WebView behavior, thermal load and offline behavior require real-device tests.
- Debug APKs are for development, not public release.
- Release signing keys must never be committed to the repository.
