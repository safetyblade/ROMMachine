#!/usr/bin/env python3
"""GEM W03R4 — isolated Title Screen Wii U source verification.
Downloads the pinned CC-BY-4.0 release, verifies SHA256 and produces only
small, source-attributed research output. NEVER writes the GEM master.
"""
from __future__ import annotations

import csv
import hashlib
import io
import json
import re
import unicodedata
import urllib.request
import zipfile
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "generated"
OUTPUT.mkdir(parents=True, exist_ok=True)
SOURCE = "https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-csv.zip"
SHA256 = "9797a394d465fe79e7d96a9cd08698ae9ce18bd842b304ffe65d27d1c7fcca47"
INPUT = ROOT / "wiiu_master_snapshot.json"
ZIP_PATH = Path("/tmp/gamedb-2026.10.09-csv.zip")
ATTRIBUTION = "Data: Title Screen (https://titlescreen.org), CC BY 4.0"

def normalize(s):
    s = unicodedata.normalize("NFKD", str(s or ""))
    s = "".join(c for c in s if not unicodedata.combining(c))
    s = s.casefold().replace("&", " and ")
    s = re.sub(r"[\u00ae\u2122\u00a9]", "", s)
    s = re.sub(r"\bthe\s+(.+)$", r"\1", s) if s.startswith("the ") else s
    return re.sub(r"[^a-z0-9]+", "", s)

def write_json(name, data):
    (OUTPUT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def pick(keys, choices):
    m = {k.casefold(): k for k in keys}
    for candidate in choices:
        if candidate.casefold() in m:
            return m[candidate.casefold()]
    return None

def download_and_verify():
    h = hashlib.sha256()
    req = urllib.request.Request(SOURCE, headers={"User-Agent":"CARTDECK-GEM-W03R4-CCBY4-research/1.0"})
    with urllib.request.urlopen(req, timeout=180) as response, ZIP_PATH.open("wb") as out:
        while True:
            part = response.read(1 << 20)
            if not part:
                break
            h.update(part)
            out.write(part)
    digest = h.hexdigest()
    if digest != SHA256:
        raise RuntimeError(f"Title Screen SHA256 mismatch: got {digest}, expected {SHA256}")
    return {"source": SOURCE, "sha256": digest, "size_bytes": ZIP_PATH.stat().st_size,
            "licence": "CC BY 4.0", "attribution": ATTRIBUTION}

def main():
    provenance = download_and_verify()
    input_rows = json.loads(INPUT.read_text(encoding="utf-8"))["records"]
    inventory = []
    table_rows = {}
    with zipfile.ZipFile(ZIP_PATH) as zf:
        members = [n for n in zf.namelist() if n.lower().endswith(".csv")]
        for name in members:
            with zf.open(name) as handle:
                reader = csv.DictReader(io.TextIOWrapper(handle, encoding="utf-8-sig", errors="replace", newline=""))
                columns = reader.fieldnames or []
                first_rows = []
                count = 0
                for record in reader:
                    count += 1
                    if len(first_rows) < 3:
                        first_rows.append(record)
                inventory.append({"filename": name, "columns": columns, "rows": count, "sample": first_rows})
        # The flattened archive is expected to provide a release+genre+platform join
        # (sometimes in distinct relational files). No speculative assumptions:
        # if a safe join cannot be inferred, return an evidence audit and no fills.
        for info in inventory:
            name, keys = info["filename"], info["columns"]
            title = pick(keys, ["title", "game_title", "game_name", "name"])
            platform = pick(keys, ["platform", "platform_name", "platform_slug", "system", "system_name"])
            genre = pick(keys, ["genre", "genre_name", "primary_genre", "genre_label"])
            if title and platform and genre:
                with zf.open(name) as handle:
                    reader = csv.DictReader(io.TextIOWrapper(handle, encoding="utf-8-sig", errors="replace", newline=""))
                    table_rows[name] = (title, platform, genre, list(reader))
    write_json("archive_schema_inventory.json", {"provenance": provenance, "tables": inventory})
    lookup = defaultdict(list)
    table_names = []
    for name, (title_field, platform_field, genre_field, records) in table_rows.items():
        table_names.append(name)
        market_field = pick(records[0].keys(), ["market", "region", "country", "market_code"]) if records else None
        class_field = pick(records[0].keys(), ["class", "release_class", "release_type"]) if records else None
        for record in records:
            if normalize(record.get(platform_field)) not in {"wiiu", "nintendowiiu"}:
                continue
            if class_field and record.get(class_field) and normalize(record.get(class_field)) not in {"commercial", "retail"}:
                continue
            g = (record.get(genre_field) or "").strip()
            if not g:
                continue
            market = (record.get(market_field) or "").strip() if market_field else ""
            if market and market.upper() not in {"US", "USA", "UNITED STATES", "WORLD", "GLOBAL"}:
                continue
            key = normalize(record.get(title_field))
            if key:
                lookup[key].append({"title": record.get(title_field), "genre": g,
                                     "market": market, "source_file": name})
    audit = []
    states = Counter()
    for rec in input_rows:
        matches = lookup.get(normalize(rec["title"]), [])
        distinct = sorted(set(x["genre"] for x in matches))
        if len(distinct) == 1 and matches:
            state, new_genre = "MATCHED_PROVISIONAL_REVIEW", distinct[0]
        elif len(distinct) > 1:
            state, new_genre = "AMBIGUOUS_SOURCE_GENRES", ""
        else:
            state, new_genre = "SOURCE_MATCH_PENDING", ""
        states[state] += 1
        audit.append({"master_row": rec["row"], "catalogue_id": rec["catalogue_id"],
                      "game_title": rec["title"], "existing_genre": rec["normalized_genre"],
                      "candidate_source_genre": new_genre, "status": state,
                      "match_count": len(matches), "evidence": matches[:8],
                      "review_required": True})
    with (OUTPUT / "wiiu_reconciliation.csv").open("w", encoding="utf-8", newline="") as out:
        fields = ["master_row", "catalogue_id", "game_title", "existing_genre", "candidate_source_genre",
                  "status", "match_count", "evidence_json", "review_required"]
        writer = csv.DictWriter(out, fieldnames=fields)
        writer.writeheader()
        for item in audit:
            writer.writerow({**{k:item.get(k, "") for k in fields},
                             "evidence_json": json.dumps(item["evidence"], ensure_ascii=False)})
    report = {"archive": provenance, "master_input_records": len(input_rows),
              "source_tables_with_direct_title_platform_genre": table_names,
              "outcome_counts": dict(states),
              "source_model_requires_relational_join": not bool(table_names),
              "decision": "Research evidence only. NO master mutations or game/DeckFit QA implied.",
              "next": "If relational tables: inspect archive_schema_inventory.json and implement exact game/release/platform/genre joins."}
    write_json("verification_summary.json", report)
    print(json.dumps(report, indent=2))

if __name__ == "__main__":
    main()
