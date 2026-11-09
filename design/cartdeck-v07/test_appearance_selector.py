#!/usr/bin/env python3
"""Design contract tests: python -m unittest discover design/cartdeck-v07 -p 'test_appearance_selector.py' -v"""
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parent
DESIGN=ROOT.parent
def load(p):
    return json.loads(p.read_text(encoding="utf-8"))

class AppearanceSelectorContractTests(unittest.TestCase):
    def test_three_presets_and_shared_identity(self):
        spec=load(ROOT/"appearance-selector.json")
        order=spec["presetOrder"]
        self.assertEqual(order,["original","pink","green"])
        source=[load(DESIGN/"cartdeck-v03"/"editions"/(name+".json")) for name in order]
        keys=("brand","controls","presentation","systems","guardrails")
        for base in source[1:]:
            for k in keys: self.assertEqual(base[k],source[0][k],k)
        self.assertTrue(all(x["edition"]["status"]=="approved-direction" for x in source))
        self.assertEqual(source[2]["edition"]["name"],"Green")

    def test_colors_live_in_shelf_palette(self):
        s=load(DESIGN/"cartdeck-v06"/"bookshelf-palette.json")
        edition_roles=set(s["roles"])
        self.assertEqual(set(s["editions"]),set(("original","pink","green")))
        for name in ("original","pink","green"):
            self.assertEqual(set(s["editions"][name]["tokens"]),edition_roles)
        spec=load(ROOT/"appearance-selector.json")
        self.assertTrue(set(spec["roleGroups"]["shelf"]).issubset(edition_roles))
        self.assertTrue(spec["implementation"]["preserveScope"].startswith("No emulator"))

    def test_curated_real_colour_chips(self):
        spec=load(ROOT/"material-swatches.json")
        selector=load(ROOT/"appearance-selector.json")
        self.assertEqual(selector["layout"]["swatchCatalogue"],"material-swatches.json")
        for key,values in spec["suggested"].items():
            self.assertGreaterEqual(len(values),6,key)
            self.assertEqual(len({chip["hex"] for chip in values}),len(values),key)
            for chip in values:
                self.assertRegex(chip["hex"],r"^#[0-9A-Fa-f]{6}$")
        for category,roles in spec["mapping"].items():
            self.assertTrue(all(source in spec["suggested"] for source in roles.values()))
        self.assertTrue(spec["interaction"]["reset"].startswith("Restore"))

    def test_swatches_custom_and_accessibility(self):
        x=load(ROOT/"appearance-selector.json")
        self.assertIn("swatch",x["layout"]["swatchRoleList"].lower())
        self.assertIn("live",x["layout"]["preview"].lower())
        self.assertTrue(x["customThemes"]["preserveFactories"])
        self.assertTrue(x["customThemes"]["emptyOverridesInheritBase"])
        self.assertGreaterEqual(x["customThemes"]["minUserThemes"],10)
        self.assertEqual(x["accessibility"]["contrastText"],4.5)
        self.assertEqual(x["accessibility"]["contrastInteractive"],3)
        self.assertTrue(x["accessibility"]["highContrastOverridesUserSwatches"])
        self.assertGreaterEqual(x["accessibility"]["minimumTargetDp"],48)
        self.assertTrue(x["implementation"]["migration"].startswith("Keep existing"))

if __name__=="__main__":
    unittest.main()
