#!/usr/bin/env python3
"""Build and publish a WARP GAME ZIP package and its catalog metadata."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path, PurePosixPath

GAME_ID_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{2,127}\\Z")
VERSION_RE = re.compile(r"\\d+\\.\\d+\\.\\d+\\Z")
SHA_RE = re.compile(r"[A-Fa-f0-9]{64}\\Z")
ALLOWED_ENGINES = {"html5", "phaser", "babylonjs", "custom"}
MAX_PACKAGE_BYTES = 256 * 1024 * 1024
MAX_EXPANDED_BYTES = 512 * 1024 * 1024
MAX_FILE_BYTES = 128 * 1024 * 1024
MAX_ENTRIES = 10_000
MAX_MANIFEST_BYTES = 64 * 1024
EXCLUDED_PARTS = {".git", ".idea", "__pycache__", ".DS_Store"}


def fail(message: str) -> None:
    raise SystemExit(message)


def read_manifest(game_dir: Path, expected_id: str, expected_version: str) -> dict:
    manifest_path = game_dir / "manifest.json"
    if not manifest_path.is_file():
        fail(f"Missing manifest: {manifest_path}")
    if manifest_path.stat().st_size > MAX_MANIFEST_BYTES:
        fail("manifest.json exceeds 64 KiB")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        fail(f"manifest.json is invalid: {error}")
    if not isinstance(manifest, dict):
        fail("manifest.json must contain a JSON object")

    required = (
        "game_id", "name", "description", "version", "engine",
        "engine_version", "game_type", "entry_point", "orientation",
        "minimum_platform_version",
    )
    for key in required:
        if not isinstance(manifest.get(key), str) or not manifest[key].strip():
            fail(f"Manifest field '{key}' must be a non-empty string")

    if not GAME_ID_RE.fullmatch(manifest["game_id"]):
        fail("Invalid manifest game_id")
    if manifest["game_id"] != expected_id:
        fail("Dispatch game_id does not match manifest game_id")
    if not VERSION_RE.fullmatch(manifest["version"]):
        fail("Game version must use MAJOR.MINOR.PATCH")
    if manifest["version"] != expected_version:
        fail("Dispatch version does not match manifest version")
    if manifest["engine"] not in ALLOWED_ENGINES:
        fail("Unsupported engine. Use html5, phaser, babylonjs or custom.")
    if manifest["game_type"] not in {"2d", "3d"}:
        fail("game_type must be 2d or 3d")
    if manifest["offline_supported"] is not True:
        fail("Published games must support offline startup")
    if not VERSION_RE.fullmatch(manifest["minimum_platform_version"]):
        fail("minimum_platform_version must use MAJOR.MINOR.PATCH")
    if len(manifest["name"]) > 80 or len(manifest["description"]) > 500:
        fail("Game name or description exceeds platform limits")

    entry_point = manifest["entry_point"]
    safe_relative_path(entry_point)
    if not (game_dir / Path(*PurePosixPath(entry_point).parts)).is_file():
        fail(f"Entry point does not exist: {entry_point}")
    return manifest


def safe_relative_path(value: str) -> PurePosixPath:
    path = PurePosixPath(value)
    if (
        not value
        or path.is_absolute()
        or "\\" in value
        or ":" in value
        or any(part in {"", ".", ".."} for part in value.split("/"))
        or any(ord(char) < 32 for char in value)
        or len(value) > 1024
        or any(len(part) > 240 for part in path.parts)
    ):
        fail(f"Unsafe package path: {value!r}")
    return path


def package_game(game_dir: Path, output: Path, game_id: str, version: str, github_output: str | None) -> None:
    game_dir = game_dir.resolve()
    if not game_dir.is_dir():
        fail(f"Game source directory not found: {game_dir}")
    manifest = read_manifest(game_dir, game_id, version)

    files: list[tuple[Path, str]] = []
    expanded_total = 0
    for path in sorted(game_dir.rglob("*")):
        relative = path.relative_to(game_dir)
        if any(part in EXCLUDED_PARTS or part.startswith(".") for part in relative.parts):
            continue
        if path.is_symlink():
            fail(f"Symbolic links are not allowed in game packages: {relative}")
        if path.is_dir():
            continue
        if not path.is_file():
            fail(f"Unsupported filesystem entry: {relative}")
        archive_name = relative.as_posix()
        safe_relative_path(archive_name)
        size = path.stat().st_size
        if size > MAX_FILE_BYTES:
            fail(f"File exceeds 128 MiB: {archive_name}")
        expanded_total += size
        if expanded_total > MAX_EXPANDED_BYTES:
            fail("Expanded game package exceeds 512 MiB")
        files.append((path, archive_name))
        if len(files) > MAX_ENTRIES:
            fail("Game package exceeds 10,000 files")

    if not any(name == "manifest.json" for _, name in files):
        fail("manifest.json must be at the package root")
    if not any(name == manifest["entry_point"] for _, name in files):
        fail("The manifest entry point is not included in the package")

    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for source, archive_name in files:
            archive.write(source, archive_name)

    size = output.stat().st_size
    if size <= 0 or size > MAX_PACKAGE_BYTES:
        output.unlink(missing_ok=True)
        fail("Compressed package must be between 1 byte and 256 MiB")

    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    filename = f"{game_id}-{version}.zip"
    if output.name != filename:
        final_path = output.with_name(filename)
        output.replace(final_path)
        output = final_path

    print(json.dumps({"filename": filename, "sha256": digest, "size": size}))
    if github_output:
        with open(github_output, "a", encoding="utf-8") as stream:
            stream.write(f"filename={filename}\\n")
            stream.write(f"sha256={digest}\\n")
            stream.write(f"size={size}\\n")


def update_catalog(catalog_path: Path, manifest_path: Path, package_path: Path, release_url: str) -> None:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if not isinstance(manifest, dict):
        fail("Game manifest must be a JSON object")
    if not GAME_ID_RE.fullmatch(str(manifest.get("game_id", ""))):
        fail("Invalid game_id in manifest")
    if not VERSION_RE.fullmatch(str(manifest.get("version", ""))):
        fail("Invalid game version in manifest")

    package_size = package_path.stat().st_size
    digest = hashlib.sha256(package_path.read_bytes()).hexdigest()
    record = {
        "game_id": manifest["game_id"],
        "name": manifest["name"],
        "description": manifest["description"],
        "version": manifest["version"],
        "engine": manifest["engine"],
        "engine_version": manifest["engine_version"],
        "game_type": manifest["game_type"],
        "orientation": manifest["orientation"],
        "package_url": f"{release_url.rstrip('/')}/{package_path.name}",
        "package_sha256": digest,
        "package_size_bytes": package_size,
        "minimum_platform_version": manifest["minimum_platform_version"],
        "offline_supported": True,
    }

    catalog_path.parent.mkdir(parents=True, exist_ok=True)
    if catalog_path.exists():
        catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    else:
        catalog = {"catalog_version": 1, "games": []}
    if not isinstance(catalog, dict) or catalog.get("catalog_version") != 1:
        fail("Unsupported catalog format")
    games = catalog.get("games")
    if not isinstance(games, list):
        fail("Catalog games must be a list")
    games = [game for game in games if isinstance(game, dict) and game.get("game_id") != record["game_id"]]
    games.append(record)
    catalog["games"] = sorted(games, key=lambda game: game["name"].casefold())

    temporary = catalog_path.with_suffix(catalog_path.suffix + ".tmp")
    temporary.write_text(json.dumps(catalog, ensure_ascii=False, indent=4) + "\\n", encoding="utf-8")
    temporary.replace(catalog_path)
    print(f"Catalog updated: {record['game_id']} {record['version']}")


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    package_parser = subparsers.add_parser("package")
    package_parser.add_argument("--game-dir", required=True, type=Path)
    package_parser.add_argument("--output", required=True, type=Path)
    package_parser.add_argument("--game-id", required=True)
    package_parser.add_argument("--version", required=True)
    package_parser.add_argument("--github-output")

    catalog_parser = subparsers.add_parser("update-catalog")
    catalog_parser.add_argument("--catalog", required=True, type=Path)
    catalog_parser.add_argument("--manifest", required=True, type=Path)
    catalog_parser.add_argument("--package", required=True, type=Path)
    catalog_parser.add_argument("--release-url", required=True)

    args = parser.parse_args()
    if args.command == "package":
        if not GAME_ID_RE.fullmatch(args.game_id) or not VERSION_RE.fullmatch(args.version):
            fail("Invalid game_id or version input")
        package_game(args.game_dir, args.output, args.game_id, args.version, args.github_output)
    elif args.command == "update-catalog":
        update_catalog(args.catalog, args.manifest, args.package, args.release_url)


if __name__ == "__main__":
    main()
