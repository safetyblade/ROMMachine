#!/usr/bin/env python3
"""Native VectorDrawable preview contract. Requires build_assets + export_android_vectors."""
import re
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parent
ANDROID="{http://schemas.android.com/apk/res/android}"

class AndroidVectorTests(unittest.TestCase):
    def test_approved_editions_have_parseable_native_vectors(self):
        samples={}
        for edition in ("original","pink"):
            folder=ROOT/"generated"/"android"/edition
            self.assertTrue(folder.exists(),f"Run exporter first: {folder}")
            paths=sorted(folder.glob("*.xml"))
            self.assertGreaterEqual(len(paths),20,edition)
            sample={}
            for p in paths:
                node=ET.parse(p).getroot()
                self.assertEqual(node.tag,"vector",p.name)
                self.assertIn(ANDROID+"viewportWidth",node.attrib)
                geom=[]
                for child in node:
                    self.assertEqual(child.tag,"path",p.name)
                    d=child.get(ANDROID+"pathData")
                    self.assertTrue(d,p.name)
                    self.assertRegex(child.get(ANDROID+"fillColor"),r"^#[0-9A-Fa-f]{8}$|^#[0-9A-Fa-f]{6}$")
                    self.assertRegex(child.get(ANDROID+"strokeColor"),r"^#[0-9A-Fa-f]{8}$|^#[0-9A-Fa-f]{6}$")
                    geom.append(d)
                self.assertTrue(geom,p.name)
                sample[p.name.replace(edition+"_","")]=geom
            samples[edition]=sample
        self.assertEqual(samples["original"],samples["pink"],
                         "Edition changes must never alter native path geometry")

    def test_inherited_core_dpad_still_has_no_ring(self):
        p=ROOT/"generated"/"android"/"original"/"cartdeck_original_dpad_master.xml"
        doc=ET.parse(p).getroot()
        self.assertEqual(len(doc),4,
                         "Corrected cross, upper/lower inlays, tiny hub; no circular plate")

if __name__=="__main__":
    unittest.main()
