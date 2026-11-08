#!/usr/bin/env python3
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parent

class CartdeckAssetContractTests(unittest.TestCase):
    def test_catalog_and_editable_vectors(self):
        c=json.loads((ROOT/"catalog.json").read_text())
        self.assertTrue(c["rendererBoundary"]["nativeHitZonesFromPadKit"])
        required={"controls","shell","shelf","boot","navigation","brand"}
        self.assertEqual(set(c["assetGroups"]), required)
        for group in c["assetGroups"].values():
            for item in (group.get("sources",[]) if isinstance(group,dict) else group):
                p=(ROOT/item).resolve()
                self.assertTrue(p.exists(), item)
                if p.suffix==".svg":
                    doc=ET.parse(p).getroot()
                    self.assertTrue(doc.tag.endswith("svg"))
        self.assertIn("controls/button-face.svg",c["assetGroups"]["controls"])
    def test_single_viewport_family(self):
        x=json.loads((ROOT/"system-viewports.json").read_text())
        self.assertTrue(x["shell"]["sameDeviceForAllSystems"])
        self.assertEqual(x["systems"]["PSX"],"fourFaceShoulder")
        self.assertEqual(x["systems"]["PSXDualShock"],"fourFaceDualAnalog")
        self.assertTrue(x["presentationModes"]["ps1InHandheldShellSupported"])
        self.assertEqual(x["screen"]["fit"],"contain")
        self.assertFalse(x["screen"]["allowCrop"])
        self.assertEqual(x["screen"]["preferredFallbacks"]["gb"],[160,144])
        self.assertEqual(x["screen"]["preferredFallbacks"]["gba"],[240,160])
    def test_spine_to_cover_to_open(self):
        x=json.loads((ROOT/"shelf-journey.json").read_text())
        self.assertEqual(x["sequence"][0],"spine-shelf")
        self.assertEqual(x["sequence"][1],"cover-detail")
        self.assertTrue(x["selectedGameDoesNotLaunchImmediately"])
        self.assertTrue(x["coverEditor"]["localOnly"])
    def test_personal_edition_editor_cannot_change_identity_or_emulation(self):
        e=json.loads((ROOT/"appearance-editor.json").read_text())
        manifest=json.loads((ROOT.parent/"cartdeck-v03"/"editions/original.json").read_text())
        self.assertTrue(e["architecture"]["userOverridesAreCosmeticOnly"])
        self.assertTrue(e["architecture"]["neverModifyGameAssets"])
        self.assertEqual(e["defaultPreset"],"original")
        self.assertIn("ps1",e["preview"][3].lower())
        self.assertEqual(set(e["editable"]),set(manifest["edition"])-{"id","name","status"})
        self.assertGreaterEqual(e["accessibility"]["textContrastMinimum"],4.5)
        self.assertGreaterEqual(e["accessibility"]["minimumTouchDp"],48)
        self.assertIn("save data and file paths",e["locked"])

    def test_canonical_brand_and_dpad_not_duplicated(self):
        d=ROOT.parent/"cartdeck-v03"/"controls/dpad-master.svg"
        self.assertIn('id="cd-dpad-cross"',d.read_text())
        self.assertFalse((ROOT/"controls/dpad-master.svg").exists())
        self.assertFalse((ROOT/"brand/cslot-master.svg").exists())

if __name__=="__main__":
    unittest.main()
