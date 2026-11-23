#!/usr/bin/env python3
"""Census checks that native CARTDECK source genres map through the single Kotlin taxonomy.

Reads real, pinned offline assets; no network, destructive data edit, inferred ROM identity,
or per-game compatibility claims. Fails the release gate if labelled metadata is lost.
"""
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "lemuroid-app/src/main/assets/gem"
KOTLIN = ROOT / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/gemrating/GemGenreTaxonomy.kt"
PINS = ("vdg-pins-a.json", "vdg-pins-b.json", "titlescreen-pins.json")


def taxonomy():
    code = KOTLIN.read_text(encoding="utf-8")
    assert "private val genres =" in code
    section = code.split("private val genres =", 1)[1].split("private val reviewTitle", 1)[0]
    entries = re.findall(r'^\s*"([^"]+)"\s+to\s+"([^"]+)"\s*,?\s*$', section, re.M)
    mapped = dict(entries)
    assert len(mapped) == len(entries), "Duplicate taxonomy source label"
    nongame_section = code.split("private val nonGame = setOf(", 1)[1].split(")", 1)[0]
    nongame = set(re.findall(r'"([^"]+)"', nongame_section))
    unknown_section = code.split("private val ambiguous = setOf(", 1)[1].split(")", 1)[0]
    ambiguous = set(re.findall(r'"([^"]+)"', unknown_section))
    assert not (set(mapped) & nongame)
    assert not (set(mapped) & ambiguous)
    return mapped, nongame, ambiguous


def main():
    mapped, nongame, ambiguous = taxonomy()
    paths = [p for registry in PINS for p in json.loads((ASSETS / registry).read_text(encoding="utf-8"))]
    missing = Counter()
    raw = Counter()
    dispositions = Counter()
    for pin in paths:
        path = ASSETS.parent / pin["path"]
        doc = json.loads(path.read_text(encoding="utf-8"))
        assert len(doc["releases"]) == pin["count"], str(path)
        for row in doc["releases"]:
            g = (row.get("primaryGenre") or "").strip().casefold()
            dispositions["total"] += 1
            if not g:
                dispositions["blank"] += 1
                continue
            dispositions["filled"] += 1
            raw[g] += 1
            if g in mapped:
                dispositions["canonical_game"] += 1
            elif g in nongame:
                dispositions["non_game"] += 1
            elif g in ambiguous:
                dispositions["ambiguous"] += 1
                missing[g] += 1
            else:
                dispositions["unmapped"] += 1
                missing[g] += 1
    covered = dispositions["canonical_game"] + dispositions["non_game"]
    filled = dispositions["filled"]
    percent = round(covered / filled * 100, 2)
    print(json.dumps({
        "snapshot": "gem-combined-2026.11.19.1",
        "dispositions": dict(dispositions),
        "mappedGenreFilledPercent": percent,
        "mappedGenreVocabulary": len(mapped),
        "unknownGenreCounts": missing.most_common(100),
        "sourceGenreVocabulary": len(raw),
    }, indent=2))
    assert dispositions["total"] == 88246
    assert filled == 79946
    assert covered * 100 >= filled * 95, f"Taxonomy coverage {percent}% below 95% exit threshold"
    print("PASS: source genre normalization meets the P0 95% gate")


if __name__ == "__main__":
    main()
