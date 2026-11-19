# WARP GAME — PHONE DEVELOPMENT WORKFLOW

**Version:** 1.0.0  
**Target:** Android phone as the primary work device.

## 1. Rules

- The phone is the primary device for project management, editing, review and testing.
- GitHub is the shared source of truth.
- Do not assume a desktop computer is available.
- Do not select a build tool solely because it works in a desktop tutorial.
- Do not begin large-scale Android implementation until a real phone-based build and test path has been proven.

## 2. Repository workflow

1. Open the WARP GAME repository on GitHub.
2. Check that the correct repository and branch are selected.
3. Open the file assigned to the task.
4. Make only the changes allowed by the task.
5. Review the complete changed file before saving.
6. Commit changes with a clear message.
7. Return to MAIN CONTROL with the commit SHA and test results.

For multi-file or large code changes, use a browser-based development environment only after its availability and Android-phone usability have been tested.

## 3. Build environment validation — required before app implementation

MAIN CONTROL must select and test a phone-accessible environment that can:
- Open the repository.
- Edit Kotlin and project configuration files.
- Resolve pinned dependencies.
- Run Gradle builds.
- Produce an installable APK.
- Run or retrieve build logs.
- Support debugging and repeatable builds.

The selected environment must be tested from the user's actual Android phone. A website being accessible in a mobile browser does not prove that its editor or build tools work correctly.

## 4. Device test procedure

For each installable build:
1. Record device model and Android version.
2. Install the APK.
3. Launch the app.
4. Test the changed feature.
5. Check logs or visible errors.
6. Test behavior with internet disabled when relevant.
7. Record crashes, performance problems and reproduction steps.

## 5. Safe file handling

- Keep source code in GitHub.
- Keep signing keys and secrets outside the repository.
- Do not delete save data during application or game removal tests unless the test explicitly requires it.
- Keep test builds separate from release builds.
- Record the commit used to produce each APK.

## 6. Current status

The phone-based Android build environment has not yet been validated. This is the first infrastructure task. Do not report the platform as buildable until an APK has been built and installed successfully on the target phone.
