#!/usr/bin/env python3
"""Build a deterministic GEM v1 candidate from a separately reviewed export.
No workbook scraping, network access, or implicit approval of research records.
Usage: python3 scripts/gem/export_catalogue.py reviewed.json approval.json output.json
The resulting digest must be independently reviewed and pinned by CARTDECK.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

SYSTEMS = set("nes snes md gb gbc gba n64 sms psp nds gg atari2600 psx fbneo mame2003plus pce lynx atari7800 scd ngp ngc ws wsc dos 3ds dc gc ps2 xbox wii xbox360 wiiu ps3".split())
SOURCE_FIELDS = {"id", "name", "url", "licence", "attribution", "revision"}
RELEASE_FIELDS = {"releaseId", "systemId", "title", "sourceId", "sourceRecordId", "primaryGenre", "verifiedSha256", "verifiedSerials"}

def require(condition, message):
    if not condition:
        raise ValueError(message)

def build(reviewed, approvals):
    require(set(reviewed) == {"catalogueVersion", "releases"}, "Unexpected input field")
    version = reviewed["catalogueVersion"]
    require(isinstance(version, str) and version.strip(), "Version required")
    require(approvals["catalogueVersion"] == version, "Approval version mismatch")
    sources = approvals["sources"]
    require(all(set(s) == SOURCE_FIELDS and all(isinstance(v, str) and v.strip() for v in s.values()) and s["url"].startswith("https://") for s in sources), "Invalid source declarations")
    source_ids = {s["id"] for s in sources}
    require(len(source_ids) == len(sources), "Duplicate source ID")
    # Approval file must be produced by a separate review, not by this exporter.
    approved = {tuple(x) for x in approvals["approvedRecords"]}
    releases = reviewed["releases"]
    require(len(releases) <= 100000, "Too many releases")
    ids, source_records = set(), set()
    output = []
    for row in releases:
        require(set(row) <= RELEASE_FIELDS and {"releaseId","systemId","title","sourceId","sourceRecordId"} <= set(row), "Unexpected or missing release field")
        record = (row["sourceId"], row["sourceRecordId"], row["releaseId"])
        require(row["sourceId"] in source_ids and record in approved, "Record not approved: " + str(record))
        require(all(isinstance(v, str) and v.strip() for v in record), "Invalid identity")
        require(row["releaseId"] not in ids and record[:2] not in source_records, "Duplicate release or source record")
        require(row["systemId"] in SYSTEMS and isinstance(row["title"], str) and 0 < len(row["title"]) <= 512 and row["title"].strip() and len(row["releaseId"]) <= 256, "Invalid release")
        genre = row.get("primaryGenre")
        require(genre is None or isinstance(genre, str) and len(genre) <= 128, "Invalid genre")
        hashes, serials = row.get("verifiedSha256", []), row.get("verifiedSerials", [])
        require(isinstance(hashes, list) and len(hashes) <= 32 and all(isinstance(h, str) and re.fullmatch("[a-fA-F0-9]{64}", h) for h in hashes), "Invalid verified hash")
        require(isinstance(serials, list) and len(serials) <= 32 and all(isinstance(s, str) and 0 < len(s) <= 128 and s.strip() for s in serials), "Invalid verified serial")
        ids.add(row["releaseId"])
        source_records.add(record[:2])
        output.append(dict(row, verifiedSha256=sorted({h.lower() for h in hashes}), verifiedSerials=sorted(set(serials))))
    require(approved == {(r["sourceId"], r["sourceRecordId"], r["releaseId"]) for r in output}, "Approval/export coverage mismatch")
    manifest = dict(schemaVersion=1, catalogueVersion=version, releaseCount=len(output), sources=sorted(sources, key=lambda s:s["id"]), releases=sorted(output, key=lambda r:r["releaseId"]))
    payload = (json.dumps(manifest, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")
    require(len(payload) <= 32*1024*1024, "Package too large")
    return payload

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reviewed", type=Path)
    parser.add_argument("approval", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    payload = build(json.loads(args.reviewed.read_text(encoding="utf-8")), json.loads(args.approval.read_text(encoding="utf-8")))
    args.output.write_bytes(payload)
    print("Candidate SHA-256:", hashlib.sha256(payload).hexdigest())
    print("Review and pin this digest independently before application import.")

if __name__ == "__main__":
    main()
