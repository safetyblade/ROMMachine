#!/usr/bin/env python3
"""Validate the exact B7 candidate bytes independently inspected from ingestion artifact."""
import hashlib
import json
from pathlib import Path

from b7_catalogue_coverage import baseline
from stage_titlescreen import QUARANTINE, SOURCE

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "lemuroid-app/src/main/assets"
PIN_SHA = "a04193e5fe23389d3cc8b16c56658037b5ee7deb8ecb340b3b180eb21629e4c3"
QUARANTINE_SHA = "bee84fa2b107762db030b300cbca5f197c1dc6b2fe368736477257fb7bb9fd9b"

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def verify():
    systems, counts = baseline()
    raw = (ASSETS / "gem/titlescreen-pins.json").read_bytes()
    assert sha(raw) == PIN_SHA
    pins = json.loads(raw)
    assert len(pins) == 30
    assert len({p["path"] for p in pins}) == 30
    seen, covered = set(), set()
    for pin in pins:
        path = ASSETS / pin["path"]
        assert path.parent == ASSETS / "gem/titlescreen"
        system = path.stem
        assert system in systems
        data = path.read_bytes()
        assert sha(data) == pin["sha256"]
        manifest = json.loads(data)
        assert manifest["schemaVersion"] == 1
        assert manifest["catalogueVersion"] == pin["version"]
        assert manifest["sources"] == pin["sources"] == [SOURCE]
        assert len(manifest["releases"]) == manifest["releaseCount"] == pin["count"]
        covered.add(system)
        for row in manifest["releases"]:
            assert row["systemId"] == system
            assert row["sourceId"] == "titlescreen"
            assert row["releaseId"] == "TS:" + row["sourceRecordId"]
            assert row["releaseId"] not in seen and not QUARANTINE.search(row["title"])
            assert not row["verifiedSha256"] and not row["verifiedSerials"]
            seen.add(row["releaseId"])
    assert len(seen) == 74030 and len(covered) == 30
    quarantine = (ROOT / "docs/gemrating/titlescreen-quarantine.json").read_bytes()
    assert sha(quarantine) == QUARANTINE_SHA
    rejected = json.loads(quarantine)
    assert len(rejected) == 1365
    assert len({r["releaseId"] for r in rejected}) == len(rejected)
    assert not seen.intersection(r["releaseId"] for r in rejected)
    assert all(QUARANTINE.search(r["title"]) for r in rejected)
    assert sum(counts.values()) == 14216
    assert covered | set(counts) == systems - {"dos", "fbneo", "mame2003plus"}
    print("PASS: 74,030 pinned Title Screen + 14,216 immutable VDG records; 88,246 across 30 systems; 1,365 quarantined")

if __name__ == "__main__":
    verify()
