import json, math, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parent
class CandidateC(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.s=json.loads((ROOT/'candidate-c-baseline.json').read_text())
 def test_visual_touch_independence(self):
  s=self.s
  for name,b in s['buttonCenters'].items():
   self.assertGreaterEqual(b['touch']['w'],48,name)
   self.assertGreaterEqual(b['touch']['h'],48,name)
   self.assertLessEqual(b['visual']['w'],b['touch']['w'],name)
   self.assertLessEqual(b['visual']['h'],b['touch']['h'],name)
 def test_core_topology(self):
  b=self.s['buttonCenters']
  self.assertLess(b['L']['center']['x'],b['D']['center']['x'])
  self.assertGreater(b['R']['center']['x'],b['A']['center']['x'])
  self.assertLess(b['B']['center']['x'],b['A']['center']['x'])
  self.assertLess(b['B']['center']['y'],b['A']['center']['y'])
  self.assertLess(b['D']['center']['y'],b['A']['center']['y'])
 def test_active_targets_separated_in_reference(self):
  s=self.s;b=s['buttonCenters']
  for profile in ('GBA','SNES','PS1_PSP','DS'):
   a=s['profiles'][profile]
   for i,n in enumerate(a):
    x,y=b[n]['center'].values();w,h=b[n]['touch'].values()
    for n2 in a[i+1:]:
     X,Y=b[n2]['center'].values();W,H=b[n2]['touch'].values()
     gap=math.hypot(max(0,abs(x-X)-(w+W)/2),max(0,abs(y-Y)-(h+H)/2))
     self.assertGreaterEqual(gap,7.99,(profile,n,n2,gap))
 def test_global_and_standard_lock(self):
  s=self.s
  self.assertEqual(s['globalSelection']['scope'],'global for all games')
  self.assertTrue(s['render']['defaultStandardInputBoundsUntouched'])
  self.assertFalse(s['globalSelection']['advancedPerButtonEditingRuntimeImplemented'])
if __name__=='__main__':unittest.main()
