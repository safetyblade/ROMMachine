#!/usr/bin/env python3
"""Build a research-only arcade catalogue from locally supplied official DAT files.

No ROM content, file names, hashes, download URLs or playable claims are exported.
This script does not access the network or modify CARTDECK application data.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

SCHEMA = "gem-02b-arcade-source-v1"
SOURCE_URLS = {
    "MAME 2003-Plus": "https://github.com/libretro/mame2003-plus-libretro/blob/master/metadata/mame2003-plus.xml",
    "FBNeo": "https://github.com/libretro/FBNeo/blob/master/dats/FinalBurn%20Neo%20(ClrMame%20Pro%20XML,%20Arcade%20only).dat",
}
CORE_PREFIXES = {"MAME 2003-Plus": "M3P", "FBNeo": "FBN"}
PARENT_COLUMNS = (
    "catalogue_id", "core", "shortname", "original_title", "release_year",
    "year_raw", "manufacturer", "source_driver", "romof_dependency",
    "orientation", "driver_status", "candidate_status", "source_url",
    "source_sha256", "evidence_status", "distribution_status",
)
VARIANT_COLUMNS = (
    "variant_id", "core", "shortname", "parent_shortname", "original_title",
    "release_year", "manufacturer", "source_driver", "source_sha256",
    "review_status",
)
EXCLUSION_COLUMNS = ("core", "shortname", "original_title", "reason")
OVERLAP_COLUMNS = ("shortname", "mame_catalogue_id", "fbneo_catalogue_id", "review_status")


def checksum(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def text_child(element: ET.Element, name: str) -> str:
    node = element.find(name)
    return ("" if node is None or node.text is None else node.text).strip()


def candidate_status(title: str, source_comment: str) -> str:
    value = (title + " " + source_comment).lower()
    if re.search(r"\b(prototype|bootleg|hack|homebrew|demo|test build|unreleased)\b", value):
        return "HOLD — nonstandard/prototype source entry"
    return "Research candidate — not runtime verified"


def parse_dat(core: str, path: Path) -> tuple[list[dict], list[dict], list[dict], dict]:
    if core not in CORE_PREFIXES:
        raise ValueError("Unsupported source core")
    if not path.is_file():
        raise FileNotFoundError(path)

    sha256 = checksum(path)
    parents: list[dict] = []
    variants: list[dict] = []
    excluded: list[dict] = []
    counts: Counter = Counter()
    seen: set[str] = set()
    context = ET.iterparse(path, events=("start", "end"))
    _, root = next(context)
    for event, element in context:
        if event != "end" or element.tag not in ("game", "machine"):
            continue
        counts["machine_records"] += 1
        shortname = element.get("name", "").strip()
        title = text_child(element, "description")
        reason = None
        if not shortname or not title:
            reason = "Missing machine shortname or description"
        elif shortname in seen:
            reason = "Duplicate shortname within same core DAT"
        elif element.get("isbios") == "yes" or element.get("isdevice") == "yes":
            reason = "BIOS or device registry entry — not a game"
        elif element.get("runnable") == "no":
            reason = "Non-runnable registry entry — not a game candidate"
        if reason:
            excluded.append({"core": core, "shortname": shortname, "original_title": title, "reason": reason})
            counts["excluded"] += 1
            root.clear()
            continue

        seen.add(shortname)
        year_raw = text_child(element, "year")
        year = int(year_raw) if re.fullmatch(r"\d{4}", year_raw) else ""
        manufacturer = text_child(element, "manufacturer")
        sourcefile = element.get("sourcefile", "")
        cloneof = element.get("cloneof", "").strip()
        romof = element.get("romof", "").strip()  # may be BIOS dependency, NOT clone
        video = element.find("video")
        orientation = "" if video is None else video.get("orientation", "")
        driver = element.find("driver")
        driver_status = "" if driver is None else driver.get("status", "")
        key = f"{CORE_PREFIXES[core]}:{shortname}"
        if cloneof:
            variants.append({
                "variant_id": key, "core": core, "shortname": shortname,
                "parent_shortname": cloneof, "original_title": title,
                "release_year": year, "manufacturer": manufacturer,
                "source_driver": sourcefile, "source_sha256": sha256,
                "review_status": "Source clone — not a distinct master game",
            })
            counts["clones"] += 1
        else:
            status = candidate_status(title, text_child(element, "comment"))
            parents.append({
                "catalogue_id": key, "core": core, "shortname": shortname,
                "original_title": title, "release_year": year, "year_raw": year_raw,
                "manufacturer": manufacturer, "source_driver": sourcefile,
                "romof_dependency": romof, "orientation": orientation,
                "driver_status": driver_status, "candidate_status": status,
                "source_url": SOURCE_URLS[core], "source_sha256": sha256,
                "evidence_status": "Source metadata only; no CARTDECK game/device QA",
                "distribution_status": "Research only — licence/rights review required",
            })
            counts["parent_candidates"] += 1
            if status.startswith("HOLD"):
                counts["parent_holds"] += 1
        root.clear()
    return parents, variants, excluded, {"sha256": sha256, "bytes": path.stat().st_size, **dict(counts)}


def read_existing_ids(path: Path | None) -> set[str]:
    if path is None:
        return set()
    with path.open(newline="", encoding="utf-8-sig") as handle:
        return {row[0].strip() for row in csv.reader(handle) if row and re.fullmatch(r"(?:M3P|FBN):[a-zA-Z0-9_]+", row[0].strip())}


def write_csv(path: Path, columns: tuple[str, ...], rows: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def build(mame: Path, fbneo: Path, output: Path, existing_ids: Path | None = None) -> dict:
    if mame.resolve() == fbneo.resolve():
        raise ValueError("DAT inputs must be distinct files")
    output.mkdir(parents=True, exist_ok=True)
    all_parent: list[dict] = []
    all_variants: list[dict] = []
    all_exclusions: list[dict] = []
    per_source: dict = {}
    for core, path in (("MAME 2003-Plus", mame), ("FBNeo", fbneo)):
        parents, variants, exclusions, stats = parse_dat(core, path)
        all_parent.extend(parents)
        all_variants.extend(variants)
        all_exclusions.extend(exclusions)
        per_source[core] = {"path": str(path), "source_url": SOURCE_URLS[core], **stats}

    pids = {x["catalogue_id"] for x in all_parent}
    if len(pids) != len(all_parent):
        raise ValueError("Duplicate parent IDs detected")
    existing = read_existing_ids(existing_ids)
    accepted = [x for x in all_parent if not x["candidate_status"].startswith("HOLD")]
    holds = [x for x in all_parent if x["candidate_status"].startswith("HOLD")]
    new = [x for x in accepted if x["catalogue_id"] not in existing]
    already = [x for x in accepted if x["catalogue_id"] in existing]
    mame_keys = {x["shortname"]: x["catalogue_id"] for x in all_parent if x["core"] == "MAME 2003-Plus"}
    fbneo_keys = {x["shortname"]: x["catalogue_id"] for x in all_parent if x["core"] == "FBNeo"}
    overlaps = [{
        "shortname": s, "mame_catalogue_id": mame_keys[s],
        "fbneo_catalogue_id": fbneo_keys[s],
        "review_status": "Shortname match only — core-specific release/ROM identity not verified",
    } for s in sorted(mame_keys.keys() & fbneo_keys.keys())]
    orphan_clones = sum(1 for x in all_variants if f"{CORE_PREFIXES[x['core']]}:{x['parent_shortname']}" not in pids)

    for name, fields, rows in (
        ("parent_candidates.csv", PARENT_COLUMNS, all_parent),
        ("new_parent_candidates.csv", PARENT_COLUMNS, new),
        ("already_catalogued.csv", PARENT_COLUMNS, already),
        ("hold_parent_candidates.csv", PARENT_COLUMNS, holds),
        ("clone_variants.csv", VARIANT_COLUMNS, all_variants),
        ("excluded_bios_devices.csv", EXCLUSION_COLUMNS, all_exclusions),
        ("cross_core_review.csv", OVERLAP_COLUMNS, overlaps),
    ):
        write_csv(output / name, fields, rows)

    manifest = {
        "schema": SCHEMA,
        "generated_utc": datetime.now(timezone.utc).isoformat(),
        "core_specific_ids": True,
        "roms_or_bios_included": False,
        "contains_rom_names_hashes_or_download_links": False,
        "game_playability_verified": False,
        "catalogue_mutated": False,
        "release_or_app_distribution_approved": False,
        "sources": per_source,
        "counts": {
            "parent_candidates": len(all_parent), "new_standard_parent_candidates": len(new),
            "already_catalogued": len(already), "hold_parent_candidates": len(holds),
            "clone_variants": len(all_variants), "excluded_bios_devices": len(all_exclusions),
            "cross_core_shortname_candidates": len(overlaps), "clones_without_parent_in_same_dat": orphan_clones,
        },
        "next_gate": "Source licence/provenance and Daniel's architecture signoff before APK use or master bulk import",
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return manifest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mame", type=Path, required=True, help="Local MAME 2003-Plus XML DAT")
    parser.add_argument("--fbneo", type=Path, required=True, help="Local FBNeo Arcade-only XML DAT")
    parser.add_argument("--output", type=Path, required=True, help="Research output directory")
    parser.add_argument("--existing-ids", type=Path, help="Optional CSV with catalogue IDs in first column")
    args = parser.parse_args()
    result = build(args.mame, args.fbneo, args.output, args.existing_ids)
    print(json.dumps(result["counts"], indent=2))


if __name__ == "__main__":
    main()
