#!/usr/bin/env python3
"""Source contract checks; no Android, image OCR or emulator claims."""
import json
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parent
NS = "{http://www.w3.org/2000/svg}"
config = json.loads((ROOT / "griparc-v1-manifest.json").read_text(encoding="utf-8"))
assert config["geometry"]["housing"] == "constant-full-L"
assert config["geometry"]["inactiveSlotPolicy"].startswith("render no button")
assert config["geometry"]["slotOrder"] == ["G1", "G2", "G3", "G4"]
assert config["geometry"]["minimumHitTargetDp"] >= 48
assert config["geometry"]["physicalVisualButton"]["width"] > config["geometry"]["physicalVisualButton"]["height"]
ref_shell = {}
for side in ("right", "left"):
    shell_svg = ET.parse(ROOT / f"griparc-shell-{side}.svg").getroot()
    assert shell_svg.attrib["data-cd-housing"] == "full-L"
    shell_path = next(node for node in shell_svg.iter() if node.attrib.get("id") == "griparc-shell")
    ref_shell[side] = shell_path.attrib["d"]
    assert not any("data-cd-slot" in e.attrib for e in shell_svg.iter())
    for count in range(1, 5):
        svg = ET.parse(ROOT / f"griparc-{side}-{count}.svg").getroot()
        assert svg.attrib["viewBox"] == shell_svg.attrib["viewBox"]
        assert svg.attrib["data-cd-count"] == str(count)
        assert svg.attrib["data-cd-housing"] == "full-L"
        path = next(node for node in svg.iter() if node.attrib.get("id") == "griparc-shell")
        assert path.attrib["d"] == ref_shell[side], (side, count, "shell changed")
        groups = {e.attrib["data-cd-slot"]: e for e in svg.iter() if "data-cd-slot" in e.attrib}
        assert sorted(groups) == [f"G{x}" for x in range(1, count + 1)], (side, count)
        for slot in config["slots"]:
            if slot["n"] <= count:
                g = groups[slot["id"]]
                center = slot["centerLeft" if side == "left" else "centerRight"]
                assert float(g.attrib["data-cd-center-x"]) == center["x"]
                assert float(g.attrib["data-cd-center-y"]) == center["y"]
                assert g.attrib["data-cd-button-style"] == "shoulder-LR"
                assert slot["hitTarget"]["w"] >= 48 and slot["hitTarget"]["h"] >= 48
        # Every inactive key must be completely absent, not an invisible hitbox.
        assert not any(e.attrib.get("data-cd-slot") in [f"G{x}" for x in range(count + 1, 5)] for e in svg.iter())
for slot in config["slots"]:
    assert slot["centerRight"]["x"] + slot["centerLeft"]["x"] == config["artboard"]["width"]
    assert slot["centerRight"]["y"] == slot["centerLeft"]["y"]
for state in ("rest", "pressed"):
    node = ET.parse(ROOT / f"griparc-button-{state}.svg").getroot()
    assert node.attrib["data-cd-button-style"] == "shoulder-LR"
print("PASS: 2 mirrored full-L shells; 8 fixed-slot 1–4 compositions; L/R-sized buttons; no inactive keys")
