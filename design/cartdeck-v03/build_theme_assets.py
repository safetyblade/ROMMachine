#!/usr/bin/env python3
"""CARTDECK editable edition artwork builder. Design-only; never patches Android runtime."""
from pathlib import Path
import json, re, sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "controls"
OUT = ROOT / "generated"
LOCKED = ["schema","identityVersion","brand","presentation","controls","systems","guardrails"]

MAP = {
 "dpad-master.svg":{
  "#0C141C":"bezel","#657585":"secondaryHi","#42505F":"secondaryHi",
  "#17212C":"button","#0F1722":"bezel","#111B26":"button",
  "#647487":"secondaryHi","#263440":"secondary","#496273":"accentLo","#5B7A89":"accent"
 },
 "action-a-master.svg":{
  "#70F2E2":"accentHi","#159A96":"accentLo","#101B25":"bezel",
  "#556575":"secondaryHi","#C7FFF3":"onChassis","#14615F":"accentLo","#102F35":"onAccent"
 },
 "action-b-master.svg":{
  "#70F2E2":"accentHi","#159A96":"accentLo","#101B25":"bezel",
  "#556575":"secondaryHi","#C7FFF3":"onChassis","#14615F":"accentLo","#102F35":"onAccent"
 },
}
def lum(c):
 a=[int(c[i:i+2],16)/255 for i in (1,3,5)]
 a=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in a]
 return .2126*a[0]+.7152*a[1]+.0722*a[2]
def ratio(a,b):
 x,y=sorted((lum(a),lum(b)),reverse=True)
 return (x+.05)/(y+.05)
def mix(a,b,p):
 x=[int(a[i:i+2],16) for i in (1,3,5)]
 y=[int(b[i:i+2],16) for i in (1,3,5)]
 return "#"+''.join(f"{round(v*(1-p)+u*p):02X}" for v,u in zip(x,y))
def paths(svg):
 return [p.attrib.get("d") for p in ET.fromstring(svg).iter() if p.tag.endswith("path")]
def main():
 files=sorted((ROOT/"editions").glob("*.json"))
 assert files, "No edition manifests found"
 base=json.loads((ROOT/"editions/original.json").read_text(encoding="utf-8"))
 masters={key:(SRC/key).read_text(encoding="utf-8") for key in MAP}
 for f in files:
  m=json.loads(f.read_text(encoding="utf-8"))
  for key in LOCKED:
   if m.get(key)!=base.get(key):
    raise ValueError(f"{f.name}: locked field changed: {key}")
  e=m["edition"]
  if e["id"]!=f.stem: raise ValueError(f"ID/name mismatch: {f.name}")
  for color in ["chassis","secondary","bezel","accent","onChassis","button","onButton","focus","glass"]:
   if not re.fullmatch(r"#[0-9a-fA-F]{6}",str(e.get(color))):
    raise ValueError(f"{f.name}: invalid {color}")
  for a,b in [("onChassis","chassis"),("onButton","button")]:
   if ratio(e[a],e[b]) < 4.5: raise ValueError(f"{f.name}: {a} vs {b} fails contrast")
  tokens={**e,
   "secondaryHi":mix(e["secondary"],e["onChassis"],.28),
   "accentHi":mix(e["accent"],"#FFFFFF",.21),
   "accentLo":mix(e["accent"],"#000000",.28),
   "onAccent":"#102229" if ratio("#102229",e["accent"])>=ratio("#FFFFFF",e["accent"]) else "#FFFFFF"}
  folder=OUT/e["id"]; folder.mkdir(parents=True,exist_ok=True)
  for name,changes in MAP.items():
   result=masters[name]
   for old,new in changes.items():result=result.replace(old,tokens[new])
   ET.fromstring(result)
   if paths(result)!=paths(masters[name]):raise ValueError(f"{e['id']}: vector path geometry changed")
   (folder/name).write_text(result,encoding="utf-8")
  for arrow in ["arrow-left.svg","arrow-right.svg","arrow-up.svg","arrow-down.svg"]:
   (folder/arrow).write_text((SRC/arrow).read_text(encoding="utf-8"),encoding="utf-8")
  print(f"PASS {e['id']}: {len(MAP)} skinned controls + 4 tintable directional glyphs")
 print("All editions share identical vector paths; only cosmetic colours vary.")
if __name__ == "__main__": main()
