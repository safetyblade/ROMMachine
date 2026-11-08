import json
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parent

class V052SpineContract(unittest.TestCase):
    def test_universal_geometry_and_title_zone(self):
        svg=ET.parse(ROOT/"cartridge/spine-base.svg").getroot()
        self.assertEqual(svg.attrib["viewBox"],"0 0 94 340")
        nodes={x.get("id"):x for x in svg if x.get("id")}
        self.assertEqual(nodes["cd-spine-outer"].get("d"),"M22 5H72L90 23V317L72 335H22L4 317V23Z")
        self.assertEqual((nodes["cd-spine-label-window"].get("x"),nodes["cd-spine-label-window"].get("y"),
                          nodes["cd-spine-label-window"].get("width"),nodes["cd-spine-label-window"].get("height")),
                         ("24","82","46","171"))
        self.assertEqual([nodes[k].get("x") for k in
              ("cd-spine-marker-one","cd-spine-marker-two","cd-spine-marker-three")],["25","41","57"])
        self.assertNotIn("cd-baked-system-badge",nodes)
    def test_editable_palette_and_edition_independent_layers(self):
        manifest=json.loads((ROOT/"shelf-presentation-v052.json").read_text())
        original=json.loads((ROOT.parent/"cartdeck-v03/editions/original.json").read_text())["edition"]
        permitted=set(original)-{"id","name","status"}
        for p in (ROOT/"cartridge/spine-base.svg",ROOT/"cartridge/shelf-rail.svg"):
            node=ET.parse(p).getroot()
            for item in node.iter():
                for key in ("data-cd-fill","data-cd-stroke"):
                    if key in item.attrib: self.assertIn(item.attrib[key],permitted)
        self.assertTrue(manifest["appearance"]["editionGeometryIdentical"])
        self.assertEqual(manifest["artwork"]["codeApi"],"game.displayCoverUrl()")
        self.assertFalse(manifest["layout"]["scaleBodyWithFontScale"])
        self.assertEqual(manifest["home"]["tagline"],"none")
        self.assertGreaterEqual(manifest["layout"]["minimumTouchWidthDp"],48)
if __name__=="__main__":
    unittest.main()
