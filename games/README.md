# Game source packages

Each published game lives in its own directory named exactly after its permanent `game_id`.

Example:
```text
games/com.warp.example/
├── manifest.json
├── index.html
├── scripts/
├── styles/
├── assets/
└── libraries/
```

Start from `games/template/manifest.json`, then replace every example value. Keep all runtime libraries and assets inside the package; published games must start offline.

To publish, merge the game source into `main`, then run the GitHub Actions workflow **Publish Game Package** with the exact manifest `game_id` and `version`.

The workflow builds the ZIP, checks its paths and limits, uploads it to a versioned GitHub Release, calculates SHA-256 and size, and updates the remote catalog. Do not add game ZIP files or engine bundles to the Android app's `src/main/assets` directory.
