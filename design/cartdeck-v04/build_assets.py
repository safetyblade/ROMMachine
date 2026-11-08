#!/usr/bin/env python3
"""Build cosmetic SVG previews for all editions; no Android runtime modifications.
Run from repo root: python design/cartdeck-v04/build_assets.py
"""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
V03 = ROOT.parent / "cartdeck-v03"
DEST = ROOT / "generated"
COLOR = re.compile(r"^#[0-9a-fA-F]{6}$")

def main():
    catalog = json.loads((ROOT / "catalog.json").read_text(encoding="utf-8"))
    source = set()
    for group in catalog["assetGroups"].values():
        for p in (group.get("sources", []) if isinstance(group, dict) else group):
            path = (ROOT / p).resolve()
            if not path.exists():
                raise ValueError(f"Missing asset: {p}")
            source.add(path)
    base = json.loads((V03 / "editions/original.json").read_text(encoding="utf-8"))
    editions = sorted((V03 / "editions").glob("*.json"))
    for cfg in editions:
        spec = json.loads(cfg.read_text(encoding="utf-8"))
        for locked in ("schema", "identityVersion", "brand", "presentation", "controls", "systems", "guardrails"):
            if spec.get(locked) != base.get(locked):
                raise ValueError(f"Edition changes locked contract: {cfg.name}:{locked}")
        edition = spec["edition"]
        if edition["id"] != cfg.stem:
            raise ValueError(f"Edition ID mismatch: {cfg.name}")
        for k in catalog["semanticTokens"]:
            if not COLOR.match(edition.get(k, "")):
                raise ValueError(f"Missing or invalid cosmetic token {cfg.name}:{k}")
        out = DEST / cfg.stem
        for p in sorted(source):
            if p.suffix != ".svg":
                continue
            rel = p.relative_to(ROOT) if p.is_relative_to(ROOT) else Path("inherited") / p.name
            dest = out / rel
            dest.parent.mkdir(parents=True,exist_ok=True)
            tree = ET.parse(p)
            geom = tree.getroot()
            for node in geom.iter():
                for token_attr, style_attr in (("data-cd-fill", "fill"), ("data-cd-stroke", "stroke")):
                    key = node.attrib.get(token_attr)
                    if key:
                        if key not in edition:
                            raise ValueError(f"Undeclared token {key} in {p}")
                        node.set(style_attr,edition[key])
            tree.write(dest, encoding="unicode", xml_declaration=False)
        print(f"PASS: {cfg.stem} edition visuals generated ({len(source)} source assets)")
    print("Design source only. Android needs Compose rendering / reviewed VectorDrawable conversion.")

if __name__ == "__main__":
    main()
