import json,unittest,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parent
class CartridgeFamilyTests(unittest.TestCase):
 def test_one_geometry_and_bounded_art(self):
  m=json.loads((ROOT/"cartridge-family.json").read_text())
  self.assertFalse(m["systemAffectsGeometry"])
  self.assertEqual(set(["GB","GBC","GBA"])-set(m["systems"]),set())
  self.assertEqual(m["approvedEditions"],["original","pink"])
  self.assertEqual(m["geometryLocked"]["front"],[320,390])
  for key in ["front-base","front-overlay","spine-base","system-badge","fallback-atlas","back-base"]:
   p=ROOT/"cartridge"/(key+".svg")
   self.assertTrue(p.exists(),p)
   node=ET.parse(p).getroot()
   self.assertTrue(node.tag.endswith("svg"))
  self.assertNotIn("pokemon",(ROOT/"README.md").read_text().lower())
 def test_local_art_and_explicit_launch(self):
  m=json.loads((ROOT/"cartridge-family.json").read_text())
  self.assertTrue(m["userArtPolicy"]["noBundledCommercialGameArtwork"])
  self.assertTrue(m["userArtPolicy"]["noExternalScraping"])
  self.assertEqual(m["libraryFlow"][0],"sideways shelf spine")
  self.assertIn("explicit Open/Resume",m["libraryFlow"])
  self.assertTrue(m["runtime"]["systemAndTitleAreTextOverlays"])
if __name__=="__main__":unittest.main()
