import json
import unittest
import xml.etree.ElementTree as ET
from fractions import Fraction
from pathlib import Path
ROOT=Path(__file__).resolve().parent

class P1BShellContract(unittest.TestCase):
    def setUp(self):
        self.spec=json.loads((ROOT/"console-shell-stage1.json").read_text())

    def test_game_video_ratios_and_identity(self):
        g=self.spec["geometry"]
        self.assertEqual(Fraction(*g["gba"]["native"]),Fraction(3,2))
        self.assertEqual(Fraction(*g["gb"]["native"]),Fraction(10,9))
        for key,target in (("gba",Fraction(3,2)),("gb",Fraction(10,9))):
            _,_,w,h=g[key]["previewGameRect"]
            self.assertEqual(Fraction(str(w))/Fraction(str(h)),target)
        self.assertTrue(g["sameChassisAllSystems"])
        self.assertTrue(self.spec["editionGeometryInvariant"])
        self.assertTrue(self.spec["glBoundary"]["rendererCount"]==1)
        self.assertTrue(self.spec["glBoundary"]["viewportRectsMeasuredNotHardcoded"])

    def test_transparent_video_hole(self):
        root=ET.parse(ROOT/"assets/console-bezel-depth-overlay.svg").getroot()
        self.assertEqual(root.get("viewBox"),"0 0 393 852")
        shapes=[e for e in root if e.tag.endswith("path")]
        self.assertTrue(any(x.get("fill-rule")=="evenodd" and "M37 141H356V379H37Z" in x.get("d","") for x in shapes))
        self.assertTrue(all(not (e.tag.endswith("rect") and e.get("fill") not in (None,"none")) for e in root))

    def test_button_states_have_no_input_semantics(self):
        for state in ("normal","pressed","focused","disabled"):
            p=ROOT/"assets"/f"face-button-{state}.svg"
            root=ET.parse(p).getroot()
            self.assertIn("128 128",root.get("viewBox"))
            self.assertFalse(any(e.tag.endswith("text") for e in root.iter()))
        self.assertEqual(self.spec["controls"]["pressedTapDelayMs"],0)
        self.assertTrue(self.spec["controls"]["touchHitZonesIndependentOfArtwork"] if "touchHitZonesIndependentOfArtwork" in self.spec["controls"] else self.spec["glBoundary"]["compositeWithoutInterceptingTouch"])

    def test_canonical_controls_and_future_modes(self):
        base=ROOT.parent
        for key,path in self.spec["approvedSourceMasters"].items():
            self.assertTrue((ROOT/path).resolve().exists(),(key,path))
        self.assertEqual(self.spec["geometry"]["gba"]["controls"],["dpad","A","B","L","R","start","select","menu"])
        self.assertEqual(self.spec["geometry"]["gb"]["controls"],["dpad","A","B","start","select","menu"])
        self.assertIn("core reported",self.spec["geometry"]["psx"]["ratio"])
        self.assertGreaterEqual(self.spec["responsiveness"]["minTouchTargetDp"],48)

if __name__=="__main__":
    unittest.main()
