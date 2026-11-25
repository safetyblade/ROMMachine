#!/usr/bin/env python3
"""Offline 33-console design packet validator: no Android, Gradle or native binaries."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[2]
folder = root / "docs/engineering/gc01/design-packets"
data = json.loads((folder / "manifest.json").read_text())
assert data["schema"] == "cartdeck.arc.design-packets/1"
assert data["order"][-1] == "Joint LAST"
source = (root / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/SystemID.kt").read_text()
actual = set(re.findall(r'^\s+([A-Z][A-Z0-9_]*)\("[^"]+"\),?\s*$', source, re.M))
entries = data["systems"]
ids = [row["id"] for row in entries]
assert len(ids) == len(set(ids)) == len(actual) == 33
assert set(ids) == actual, f"SystemID mismatch: {set(ids) ^ actual}"
assert set(row["batch"] for row in entries) == set("ABCDE")
for row in entries:
    ident = row["id"]
    assert row["joint"] == "blocked_until_all_solos"
    assert row["grip"]["physical"] == "pending"
    assert row["pivot"]["design"] != "solo_visual_accepted" or ident in ("GBA", "PSP")
    p = folder / f"{ident}.md"
    assert p.exists(), ident
    s = p.read_text()
    assert ident in s and "PivotArc" in s and "GripArc" in s and "Joint" in s
found = {p.stem for p in folder.glob("*.md")} - {"README"}
assert found == set(ids), f"Untracked design packets: {found ^ set(ids)}"
assert next(row for row in entries if row["id"] == "NGC")["note"].startswith("Neo Geo Pocket Color")
print("PASS: 33 distinct consoles in five design batches; zero unearned GripArc or Joint acceptance")
