#!/usr/bin/env python3
"""Visual-only D-pad regression test. Run: python -m unittest discover design/cartdeck-v03 -p 'test_dpad_consistency.py'"""
from pathlib import Path
import json
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent


def find(node, ident):
    return next((e for e in node.iter() if e.attrib.get("id") == ident), None)


def paths(node):
    return [(e.attrib.get("id"), e.attrib.get("d")) for e in node.iter() if e.tag.endswith("path")]


class DpadConsistency(unittest.TestCase):
    def test_signature_cross_and_no_circular_mount(self):
        doc = ET.fromstring((ROOT / "controls/dpad-master.svg").read_text())
        for part in ("cd-dpad-cross", "cd-dpad-upper-bar", "cd-dpad-lower-bar", "cd-dpad-centre"):
            self.assertIsNotNone(find(doc, part), part)
        circles = [e for e in doc.iter() if e.tag.endswith("circle")]
        self.assertEqual(len(circles), 1)
        self.assertLessEqual(float(circles[0].get("r")), 9)
        self.assertEqual(len(paths(doc)), 3)

    def test_palette_changes_only_colours(self):
        with tempfile.TemporaryDirectory() as temp:
            work = Path(temp)
            shutil.copytree(ROOT / "controls", work / "controls")
            shutil.copytree(ROOT / "editions", work / "editions")
            shutil.copy2(ROOT / "build_theme_assets.py", work / "build_theme_assets.py")
            p = subprocess.run(["python", str(work / "build_theme_assets.py")],
                               capture_output=True, text=True)
            self.assertEqual(p.returncode, 0, p.stdout + p.stderr)
            master = ET.fromstring((work / "controls/dpad-master.svg").read_text())
            results = {}
            for edition in ("original", "pink", "green", "mono"):
                svg = (work / "generated" / edition / "dpad-master.svg").read_text()
                result = ET.fromstring(svg)
                self.assertEqual(paths(master), paths(result), edition)
                self.assertFalse(any(e.tag.endswith("circle") and float(e.get("r", 0)) > 9 for e in result.iter()))
                results[edition] = svg
            self.assertNotEqual(results["original"], results["pink"])
            self.assertNotEqual(results["pink"], results["green"])

    def test_edition_does_not_change_emulator_or_input_contract(self):
        items = [json.loads(p.read_text()) for p in (ROOT / "editions").glob("*.json")]
        original = next(i for i in items if i["edition"]["id"] == "original")
        for item in items:
            for field in ("schema", "identityVersion", "brand", "presentation",
                          "controls", "systems", "guardrails"):
                self.assertEqual(item[field], original[field], field)


if __name__ == "__main__":
    unittest.main()
