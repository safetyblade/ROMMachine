#!/usr/bin/env python3
"""Run: python -m unittest discover design/cartdeck-v06 -p 'test_bookshelf_palette.py'"""
import json, re, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parent

def brightness(code):
    rgb=[int(code[i:i+2],16)/255 for i in (1,3,5)]
    linear=[v/12.92 if v<=0.04045 else ((v+0.055)/1.055)**2.4 for v in rgb]
    return .2126*linear[0]+.7152*linear[1]+.0722*linear[2]

def ratio(a,b):
    x,y=sorted([brightness(a),brightness(b)],reverse=True)
    return (x+.05)/(y+.05)

class BookshelfPaletteTests(unittest.TestCase):
    def test_common_roles_and_locked_navigation(self):
        d=json.loads((ROOT/"bookshelf-palette.json").read_text())
        self.assertEqual(d["geometry"]["browse"],"vertical-scroll-only")
        self.assertTrue(d["geometry"]["sectionsAndEmptySlots"])
        self.assertTrue(d["geometry"]["favouritesAndSearchSeparate"])
        self.assertEqual(set(d["editions"]),{"original","pink","green"})
        roles=set(d["roles"])
        for edition,item in d["editions"].items():
            values=item["tokens"]
            self.assertEqual(set(values),roles,edition)
            for key,value in values.items():
                self.assertRegex(value,r"^#[0-9A-Fa-f]{6}$",(edition,key))

    def test_section_text_drag_and_focus_contrast(self):
        d=json.loads((ROOT/"bookshelf-palette.json").read_text())
        for edition,item in d["editions"].items():
            t=item["tokens"]
            with self.subTest(edition=edition):
                self.assertGreaterEqual(ratio(t["sectionText"],t["shelfBackboard"]),4.5)
                self.assertGreaterEqual(ratio(t["focusOutline"],t["shelfBackboard"]),3.0)
                self.assertGreaterEqual(ratio(t["focusOutline"],t["vacantCell"]),3.0)
                self.assertGreaterEqual(ratio(t["interactionAccent"],t["shelfBackboard"]),3.0)

    def test_cosmetic_only(self):
        d=json.loads((ROOT/"bookshelf-palette.json").read_text())
        b=d["nativeRendering"]
        self.assertTrue(b["noUserCoverArtTint"])
        self.assertTrue(b["noGameViewportChanges"])
        self.assertTrue(b["sameGeometryAcrossEditions"])
        self.assertTrue(b["onCustomTheme"].startswith("Derive"))
        self.assertTrue(b["useComposeSemanticTokens"])
        self.assertGreaterEqual(b["touchTargetMinimumDp"],48)
if __name__=="__main__":
    unittest.main()
