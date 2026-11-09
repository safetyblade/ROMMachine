#!/usr/bin/env python3
"""ASSET-H1: generate Android VectorDrawables and colours from Sienna's editable masters.

No ROM, emulator, PadKit, game display or existing UI code is modified here.
All three factory editions ship as resources. Source SVGs and JSON remain canonical.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
V03 = ROOT / "design/cartdeck-v03"
V04 = ROOT / "design/cartdeck-v04"
V05 = ROOT / "design/cartdeck-v05"
EDITIONS = ("original", "pink", "green")
TOKEN_KEYS = ("chassis", "secondary", "bezel", "glass", "button", "accent",
              "onChassis", "onButton", "focus")
GROUPS = ("shell", "controls", "shelf", "boot", "navigation")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_python(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load generator: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def luminance(value: str) -> float:
    parts = [int(value[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in parts]
    return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]


def contrast(a: str, b: str) -> float:
    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def safe_focus(base: dict[str, str], surface: str) -> str:
    # Preserve the canonical 'focus' token and derive an accessible colour for
    # its actual surface; Pink on a light chassis needs a dark outline.
    for candidate in (base["focus"], base["onChassis"], base["bezel"]):
        if contrast(candidate, base[surface]) >= 3.0:
            return candidate
    raise ValueError(f"No accessible focus against {surface}")


def xml_resource_name(edition: str, token: str) -> str:
    snake = re.sub(r"([a-z])([A-Z])", r"\1_\2", token).lower()
    return f"cartdeck_{edition}_{snake}"


def palette() -> dict[str, dict[str, str]]:
    values = {}
    for edition in EDITIONS:
        path = V03 / f"editions/{edition}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        if data["edition"]["status"] != "approved-direction":
            raise ValueError(f"Edition is not approved-direction: {edition}")
        obj = data["edition"]
        fields = {k: obj[k] for k in TOKEN_KEYS}
        if any(not re.fullmatch(r"#[0-9a-fA-F]{6}", value) for value in fields.values()):
            raise ValueError(f"Malformed palette: {edition}")
        if contrast(fields["chassis"], fields["onChassis"]) < 4.5:
            raise ValueError(f"Shell text fails 4.5:1: {edition}")
        if contrast(fields["button"], fields["onButton"]) < 4.5:
            raise ValueError(f"Button text fails 4.5:1: {edition}")
        fields["focusOnChassis"] = safe_focus(fields, "chassis")
        fields["focusOnSecondary"] = safe_focus(fields, "secondary")
        values[edition] = fields
    return values


def check_cartridge_contract() -> dict:
    spec = json.loads((V05 / "cartridge-family.json").read_text(encoding="utf-8"))
    if spec["systemAffectsGeometry"] or spec["approvedEditions"] != list(EDITIONS):
        raise ValueError("Cartridge family no longer matches the common Original/Pink identity")
    if set(("GB", "GBC", "GBA")) - set(spec["systems"]):
        raise ValueError("GB/GBC/GBA labels missing")
    if spec["geometryLocked"]["artWindow"] != {
        "x": 40, "y": 113, "width": 240, "height": 200, "radius": 8
    }:
        raise ValueError("Arc-Rail clipping contract changed; review native cover bounds")
    return spec


def write_colors(out: Path, values: dict[str, dict[str, str]]) -> None:
    target = out / "values/cartdeck_edition_colors.xml"
    target.parent.mkdir(parents=True, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for edition in EDITIONS:
        for key, color in values[edition].items():
            lines.append(f'    <color name="{xml_resource_name(edition, key)}">{color.upper()}</color>')
    lines.append("</resources>")
    target.write_text("\n".join(lines) + "\n", encoding="utf-8")


def source_path(source_rel: str) -> tuple[Path, Path]:
    master = (V04 / source_rel).resolve()
    if not master.is_file():
        raise FileNotFoundError(master)
    # Matching the existing v0.4 edition builder's relative output naming.
    rel = master.relative_to(V04) if master.is_relative_to(V04) else Path("inherited") / master.name
    return master, rel


def geometry_signature(vector_xml: Path) -> str:
    android = "{http://schemas.android.com/apk/res/android}"
    doc = ET.parse(vector_xml).getroot()
    if doc.tag != "vector" or len(doc) == 0:
        raise ValueError(f"Invalid Android vector: {vector_xml}")
    fields = [(p.get(android + "pathData") or "") for p in doc]
    if not all(fields):
        raise ValueError(f"Android vector missing path data: {vector_xml}")
    return digest(json.dumps(fields, separators=(",", ":")).encode("utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True,
                        help="Gradle build/generated/cartdeck/res source directory")
    args = parser.parse_args()
    out = args.output.resolve()
    # Never erase a source directory as a side effect of an asset build.
    if out == ROOT or out == ROOT / "lemuroid-app/src/main/res" or not out.name == "res":
        raise ValueError("Output must be a dedicated generated resource directory named res")

    spec = check_cartridge_contract()
    values = palette()
    catalog = json.loads((V04 / "catalog.json").read_text(encoding="utf-8"))
    if catalog["sourceOfTruth"]["cartridgeFamily"] != "../cartdeck-v05/cartridge-family.json":
        raise ValueError("v0.4 catalog still points at superseded cartridge")
    if tuple(catalog["semanticTokens"]) != (
        "chassis", "secondary", "bezel", "accent", "onChassis",
        "button", "onButton", "focus", "glass"
    ):
        raise ValueError("Semantic token contract has changed")

    build_assets = load_python(V04 / "build_assets.py", "cartdeck_v04_build")
    android_export = load_python(V04 / "export_android_vectors.py", "cartdeck_android_export")
    build_assets.main()  # canonical geometry, palette changes only

    if out.exists():
        shutil.rmtree(out)
    (out / "drawable-nodpi").mkdir(parents=True)
    write_colors(out, values)

    seen = set()
    assets = []
    geometry_by_source = {}
    for group in GROUPS:
        for source_rel in catalog["assetGroups"][group]:
            # Launcher/TV already reference the canonical v0.3 C-slot paths.
            # Never create duplicate branded marks or an independent dpad master.
            if source_rel.endswith("/cslot-master.svg") or source_rel.endswith("/cartdeck-wordmark-master.svg"):
                continue
            master, rel = source_path(source_rel)
            for edition in EDITIONS:
                generated = V04 / "generated" / edition / rel
                if not generated.exists():
                    raise ValueError(f"Edition source was not produced: {generated}")
                resource = f"cartdeck_h1_{edition}_{master.stem.replace('-', '_')}"
                if resource in seen:
                    raise ValueError(f"Duplicate native resource: {resource}")
                seen.add(resource)
                destination = out / "drawable-nodpi" / (resource + ".xml")
                android_export.export(generated, destination, values[edition])
                signature = geometry_signature(destination)
                old = geometry_by_source.setdefault(str(master), signature)
                if old != signature:
                    raise ValueError(f"factory edition geometry mismatch: {master}")
                assets.append({
                    "edition": edition,
                    "group": group,
                    "canonicalMaster": str(master.relative_to(ROOT)),
                    "masterSha256": digest(master.read_bytes()),
                    "nativeResource": resource,
                    "nativeSha256": digest(destination.read_bytes()),
                    "geometrySha256": signature,
                    "androidConsumer": "P1-A/B/C/D: not wired in ASSET-H1",
                })

    # Ensure the superseded old v0.4 bookshelf geometry was not exported.
    old_shelf = ("book-spine.svg", "cover-case.svg", "cover-placeholder.svg", "cartridge-edge.svg")
    if any(entry["canonicalMaster"].endswith(old_shelf) for entry in assets):
        raise ValueError("Superseded v0.4 physical case shape in native export")
    if not any(a["canonicalMaster"].endswith("dpad-master.svg") for a in assets):
        raise ValueError("Approved v0.3.1 D-pad missing")
    if len(assets) < 60:
        raise ValueError(f"Expected at least 60 factory vector resources; got {len(assets)}")

    manifest = {
        "schema": "cartdeck.nativeAssets/1.0",
        "designFamily": spec["name"],
        "visualStatus": spec["state"],
        "editions": list(EDITIONS),
        "masterDir": "design/cartdeck-v03/; design/cartdeck-v04/; design/cartdeck-v05/",
        "paletteSources": {
            edition: digest((V03 / f"editions/{edition}.json").read_bytes())
            for edition in EDITIONS
        },
        "focusContrast": {
            edition: {
                "chassis": round(contrast(values[edition]["focusOnChassis"], values[edition]["chassis"]), 3),
                "secondary": round(contrast(values[edition]["focusOnSecondary"], values[edition]["secondary"]), 3),
            }
            for edition in EDITIONS
        },
        "cartridgeArtWindow": spec["geometryLocked"]["artWindow"],
        "systemsAsLabelsNotGeometry": True,
        "lockedRendererAndPadKit": True,
        "assets": sorted(assets, key=lambda row: (row["nativeResource"], row["edition"])),
    }
    # Evidence is outside Android res/ so no JSON accidentally enters packaged resources.
    evidence = out.parent / "native-assets-manifest.json"
    evidence.write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"PASS ASSET-H1: {len(assets)} Android vectors / {len(EDITIONS)} editable editions; "
          f"resources={out}; manifest={evidence}")


if __name__ == "__main__":
    main()
