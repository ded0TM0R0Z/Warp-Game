# WARP GAME — PROJECT RULES

**Document version:** 1.0.0  
**Applies to:** Every chat, contributor and code change in this repository.

## 1. Authority

- MAIN CONTROL owns architecture, shared interfaces, package formats, naming rules and task assignment.
- A task chat may implement only its assigned scope.
- A task chat must not silently change shared APIs, storage paths, manifest fields or security rules.
- If a conflict or missing requirement is found, stop and report it to MAIN CONTROL.

## 2. Single source of truth

Before coding, read:
1. `README.md`
2. `docs/TECHNICAL_STANDARD.md`
3. This file
4. The task-specific document

If documents conflict, do not guess. Report the conflict. MAIN CONTROL resolves it and updates the documentation.

## 3. Naming and code style

- Files and directories: `snake_case`.
- Classes and types: `PascalCase`.
- Functions and variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- JSON keys: `snake_case`.
- UTF-8 text.
- Four spaces for indentation unless the language formatter requires another standard.
- Use descriptive names. Avoid unexplained abbreviations.
- Do not duplicate shared constants or API definitions.

## 4. Code quality

- Keep modules small and responsibilities separate.
- Prefer explicit interfaces over hidden cross-module dependencies.
- Validate external input, manifests, paths and downloaded data.
- Handle errors; do not silently ignore failures.
- Do not use placeholder implementations in production paths without marking them clearly.
- Do not add dependencies without explaining why they are needed.
- Pin dependency versions. Never use floating versions such as `latest` in release configuration.
- Do not add required CDN dependencies to offline game packages.
- Never put passwords, API keys, signing keys or personal tokens in source control.

## 5. Changes and commits

- Inspect existing files before editing.
- Change only files within the assigned scope.
- Preserve working behavior unless the task explicitly changes it.
- Do not overwrite unknown work.
- Use clear commit messages.
- After changes, inspect the resulting files and diff where available.
- Never claim a test passed unless it was actually run.

## 6. Required task report

Every task chat must report:
- Result: complete / partial / blocked.
- Files created.
- Files changed.
- Files deleted.
- Interfaces or dependencies changed.
- Tests actually run and their results.
- Known issues and remaining work.
- Commit SHA or pull request link, if available.

## 7. Testing

Tests must match the changed code. At minimum:
- Validate syntax/build for the affected module where tooling permits.
- Test success and failure paths for data operations.
- Test offline behavior when network access is involved.
- Test install/update/remove boundaries when package handling is involved.
- Report device, Android version and test method for device tests.

## 8. Security and user data

- Treat downloaded games and catalog metadata as untrusted input.
- Keep game data isolated.
- Never expose unrestricted host filesystem access to game code.
- Do not log secrets or private save data.
- Do not weaken validation to make a test pass.

## 9. Architecture changes

Changes to any of the following require MAIN CONTROL approval:
- Public APIs and SDK methods.
- Game manifest schema.
- Storage layout.
- Runtime or rendering engine.
- Installation and update process.
- Security boundaries.
- Supported Android versions and device requirements.
