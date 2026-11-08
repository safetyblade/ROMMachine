import json
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def lum(value):
    rgb = [int(value[i:i+2], 16)/255 for i in (1,3,5)]
    ch = [x/12.92 if x <= 0.04045 else ((x+0.055)/1.055)**2.4 for x in rgb]
    return 0.2126*ch[0] + 0.7152*ch[1] + 0.0722*ch[2]

def contrast(a, b):
    high, low = sorted((lum(a), lum(b)), reverse=True)
    return (high+0.05)/(low+0.05)

class CartridgeFamilyTests(unittest.TestCase):
    def setUp(self):
        self.family = json.loads((ROOT/"cartridge-family.json").read_text())

    def test_one_geometry_and_bounded_art(self):
        m = self.family
        self.assertFalse(m["systemAffectsGeometry"])
        self.assertTrue({"GB","GBC","GBA"}.issubset(set(m["systems"])))
        self.assertEqual(m["approvedEditions"], ["original","pink"])
        self.assertEqual(m["geometryLocked"]["front"], [320,390])
        self.assertEqual(m["geometryLocked"]["spine"], [94,340])
        self.assertEqual(m["geometryLocked"]["artWindow"], {"x":40,"y":113,"width":240,"height":200,"radius":8})
        self.assertEqual(m["geometryLocked"]["badgeInSpine"], {"x":13,"y":273,"width":68,"height":30})
        for key in ("front-base","front-overlay","spine-base","system-badge","fallback-atlas","back-base"):
            p = ROOT/"cartridge"/(key+".svg")
            self.assertTrue(p.exists(),p)
            self.assertTrue(ET.parse(p).getroot().tag.endswith("svg"))
        self.assertNotIn("pokemon", (ROOT/"README.md").read_text().lower())

    def test_badge_is_one_runtime_component_not_double_drawn(self):
        front = ET.parse(ROOT/"cartridge/front-overlay.svg").getroot()
        spine = ET.parse(ROOT/"cartridge/spine-base.svg").getroot()
        badge = ET.parse(ROOT/"cartridge/system-badge.svg").getroot()
        def has_baked_badge(root):
            return any(node.tag.endswith("rect") and node.get("x") in ("215","17") and node.get("y") in ("330","271") for node in root.iter())
        self.assertFalse(has_baked_badge(front))
        self.assertFalse(has_baked_badge(spine))
        badge_border = next(n for n in badge.iter() if n.tag.endswith("rect"))
        self.assertEqual(badge_border.get("data-cd-stroke"), "focus")
        art_border = next(n for n in front.iter() if n.tag.endswith("rect") and n.get("x")=="40")
        self.assertEqual(art_border.get("data-cd-stroke"), "focus")
        self.assertEqual(self.family["runtime"]["badgeTextColourToken"],"onButton")
        self.assertEqual(self.family["runtime"]["shellSelectionOutlineToken"],"onChassis")

    def test_cosmetic_palette_safe_label_contrast(self):
        for edition in ("original","pink"):
            p = ROOT.parent/"cartdeck-v03"/"editions"/(edition+".json")
            c = json.loads(p.read_text())["edition"]
            with self.subTest(edition=edition):
                self.assertGreaterEqual(contrast(c["onChassis"],c["chassis"]),4.5)
                self.assertGreaterEqual(contrast(c["onButton"],c["bezel"]),4.5)
                self.assertGreaterEqual(contrast(c["focus"],c["bezel"]),3.0)
                self.assertGreaterEqual(contrast(c["onChassis"],c["chassis"]),3.0)
        self.assertEqual(self.family["runtime"]["coverFitDefault"], "contain")

    def test_local_art_and_explicit_launch(self):
        m = self.family
        self.assertTrue(m["userArtPolicy"]["noBundledCommercialGameArtwork"])
        self.assertTrue(m["userArtPolicy"]["noExternalScraping"])
        self.assertEqual(m["libraryFlow"][0],"sideways shelf spine")
        self.assertIn("explicit Open/Resume",m["libraryFlow"])
        self.assertTrue(m["runtime"]["systemAndTitleAreTextOverlays"])
        self.assertTrue(m["runtime"]["allGamesUseIdenticalCartridgeGeometry"])
        self.assertIn("approval-pending", m["state"])

if __name__=="__main__":
    unittest.main()
