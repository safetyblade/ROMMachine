import json, unittest
from pathlib import Path
ROOT=Path(__file__).parent
class OneHandContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.d=json.loads((ROOT/"layout-contract.json").read_text())
    def test_global_mode_and_future_siblings(self):
        a=self.d
        self.assertEqual(a["nav"]["parent"],"Layout")
        self.assertEqual(a["nav"]["item"],"One Hand Mode")
        self.assertIn("Racing",a["nav"]["futureSiblings"])
        self.assertEqual(a["scopeNow"]["selectedLayout"],"GLOBAL for every game")
        self.assertFalse(a["scopeNow"]["perGameOverrides"])
        self.assertFalse(a["scopeNow"]["genreAutomation"])
    def test_primary_cluster_and_standard_lock(self):
        p=self.d["placement"];self.assertTrue(p["standardModeStartSelectLocked"])
        self.assertGreaterEqual(p["minFunctionalHitDp"],48)
        self.assertLess(p["left"]["dock"][0]+p["left"]["dock"][2],p["left"]["utility"][0]+1)
        self.assertLess(p["right"]["utility"][0]+p["right"]["utility"][2],p["right"]["dock"][0]+1)
        for hand in ("left","right"):
            box=p[hand]["dock"];x,y,w,h=box
            coords=[p[hand]["dpadCenter"],*p[hand]["abCenters"].values()]
            for px,py in coords: self.assertTrue(x<=px<=x+w and y<=py<=y+h,(hand,px,py))
            for xx,yy,ww,hh in p[hand]["shoulderVisuals"].values():
                self.assertTrue(x<=xx and xx+ww<=x+w and y<=yy and yy+hh<=y+h,hand)
    def test_system_support_and_no_video_mirror(self):
        self.assertEqual(set(self.d["controlSets"]),{"gb","gbc","gba","snes","ps1","nds"})
        self.assertIn("L",self.d["controlSets"]["gba"])
        self.assertIn("stylus touch mapping",self.d["controlSets"]["nds"])
        self.assertFalse(self.d["display"]["mirrorGameVideo"])
        self.assertEqual(self.d["display"]["rendererCount"],1)
    def test_review_sources(self):
        for f in self.d["previewFiles"]: self.assertTrue((ROOT/f).is_file(),f)
if __name__=="__main__":unittest.main()
