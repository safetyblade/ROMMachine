#!/usr/bin/env python3
"""Reproduce native Arc-Rail layers from the approved SVG/token sources."""
import importlib.util
import json
import sys
from pathlib import Path
import tempfile
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "vectors", ROOT / "design/cartdeck-v04/export_android_vectors.py"
)
vectors = importlib.util.module_from_spec(spec)
spec.loader.exec_module(vectors)


def main():
    for edition in ("original", "pink"):
        colors = json.loads(
            (ROOT / f"design/cartdeck-v03/editions/{edition}.json").read_text()
        )["edition"]
        sources = list((ROOT / "design/cartdeck-v05/cartridge").glob("*.svg"))
        sources.append(ROOT / "design/cartdeck-v03/brand/cslot-master.svg")
        for source in sources:
            svg = ET.parse(source)
            for node in svg.iter():
                for kind in ("fill", "stroke"):
                    token = node.get(f"data-cd-{kind}")
                    if token:
                        node.set(kind, colors[token])
            name = source.stem.replace("-", "_")
            destination = ROOT / (
                "lemuroid-app/src/main/res/drawable/"
                f"cartdeck_{edition}_{name}.xml"
            )
            with tempfile.TemporaryDirectory() as directory:
                resolved = Path(directory) / source.name
                svg.write(resolved)
                vectors.export(resolved, destination, colors)


if __name__ == "__main__":
    main()
