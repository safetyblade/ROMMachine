import json, math, unittest, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parent
class DiagonalOneHandDesign(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.s=json.loads((ROOT/'layout-contract.json').read_text())
    def test_global_and_future(self):
        x=self.s
        self.assertEqual(x['navigation']['path'],['Layout','One Hand Mode','Left | Right'])
        self.assertTrue(x['navigation']['oneSelectionForAllGames'])
        self.assertFalse(x['futureProfiles']['perCartridge'])
        self.assertFalse(x['futureProfiles']['genreAuto'])
        self.assertIn('Racing',x['navigation']['futureSiblings'])
    def test_mirrored_diagonal_and_safe_zones(self):
        l=self.s['geometry']['left'];r=self.s['geometry']['right']
        for key in ('dpad','a','b'):
            self.assertEqual(l[key][0]+r[key][0],393)
            self.assertEqual(l[key][1],r[key][1])
        self.assertLess(r['a'][0],r['dpad'][0])
        self.assertLess(r['b'][0],r['a'][0])
        self.assertLess(l['dpad'][0],l['a'][0])
        self.assertLess(l['a'][0],l['b'][0])
        for hand,g in self.s['geometry'].items():
            self.assertLess(g['dpad'][1],g['a'][1])
            self.assertLess(g['a'][1],g['b'][1])
            dx,dy,dw,dh=g['dock']; ux,uy,uw,uh=g['utility']
            self.assertTrue(dx<g['dpad'][0]<dx+dw)
            self.assertTrue(dx<g['a'][0]<dx+dw)
            self.assertTrue(dx<g['b'][0]<dx+dw)
            self.assertTrue(dx+dw<=ux or ux+uw<=dx)
            self.assertLess(g['b'][1]+27,self.s['reference']['bottomGestureReserveRefY'])
    def test_reference_target_clearances(self):
        min_gap=self.s['buttonTouchReference']['minReferenceGapPx']
        for hand,g in self.s['geometry'].items():
            centers={k:g[k] for k in ('dpad','a','b')}
            rects={'Dpad':(centers['dpad'][0]-50,centers['dpad'][1]-50,centers['dpad'][0]+50,centers['dpad'][1]+50)}
            for k in ('a','b'):
                x,y=centers[k];rects[k]=(x-27,y-27,x+27,y+27)
            for i,(x,y,w,h) in enumerate(g['shoulders']):rects[f'shoulder{i}']=(x,y,x+w,y+h)
            ux,uy,_,_=g['utility']
            for i,y in enumerate((553,622,691)):rects[f'util{i}']=(ux+9,y,ux+89,y+50)
            pairs=list(rects.items())
            for i,(name,a) in enumerate(pairs):
                for other,b in pairs[i+1:]:
                    gap=math.hypot(max(0,a[0]-b[2],b[0]-a[2]),max(0,a[1]-b[3],b[1]-a[3]))
                    self.assertGreaterEqual(gap,min_gap,(hand,name,other,gap))
        self.assertGreaterEqual(self.s['buttonTouchReference']['minimumTargetDp'],48)
        self.assertTrue(self.s['buttonTouchReference']['sourcePxNotValidatedDp'])
    def test_six_editable_editions_and_guides(self):
        for hand in ('left','right'):
            for edition in ('original','pink','green'):
                self.assertTrue(ET.parse(ROOT/'review'/f'{hand}-{edition}.svg').getroot().tag.endswith('svg'))
            self.assertTrue((ROOT/'review'/f'{hand}-reach.svg').is_file())
        self.assertTrue(self.s['reference']['isAndroidDp'] is False)
if __name__=='__main__':unittest.main()
