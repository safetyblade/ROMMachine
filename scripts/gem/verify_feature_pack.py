#!/usr/bin/env python3
"""Verify the bundled GEM feature overlay against every exact pinned source record.

No network, ROM access, device state, mutation or speculative cross-platform merging.
The Title Screen-derived facts are metadata categories only, not title-play tests.
"""
from collections import Counter
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
ASSET_ROOT = ROOT / "lemuroid-app/src/main/assets"
GEM = ASSET_ROOT / "gem"
PIN_REGISTRIES = ("vdg-pins-a.json", "vdg-pins-b.json", "titlescreen-pins.json")
PACK = GEM / "features/v1.json"
SOURCE_GENRE = re.compile(r"^Source genre: (.+); taxonomy derived, gameplay untested$")


def main():
    pins = [p for name in PIN_REGISTRIES for p in json.loads((GEM / name).read_text(encoding="utf-8"))]
    source = {}
    for pin in pins:
        path = ASSET_ROOT / pin["path"]
        payload = path.read_bytes()
        assert hashlib.sha256(payload).hexdigest() == pin["sha256"], str(path)
        group = json.loads(payload)
        assert group["releaseCount"] == pin["count"]
        for row in group["releases"]:
            rid = row["releaseId"]
            assert rid not in source, f"Duplicate release: {rid}"
            source[rid] = row
    assert len(source) == 88246
    data = PACK.read_bytes()
    overlay = json.loads(data)
    assert overlay["schemaVersion"] == 1
    records = overlay["records"]
    assert len(records) == 1235, len(records)
    assert len(data) < 1024 * 1024
    assert len(records) <= 2000
    assert len({r["releaseId"] for r in records}) == len(records)
    origin = Counter()
    systems = Counter()
    for feature in records:
        rid = feature["releaseId"]
        release = source.get(rid)
        assert release is not None, f"Unknown source release: {rid}"
        assert feature["title"] == release["title"], f"Stale title for {rid}"
        assert not feature.get("verifiedWorkId"), "No title-based work identity permitted in P0"
        if feature.get("sourceId") == "titlescreen":
            assert release["sourceId"] == "titlescreen"
            assert feature["sourceRecord"] == release["sourceRecordId"]
            assert feature["sourceLicence"] == "CC-BY-4.0"
            assert feature["sourceRevision"] == "2026.10.09"
            match = SOURCE_GENRE.match(feature["evidence"])
            assert match is not None, f"Unlabelled inference for {rid}"
            assert match.group(1) == release.get("primaryGenre"), f"Source genre mismatch: {rid}"
            assert (
                feature.get("mechanics")
                or feature.get("playstyle")
                or feature.get("crossoverGenre")
            ), f"No enriched feature beyond genre: {rid}"
            origin["source-derived, not independently gameplay-reviewed"] += 1
        else:
            assert feature.get("sourceId") == "cartdeck-curated", feature["sourceId"]
            assert feature.get("evidence")
            origin["prior curated editorial seed"] += 1
        systems[release["systemId"]] += 1
    assert origin["source-derived, not independently gameplay-reviewed"] == 1200
    assert origin["prior curated editorial seed"] == 35
    print(json.dumps({
        "sourceCatalogueReleases": len(source),
        "featurePackRecords": len(records),
        "sourceDerived": origin["source-derived, not independently gameplay-reviewed"],
        "priorCurated": origin["prior curated editorial seed"],
        "platformCoverage": dict(sorted(systems.items())),
        "sha256": hashlib.sha256(data).hexdigest(),
        "sizeBytes": len(data),
        "compatibilityCertified": 0,
        "savedataOrRatingsModified": 0,
    }, indent=2))
    print("PASS: 1235 feature records link to exact preserved licensed releases")


if __name__ == "__main__":
    main()
