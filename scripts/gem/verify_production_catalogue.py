#!/usr/bin/env python3
"""Verify the committed historical GEM baseline from its retained GPLv3 SQL source.

Never executes SQL or requests a network resource. All unnumbered table rows use
snapshot-specific ordinal IDs; they must not be mapped to a different snapshot.
"""
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "lemuroid-app/src/main/assets"
SOURCE = ROOT / "third_party/gem/videogames-database"
SYSTEMS = {
    "2600": "atari2600", "3ds": "3ds", "dc": "dc", "ds": "nds",
    "gb": "gb", "gba": "gba", "gc": "gc", "gen": "md", "gg": "gg",
    "n64": "n64", "nes": "nes", "ps": "psx", "ps2": "ps2",
    "ps3": "ps3", "psp": "psp", "scd": "scd", "snes": "snes",
    "tg16": "pce", "wii": "wii", "wiiu": "wiiu", "ws": "ws",
    "x360": "xbox360", "xb": "xbox",
}
Q = r"'((?:\\.|''|[^'])*)'"
ROW = re.compile(r"^\(\s*(?:(\d+)\s*,\s*)?" + Q + r"\s*,\s*" + Q
                 + r"\s*,\s*(?:" + Q + r"|NULL)\s*,\s*(?:\d+|NULL)\s*,\s*" + Q + r"\s*\)\s*[,;]\s*$")
EXCLUDED = re.compile(
    r"\b(?:demo(?:nstration)?|bios|firmware|prototype|sample|trial|test disc|"
    r"weekly sales|system update|bonus disc|video disc)\b", re.I | re.ASCII)
def unquote(value):
    return re.sub(r"""\\(['"\\])""", r"\1", value.replace("''", "'"))

def git_blob_sha(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def rebuild(table, source):
    raw = (SOURCE / (table + ".sql")).read_bytes()
    assert git_blob_sha(raw) == source["revision"], f"{table}: upstream source blob mismatch"
    rows = [line.strip() for line in raw.decode("utf-8").splitlines()
            if re.match(r"^\s*\(", line) and re.search(r"\)\s*[,;]\s*$", line)]
    assert rows, table
    result, ids = [], set()
    for ordinal, line in enumerate(rows, 1):
        match = ROW.fullmatch(line)
        assert match, f"{table}: malformed source row {ordinal}"
        ident, title, genre, _, _ = match.groups()
        ident = ident or f"row-{ordinal}"
        assert ident not in ids, f"{table}: duplicate source row"
        ids.add(ident)
        title, genre = unquote(title).strip(), unquote(genre).strip()
        if not title or len(title.encode("utf-16-le")) // 2 > 512 or EXCLUDED.search(title):
            continue
        release = dict(releaseId=f"VDG:{table}:{ident}", systemId=SYSTEMS[table],
                       title=title, sourceId=source["id"], sourceRecordId=ident,
                       verifiedSha256=[], verifiedSerials=[])
        if genre and genre.lower() not in {"misc", "unknown", "other"}:
            release["primaryGenre"] = genre
        result.append(release)
    return sorted(result, key=lambda r: r["releaseId"])

def verify():
    pins = []
    for group in ("a", "b"):
        pins.extend(json.loads((ASSETS / f"gem/vdg-pins-{group}.json").read_text(encoding="utf-8")))
    assert len(pins) == len(SYSTEMS) == 23
    assert {Path(pin["path"]).stem for pin in pins} == set(SYSTEMS)
    assert (ASSETS / "gem/licenses/VDG-GPL-3.0.txt").read_bytes() == (SOURCE / "LICENSE").read_bytes()
    total = 0
    for pin in pins:
        path = ASSETS / pin["path"]
        table = path.stem
        data = path.read_bytes()
        assert hashlib.sha256(data).hexdigest() == pin["sha256"], f"{table}: pin mismatch"
        manifest = json.loads(data)
        assert manifest["schemaVersion"] == 1
        assert manifest["catalogueVersion"] == pin["version"]
        assert manifest["sources"] == pin["sources"]
        source, = manifest["sources"]
        assert source["id"] == "vdg-" + table and source["licence"] == "GPL-3.0-only"
        assert source["attribution"] and source["url"]
        expected = rebuild(table, source)
        assert expected == manifest["releases"], f"{table}: rows do not reproduce"
        assert len(expected) == manifest["releaseCount"] == pin["count"]
        total += len(expected)
        print(f"{table}: {len(expected)} validated records")
    assert total == 14216, total
    print(f"PASS: {total} source-reproduced, pinned entries across {len(pins)} tables")

if __name__ == "__main__":
    verify()
