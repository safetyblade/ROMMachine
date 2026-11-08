#!/usr/bin/env python3
"""Export palette-resolved CARTDECK design masters to Android VectorDrawable previews.

This does NOT add assets to Android source sets or modify the emulator.
Run: python design/cartdeck-v04/build_assets.py
     python design/cartdeck-v04/export_android_vectors.py
Use --include-prototypes only to preview the unapproved Green/Mono editions.
"""
from pathlib import Path
import argparse
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
V03 = ROOT.parent / "cartdeck-v03"
ANDROID = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID)

def local(tag):
    return tag.rsplit("}", 1)[-1]

def num(value):
    return f"{float(value):g}"

def rounded_rect(node):
    x,y = (float(node.get(k,"0")) for k in ("x","y"))
    w,h = (float(node.get(k,"0")) for k in ("width","height"))
    r = min(float(node.get("rx","0")),w/2,h/2)
    if not r: return f"M{x:g},{y:g}H{x+w:g}V{y+h:g}H{x:g}Z"
    return f"M{x+r:g},{y:g}H{x+w-r:g}A{r:g},{r:g} 0 0,1 {x+w:g},{y+r:g}V{y+h-r:g}A{r:g},{r:g} 0 0,1 {x+w-r:g},{y+h:g}H{x+r:g}A{r:g},{r:g} 0 0,1 {x:g},{y+h-r:g}V{y+r:g}A{r:g},{r:g} 0 0,1 {x+r:g},{y:g}Z"

def circle(node):
    cx,cy,r=(float(node.get(k,0)) for k in ("cx","cy","r"))
    return f"M{cx+r:g},{cy:g}A{r:g},{r:g} 0 1,1 {cx-r:g},{cy:g}A{r:g},{r:g} 0 1,1 {cx+r:g},{cy:g}Z"

def inherited_styles(node, parent):
    attrs=("fill","stroke","stroke-width","stroke-linecap","stroke-linejoin","stroke-opacity","fill-opacity")
    styles=dict(parent)
    for key in attrs:
        if key in node.attrib:styles[key]=node.attrib[key]
    return styles

def append_shapes(svg,vector,colors):
    count=0
    def walk(node,style):
        nonlocal count
        element=local(node.tag)
        if node.attrib.get("transform"):
            raise ValueError(f"SVG transform needs reviewed native conversion: {node.tag}")
        style=inherited_styles(node,style)
        if element in ("path","rect","circle"):
            data={"path":node.get("d"),"rect":rounded_rect(node),"circle":circle(node)}[element]
            if not data: raise ValueError("Empty path data")
            output=ET.SubElement(vector,f"{{{ANDROID}}}path")
            output.set(f"{{{ANDROID}}}pathData",data)
            for svg_key,android_key in (("fill","fillColor"),("stroke","strokeColor")):
                value=style.get(svg_key,"none")
                if value=="none": value="#00000000"
                elif value=="currentColor":value=colors["accent"]
                if not re.fullmatch(r"#[0-9a-fA-F]{6,8}",value):
                    raise ValueError(f"Unresolved SVG colour: {value}")
                output.set(f"{{{ANDROID}}}{android_key}",value)
            if style.get("stroke-width"): output.set(f"{{{ANDROID}}}strokeWidth",num(style["stroke-width"]))
            if style.get("stroke-linejoin"):output.set(f"{{{ANDROID}}}strokeLineJoin",style["stroke-linejoin"])
            if style.get("stroke-linecap"):output.set(f"{{{ANDROID}}}strokeLineCap",style["stroke-linecap"])
            if style.get("stroke-opacity"):output.set(f"{{{ANDROID}}}strokeAlpha",num(style["stroke-opacity"]))
            if style.get("fill-opacity"):output.set(f"{{{ANDROID}}}fillAlpha",num(style["fill-opacity"]))
            count+=1
        else:
            for child in node:
                if local(child.tag) in ("g","path","rect","circle"):
                    walk(child,style)
    walk(svg,{})
    if not count:raise ValueError("Source contains no convertible geometry")
    return count

def export(src,dest,colors):
    root=ET.parse(src).getroot()
    vb=root.get("viewBox")
    if not vb:raise ValueError(f"Missing SVG viewBox: {src}")
    x,y,w,h=map(float,vb.split())
    if x!=0 or y!=0: raise ValueError("Use viewBox with zero origin for native preview")
    vector=ET.Element("vector")
    for k,v in (("width",f"{w:g}dp"),("height",f"{h:g}dp"),
                ("viewportWidth",f"{w:g}"),("viewportHeight",f"{h:g}")):
        vector.set(f"{{{ANDROID}}}{k}",v)
    count=append_shapes(root,vector,colors)
    dest.parent.mkdir(parents=True,exist_ok=True)
    ET.ElementTree(vector).write(dest,encoding="unicode",xml_declaration=True)
    return count

def main(include_prototypes=False):
    catalog=json.loads((ROOT/"catalog.json").read_text())
    groups=("shell","controls","shelf","boot","navigation")
    items=set()
    for group in groups:
        items.update(x for x in catalog["assetGroups"][group] if not x.endswith("wordmark-master.svg"))
    # Android branded launcher assets already exist. Preserve the exact C-slot
    # as a separate preview; do not auto-convert custom wordmark transforms.
    items.add("../cartdeck-v03/brand/cslot-master.svg")
    editions=sorted((V03/"editions").glob("*.json"))
    total=0
    for f in editions:
        e=json.loads(f.read_text())["edition"]
        if e["status"] != "approved-direction" and not include_prototypes:
            continue
        base=ROOT/"generated"/e["id"]
        for item in sorted(items):
            source=(ROOT/item).resolve()
            rel=(source.relative_to(ROOT) if source.is_relative_to(ROOT)
                 else Path("inherited")/source.name)
            master=base/rel
            if not master.exists():
                raise ValueError(f"Missing edition-resolved SVG: {master}; run build_assets first")
            name="cartdeck_"+e["id"]+"_"+source.stem.replace("-","_")+".xml"
            dest=ROOT/"generated"/"android"/e["id"]/name
            total+=export(master,dest,e)
        print(f"PASS native vector preview: {e['id']}, {len(items)} resource files")
    print(f"Exported {total} Android VectorDrawable shape layers (design preview, not auto-wired).")

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--include-prototypes",action="store_true")
    args=p.parse_args()
    main(args.include_prototypes)
