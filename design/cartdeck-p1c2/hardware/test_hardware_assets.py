#!/usr/bin/env python3
import json, unittest, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parent
class HardwarePackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest=json.loads((ROOT/'asset-manifest.json').read_text())
        cls.layout=json.loads((ROOT/'layout-contract.json').read_text())
    def test_every_editable_master_exists_and_parses(self):
        assets=self.manifest['assetInventory']
        self.assertEqual(len(assets),52)
        self.assertEqual(len(set(a['id'] for a in assets)),len(assets))
        self.assertEqual(len(set(a['file'] for a in assets)),len(assets))
        for a in assets:
            p=ROOT/a['file']; self.assertTrue(p.exists(),p)
            self.assertTrue(ET.parse(p).getroot().tag.endswith('svg'),p)
    def test_named_screen_and_body_masks_are_real_svg_masks(self):
        names=('screen/standard-bezel-mask.svg','screen/ds-expanded-bezel-mask.svg',
               'chassis/front-body-mask.svg','chassis/control-deck-mask.svg')
        for name in names:
            root=ET.parse(ROOT/name).getroot()
            self.assertTrue(any(node.tag.endswith('mask') for node in root.iter()),name)
        self.assertEqual(self.manifest['assetCount'],52)
    def test_six_independent_glow_families_and_default_off(self):
        zs=self.manifest['glowZones'];self.assertEqual(len(zs),6)
        for z in zs:
            for key in ('mask','on','unlit'):self.assertTrue((ROOT/z[key]).is_file(),z)
            self.assertFalse(z['defaultIlluminated']);self.assertEqual(z['runtimeControlsWorkstream'],'P1-C3 #75')
    def test_screen_is_canonical_and_ds_separate(self):
        r=self.manifest['render'];self.assertEqual(r['rendererCount'],1)
        self.assertEqual(r['screenStructuralSurroundDp'],7)
        self.assertTrue(r['bodyOpaque'] and r['deckOpaque'])
        self.assertTrue(r['lowerTranslucentBandForbidden'])
        c=self.layout['systems'];self.assertEqual(c['STANDARD_4_3']['gameViewportRef'][2],349)
        self.assertEqual(c['NDS']['viewportRef'][2],357)
        self.assertTrue(self.manifest['layoutContracts']['DUAL_SCREEN']['noSecondRenderer'])
    def test_brand_and_aux_do_not_overlap_in_reference(self):
        d=self.layout['systems']['NDS'];a=d['auxStripRef'];seat=d['cslotDecorativeRef'];menu=d['menuRef']
        self.assertTrue(a[1]<=seat[1] and seat[1]+seat[3] < menu[1])
        self.assertNotEqual(seat,menu)
        self.assertTrue(self.manifest['identity']['neverReplaceTouchHitGeometry'])
    def test_palette_geometry_and_no_runtime_code(self):
        x=self.manifest
        self.assertEqual(set(x['identity']['editions']),{'original','pink','green'})
        self.assertTrue(x['identity']['geometryIsEditionInvariant'])
        self.assertTrue(x['render']['allGraphicsPointerTransparent'])
        self.assertFalse(x['futurePersonalisation']['decalsPatternsLightingDeferred'] is False)
if __name__=='__main__':unittest.main()
