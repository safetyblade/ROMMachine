#!/usr/bin/env python3
import json, math, unittest, xml.etree.ElementTree as ET
from pathlib import Path
R=Path(__file__).resolve().parent
class PivotArcDesignChecks(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.s=json.loads((R/'layout-contract.json').read_text())
    def boxes(self,hand='right'):
        items=self.s['geometry']['rightHandRelativeCenters']
        return {name:(v['x']*(1 if hand=='right' else -1)-v['w']/2,v['y']-v['h']/2,
                 v['x']*(1 if hand=='right' else -1)+v['w']/2,v['y']+v['h']/2) for name,v in items.items()}
    def test_exact_sketch_topology(self):
        c=self.s['geometry']['rightHandRelativeCenters']
        self.assertLess(c['P2']['x'],c['P1']['x'])
        self.assertLess(c['P3']['x'],c['P4']['x'])
        self.assertLess(c['P1']['y'],c['P2']['y'])
        self.assertLess(c['P2']['y'],c['D']['y'])
        self.assertLess(c['D']['y'],c['P3']['y'])
        self.assertLess(c['P3']['y'],c['P4']['y'])
        self.assertGreater(c['R']['x'],c['L']['x'])
        self.assertGreater(c['L']['w'],c['L']['h'])
        self.assertGreater(c['R']['h'],c['R']['w'])
    def test_separation_both_hands(self):
        for hand in ('right','left'):
            boxes=list(self.boxes(hand).items())
            for i,(a,v) in enumerate(boxes):
                for b,w in boxes[i+1:]:
                    gap=math.hypot(max(0,v[0]-w[2],w[0]-v[2]),max(0,v[1]-w[3],w[1]-v[3]))
                    self.assertGreaterEqual(gap,7.99,(hand,a,b,gap))
    def test_minimum_targets_and_extent(self):
        d=self.s
        for k,v in d['geometry']['rightHandRelativeCenters'].items():
            self.assertGreaterEqual(v['w'],48,k);self.assertGreaterEqual(v['h'],48,k)
        ext=d['geometry']['extent'];self.assertEqual(ext['width'],285);self.assertEqual(ext['height'],300)
        self.assertTrue(d['anchor']['safeMarginDp']*2+ext['width'] <= 320)
    def test_global_and_mapping_variants(self):
        s=self.s
        self.assertTrue(s['layoutSelection']['globalAcrossAllGames'])
        self.assertFalse(s['layoutSelection']['savedPerGameOverrides'])
        self.assertFalse(s['layoutSelection']['genreRulesEnabled'])
        self.assertTrue(s['layoutSelection']['restoreStandardUnchanged'])
        for x in ('A','B'):
            m=s['mappingExperiments'][x]['four']
            self.assertEqual(len(set(m.values())),4)
            self.assertEqual(set(m.values()),{'P1','P2','P3','P4'})
    def test_source_svg_files(self):
        for name in ('core-right.svg','core-left.svg','modules.svg','anchor-comparison.svg'):
            path=R/'diagrams'/name
            self.assertTrue(ET.parse(path).getroot().tag.endswith('svg'),name)
        self.assertTrue((R/'prototype.html').is_file())
    def test_touch_ownership_disclaimer(self):
        t=self.s['touchResearch']
        self.assertIn('touch-down ownership',t['default'])
        self.assertIn('Existing PadKit',t['phaseOneDoNotChange'])
if __name__=='__main__':unittest.main()
