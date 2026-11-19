#!/usr/bin/env python3
"""GEM B7-A: offline coverage audit and Title Screen candidate projection.
Candidate output is NOT a trusted package. No APK assets or approvals are written.
"""
import argparse
import collections
import hashlib
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "lemuroid-app/src/main/assets/gem"
REGISTRY = ROOT / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/SystemID.kt"
PLATFORMS = {
    "Atari 2600": "atari2600", "Atari 7800": "atari7800",
    "Atari Lynx": "lynx", "Nintendo Entertainment System": "nes",
    "Super Nintendo Entertainment System": "snes", "Nintendo 64": "n64",
    "Game Boy": "gb", "Game Boy Color": "gbc", "Game Boy Advance": "gba",
    "Nintendo DS": "nds", "Nintendo 3DS": "3ds", "Master System": "sms",
    "Mega Drive / Genesis": "md", "Game Gear": "gg", "Mega-CD / Sega CD": "scd",
    "PC Engine / TurboGrafx-16": "pce", "Neo Geo Pocket": "ngp",
    "Neo Geo Pocket Color": "ngc", "WonderSwan": "ws", "WonderSwan Color": "wsc",
    "PlayStation": "psx", "PlayStation 2": "ps2", "PlayStation 3": "ps3",
    "PlayStation Portable": "psp", "Dreamcast": "dc", "GameCube": "gc",
    "Xbox": "xbox", "Xbox 360": "xbox360", "Wii": "wii", "Wii U": "wiiu",
}
# Arcade compatibility is core-specific, not a generic platform mapping.
# DOS is not covered by this archive. Neither is mapped by inference.

def baseline():
    systems = set(re.findall(r'^\s*[A-Z0-9_]+\("([^"]+)"\)', REGISTRY.read_text(), re.M))
    if not systems:
        raise ValueError("System registry not found")
    counts = collections.Counter()
    seen = set()
    for registry in sorted(ASSETS.glob("vdg-pins-*.json")):
        for pin in json.loads(registry.read_text()):
            raw = (ASSETS.parent / pin["path"]).read_bytes()
            if hashlib.sha256(raw).hexdigest() != pin["sha256"]:
                raise ValueError("Baseline pin mismatch: " + pin["path"])
            releases = json.loads(raw)["releases"]
            if len(releases) != pin["count"]:
                raise ValueError("Baseline count mismatch")
            for release in releases:
                ident, system = release["releaseId"], release["systemId"]
                if ident in seen or system not in systems:
                    raise ValueError("Duplicate or unknown baseline record")
                seen.add(ident)
                counts[system] += 1
    return systems, counts

def project(db, systems):
    meta = dict(db.execute("SELECT key, value FROM meta"))
    if str(meta.get("schema")) != "1" or not meta.get("version"):
        raise ValueError("Unknown Title Screen schema/version")
    platform_ids = dict(db.execute("SELECT id, name FROM platform"))
    mapping = {key: PLATFORMS[name] for key, name in platform_ids.items() if name in PLATFORMS}
    if len(mapping) != len(PLATFORMS) or not set(mapping.values()) <= systems:
        raise ValueError("Platform map requires explicit review")
    rows = []
    seen = set()
    for ident, platform, title, latin, genre in db.execute(
        """SELECT r.id, r.platform_id, r.title, r.title_latin, g.name
        FROM release r JOIN work w ON w.id = r.work_id
        LEFT JOIN genre g ON g.id = r.genre_id
        WHERE r.licensed = 'commercial' AND w.kind IN ('game', 'compilation')
        ORDER BY r.id"""
    ):
        if platform not in mapping:
            continue
        title = (title or latin or "").strip()
        if not title or len(title.encode("utf-16-le")) // 2 > 512:
            continue
        if not re.fullmatch(r"r[a-z0-9]{7}", ident) or ident in seen:
            raise ValueError("Invalid or duplicate source identity")
        seen.add(ident)
        row = dict(releaseId="TS:" + ident, sourceId="titlescreen",
                   sourceRecordId=ident, systemId=mapping[platform], title=title)
        if genre and len(genre) <= 100:
            row["primaryGenre"] = genre
        rows.append(row)
    return meta["version"], rows

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sqlite", type=Path)
    parser.add_argument("--sha256", help="Independently obtained SHA256 of DECOMPRESSED SQLite")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    systems, counts = baseline()
    report = {"baselineRecords": sum(counts.values()), "registeredSystems": len(systems),
              "baselineBySystem": {s: counts[s] for s in sorted(systems)},
              "missingSystems": sorted(systems - counts.keys())}
    if args.sqlite:
        if not args.sha256 or not args.output:
            parser.error("--sqlite requires --sha256 and --output outside the repository")
        if args.output.resolve().is_relative_to(ROOT):
            parser.error("Candidate output must remain outside repository")
        with args.sqlite.open("rb") as f:
            digest = hashlib.file_digest(f, "sha256").hexdigest()
        if digest != args.sha256.lower():
            raise ValueError("Source digest mismatch")
        with sqlite3.connect(args.sqlite.resolve().as_uri() + "?mode=ro&immutable=1", uri=True) as db:
            db.execute("PRAGMA trusted_schema=OFF")
            version, rows = project(db, systems)
        candidates = collections.Counter(r["systemId"] for r in rows)
        args.output.mkdir(parents=True, exist_ok=True)
        for system in sorted(candidates):
            payload = {"catalogueVersion": "gem-ts-" + version + "-" + system,
                       "releases": [r for r in rows if r["systemId"] == system]}
            (args.output / (system + ".json")).write_text(
                json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        report.update(candidateVersion=version, sourceSha256=digest,
                      source="Data: Title Screen (https://titlescreen.org), CC BY 4.0; adapted",
                      candidateBySystem={s: candidates[s] for s in sorted(systems)},
                      candidateRecords=len(rows), candidateMissing=sorted(systems - candidates.keys()),
                      approval="UNREVIEWED; no trusted assets or record approvals generated")
    print(json.dumps(report, indent=2))

if __name__ == "__main__":
    main()
