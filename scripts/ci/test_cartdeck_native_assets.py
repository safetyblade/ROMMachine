#!/usr/bin/env python3
"""ASSET-H1 checks executable with Python 3.11; Android SDK is not required."""
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "scripts/generate_cartdeck_native_assets.py"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class NativeAssetTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory(prefix="cartdeck-native-")
        cls.output = Path(cls.tmp.name) / "res"
        cls.run_generator()
        cls.manifest_path = cls.output.parent / "native-assets-manifest.json"
        cls.manifest = json.loads(cls.manifest_path.read_text(encoding="utf-8"))

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    @classmethod
    def run_generator(cls):
        subprocess.run(
            [sys.executable, str(GENERATOR), "--output", str(cls.output)],
            cwd=ROOT,
            check=True,
        )

    def test_all_three_factory_editions_and_single_geometry(self):
        self.assertEqual(self.manifest["editions"], ["original", "pink", "green"])
        self.assertTrue(self.manifest["systemsAsLabelsNotGeometry"])
        self.assertTrue(self.manifest["lockedRendererAndPadKit"])
        self.assertIn("Arc-Rail", self.manifest["designFamily"])
        self.assertEqual(self.manifest["cartridgeArtWindow"], {
            "x": 40, "y": 113, "width": 240, "height": 200, "radius": 8
        })
        self.assertGreaterEqual(len(self.manifest["assets"]), 60)
        names = [row["nativeResource"] for row in self.manifest["assets"]]
        self.assertEqual(len(names), len(set(names)))
        self.assertFalse(any("mono" in name for name in names))
        self.assertFalse(any("book_spine" in name or "cover_case" in name for name in names))

    def test_native_vectors_parse_and_factory_geometry_match(self):
        by_master = {}
        for row in self.manifest["assets"]:
            source = ROOT / row["canonicalMaster"]
            self.assertTrue(source.exists(), str(source))
            self.assertEqual(hashlib.sha256(source.read_bytes()).hexdigest(), row["masterSha256"])
            vector = self.output / "drawable-nodpi" / (row["nativeResource"] + ".xml")
            self.assertTrue(vector.exists())
            self.assertEqual(hashlib.sha256(vector.read_bytes()).hexdigest(), row["nativeSha256"])
            root = ET.parse(vector).getroot()
            self.assertEqual(root.tag, "vector")
            self.assertGreater(len(root), 0)
            self.assertTrue(all(node.tag == "path" for node in root))
            self.assertTrue(all(node.get(ANDROID + "pathData") for node in root))
            by_master.setdefault(row["canonicalMaster"], set()).add(row["geometrySha256"])
        self.assertTrue(all(len(signatures) == 1 for signatures in by_master.values()))
        self.assertEqual(len(by_master) * 3, len(self.manifest["assets"]))

    def test_corrected_dpad_has_cross_and_no_mounting_disc(self):
        matches = [r for r in self.manifest["assets"] if r["canonicalMaster"].endswith("/dpad-master.svg")]
        self.assertEqual(len(matches), 3)
        for row in matches:
            p = self.output / "drawable-nodpi" / (row["nativeResource"] + ".xml")
            root = ET.parse(p).getroot()
            self.assertEqual(len(root), 4, "Cross + two direction bars + small centre hub")
        source = (ROOT / "design/cartdeck-v03/controls/dpad-master.svg").read_text(encoding="utf-8")
        self.assertIn('id="cd-dpad-cross"', source)
        self.assertNotIn("mounting-plate", source)

    def test_generated_colours_match_edition_manifests_and_kotlin_refs(self):
        resource_xml = self.output / "values/cartdeck_edition_colors.xml"
        root = ET.parse(resource_xml).getroot()
        resources = {child.attrib["name"]: child.text for child in root}
        self.assertEqual(len(resources), 33)
        for edition in ("original", "pink", "green"):
            canonical = json.loads(
                (ROOT / f"design/cartdeck-v03/editions/{edition}.json").read_text(encoding="utf-8")
            )["edition"]
            for name in ("chassis", "secondary", "bezel", "glass", "button",
                         "accent", "onChassis", "onButton", "focus"):
                snake = re.sub(r"([a-z])([A-Z])", r"\1_\2", name).lower()
                self.assertEqual(resources[f"cartdeck_{edition}_{snake}"], canonical[name].upper())
            self.assertGreaterEqual(self.manifest["focusContrast"][edition]["chassis"], 3)
            self.assertGreaterEqual(self.manifest["focusContrast"][edition]["secondary"], 3)
        kotlin = (
            ROOT / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/"
                   "mobile/shared/compose/ui/CartdeckEditionTokens.kt"
        ).read_text(encoding="utf-8")
        refs = set(re.findall(r"R\.color\.(cartdeck_\w+)", kotlin))
        self.assertEqual(refs, set(resources))
        self.assertIn("resolveCartdeckEdition", kotlin)

    def test_exact_reproduction(self):
        before = {
            p.relative_to(self.output.parent).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in self.output.parent.rglob("*") if p.is_file()
        }
        self.run_generator()
        after = {
            p.relative_to(self.output.parent).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in self.output.parent.rglob("*") if p.is_file()
        }
        self.assertEqual(before, after, "Two clean generations produced different native artifacts")


if __name__ == "__main__":
    unittest.main()
