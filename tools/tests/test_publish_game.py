import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publish_game


class PublishGameTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.game_dir = self.root / "com.warp.sample"
        self.game_dir.mkdir()
        manifest = {
            "game_id": "com.warp.sample",
            "name": "Sample",
            "description": "A test package",
            "version": "1.0.0",
            "engine": "html5",
            "engine_version": "1",
            "game_type": "2d",
            "entry_point": "index.html",
            "orientation": "landscape",
            "offline_supported": True,
            "minimum_platform_version": "0.2.0",
        }
        (self.game_dir / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")
        (self.game_dir / "index.html").write_text("<html></html>", encoding="utf-8")

    def tearDown(self):
        self.temp.cleanup()

    def test_package_contains_manifest_and_entry_point(self):
        output = self.root / "com.warp.sample-1.0.0.zip"
        publish_game.package_game(
            self.game_dir, output, "com.warp.sample", "1.0.0", None
        )
        with zipfile.ZipFile(output) as archive:
            self.assertEqual(
                set(archive.namelist()),
                {"manifest.json", "index.html"},
            )

    def test_catalog_records_exact_package_integrity(self):
        output = self.root / "com.warp.sample-1.0.0.zip"
        publish_game.package_game(
            self.game_dir, output, "com.warp.sample", "1.0.0", None
        )
        catalog_path = self.root / "catalog.json"
        publish_game.update_catalog(
            catalog_path,
            self.game_dir / "manifest.json",
            output,
            "https://github.com/ded0TM0R0Z/Warp-Game/releases/download/test-tag",
        )
        catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
        game = catalog["games"][0]
        self.assertEqual(game["package_size_bytes"], output.stat().st_size)
        self.assertEqual(game["package_sha256"], publish_game.hash_file(output))
        self.assertTrue(game["package_url"].endswith("/com.warp.sample-1.0.0.zip"))

    def test_rejects_traversal_paths(self):
        with self.assertRaises(SystemExit):
            publish_game.safe_relative_path("../outside.txt")

    def test_rejects_non_offline_game(self):
        manifest_path = self.game_dir / "manifest.json"
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["offline_supported"] = False
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        with self.assertRaises(SystemExit):
            publish_game.read_manifest(self.game_dir, "com.warp.sample", "1.0.0")


if __name__ == "__main__":
    unittest.main()
