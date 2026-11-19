# WARP GAME — PHONE DEVELOPMENT WORKFLOW

**Version:** 1.1.0  
**Target:** Android phone as the primary work device.

## 1. Rules

- The phone is the primary device for project management, editing, review and testing.
- GitHub is the shared source of truth.
- Do not assume a desktop computer is available.
- The primary build path is GitHub Actions cloud builds. A local Android IDE is not required for the basic workflow.
- Do not report the platform as buildable until an APK has been built and installed successfully on the target phone.

## 2. Repository workflow

1. Open the WARP GAME repository on GitHub.
2. Check that the correct repository and branch are selected.
3. Open the file assigned to the task.
4. Make only the changes allowed by the task.
5. Review the complete changed file before saving.
6. Commit changes with a clear message.
7. GitHub Actions automatically attempts an Android debug build after a push to `main`.
8. Return to MAIN CONTROL with the commit SHA and test results.

For large code changes, MAIN CONTROL may edit and commit files directly through the connected GitHub integration.

## 3. Download and install a test APK

1. Open the repository's **Actions** tab.
2. Open the latest **Android Build** workflow run.
3. Wait for the run to finish successfully.
4. Download the `warp-game-debug-apk` artifact.
5. Extract the ZIP archive.
6. Install the APK on the phone.
7. Launch the app and test the changed feature.

If no workflow run appears, check whether GitHub Actions is enabled for the repository.

## 4. Device test procedure

For each installable build:
1. Record device model and Android version.
2. Record the commit SHA used for the build.
3. Install the APK.
4. Launch the app.
5. Test the changed feature.
6. Check logs or visible errors.
7. Test behavior with internet disabled when relevant.
8. Record crashes, performance problems and reproduction steps.

## 5. Safe file handling

- Keep source code in GitHub.
- Keep signing keys and secrets outside the repository.
- Do not delete save data during application or game removal tests unless the test explicitly requires it.
- Keep debug builds separate from release builds.
- Record the commit used to produce each APK.

## 6. Current status

The Android project skeleton and cloud build workflow have been committed. The first successful workflow run and installation on the target phone are still required before the build environment is considered validated.
