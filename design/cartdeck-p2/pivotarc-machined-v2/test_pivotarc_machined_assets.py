#!/usr/bin/env python3
"""Validate owner-approved SVGZ source integrity and design-only asset hooks."""
from pathlib import Path
import gzip
import hashlib
import json
from xml.etree import ElementTree as ET
root = Path(__file__).resolve().parent
contract = json.loads((root / "pivotarc-machined-v2-contract.json").read_text())
original = gzip.decompress((root / "CARTDECK_PivotArc_Tactile_V2_Concept.svgz").read_bytes())
assert hashlib.sha256(original).hexdigest() == contract["source"]["svgSha256"]
assert len(original) == contract["source"]["sourceSizeBytes"]
concept = ET.fromstring(original)
assert concept.attrib["viewBox"] == contract["source"]["viewBox"]
cues = ET.parse(root / "pivotarc-v2-material-cues.svg").getroot()
parts = {node.attrib["data-cd-part"] for node in cues.iter() if "data-cd-part" in node.attrib}
assert parts == {"chamfered-cradle","directional-well","raised-face-seat","shoulder-seat","hold-switch"}, parts
assert "no new pointer handlers" in contract["locked"]
print("PASS: exact approved SVGZ, source SHA, artboard and five material cue components")
