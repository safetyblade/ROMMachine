#!/usr/bin/env python3
"""Read-only quality census of CARTDECK's actual pinned, offline GEM release shards.

A diagnostic, not a replacement database or a source-update process. Ineligible
does not imply invalid content: the current GEM recommender uses a narrow genre
allow-list and title regex; record that exact observed behaviour without deleting data.
"""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "lemuroid-app/src/main/assets/gem"
APPROVALS = ("vdg-pins-a.json", "vdg-pins-b.json", "titlescreen-pins.json")
ACCEPTED_GENRES = {
    "action", "adventure", "fighting", "platform", "puzzle", "racing",
    "role-playing", "shooter", "simulation", "sports", "strategy",
}
# Mirrors GemDiscovery.nonGames as of this census; flags are NOT confirmed non-games.
TITLE_REJECT = re.compile(
    r"\b(demo|prototype|bios|firmware|sample|trial|map pack|bonus disc|video disc|double pack|"
    r"triple pack|compilation|soundtrack|system update|test disc|expansion pack|dlc)\b",
    re.IGNORECASE,
)
SPACE = re.compile(r"\s+")


def rate(n, d):
    return round(100.0 * n / d, 2) if d else 0.0


def census():
    pins = [pin for registry in APPROVALS for pin in json.loads((ASSETS / registry).read_text())]
    assert len(pins) == 53, "Expected 53 retained signed/approved source shards"
    rows_by_id = {}
    source_stats = defaultdict(Counter)
    platform_stats = defaultdict(Counter)
    raw_genres = Counter()
    excluded_genres = Counter()
    filtered_examples = []
    duplicate_keys = defaultdict(lambda: defaultdict(list))
    unknown_genre_examples = []
    for pin in pins:
        path = ASSETS.parent / pin["path"]
        doc = json.loads(path.read_text(encoding="utf-8"))
        assert doc["releaseCount"] == pin["count"] == len(doc["releases"]), str(path)
        assert doc["catalogueVersion"] == pin["version"], str(path)
        for row in doc["releases"]:
            rid = row["releaseId"]
            assert rid not in rows_by_id, f"Duplicate source-qualified release ID: {rid}"
            rows_by_id[rid] = row
            source = "Title Screen" if rid.startswith("TS:") else "VDG"
            assert source == ("Title Screen" if pin["path"].startswith("gem/titlescreen/") else "VDG")
            system = row["systemId"]
            genre = (row.get("primaryGenre") or "").strip().casefold()
            is_genre_known = bool(genre)
            is_recognised = genre in ACCEPTED_GENRES
            title_blocked = bool(TITLE_REJECT.search(row["title"]))
            s = source_stats[source]
            p = platform_stats[system]
            for stats in (s, p):
                stats["total"] += 1
                stats["genre_filled"] += is_genre_known
                stats["genre_recognised"] += is_recognised
                stats["recommender_eligible"] += is_recognised and not title_blocked
                stats["title_regex_blocked"] += title_blocked
                stats["verified_sha256"] += bool(row.get("verifiedSha256"))
                stats["verified_serials"] += bool(row.get("verifiedSerials"))
            raw_genres[genre or "(missing)"] += 1
            if genre and not is_recognised:
                excluded_genres[genre] += 1
            if title_blocked and len(filtered_examples) < 50:
                filtered_examples.append({"releaseId": rid, "title": row["title"], "genre": genre})
            if not genre and len(unknown_genre_examples) < 15:
                unknown_genre_examples.append({"releaseId": rid, "title": row["title"]})
            key = (system, SPACE.sub(" ", row["title"].strip()).casefold())
            duplicate_keys[key][source].append(rid)

    assert len(rows_by_id) == 88246, len(rows_by_id)
    feature_path = ASSETS / "features/v1.json"
    feature_doc = json.loads(feature_path.read_text(encoding="utf-8"))
    assert feature_doc["schemaVersion"] == 1
    features = feature_doc["records"]
    assert len({r["releaseId"] for r in features}) == len(features), "Duplicate feature IDs"
    bad = [
        r["releaseId"] for r in features
        if r["releaseId"] not in rows_by_id or r["title"] != rows_by_id[r["releaseId"]]["title"]
    ]
    assert not bad, f"Feature-pack identity mismatch: {bad[:8]}"

    cross_source = [
        {"system": system, "title": title, "vdg": len(src["VDG"]), "titlescreen": len(src["Title Screen"])}
        for (system, title), src in duplicate_keys.items()
        if src["VDG"] and src["Title Screen"]
    ]
    cross_source.sort(key=lambda r: (r["system"], r["title"]))
    in_source_collision = [
        {"system": system, "title": title, "source": source, "releaseRecords": len(ids)}
        for (system, title), src in duplicate_keys.items()
        for source, ids in src.items() if len(ids) > 1
    ]
    total = len(rows_by_id)
    recognized = sum(v["genre_recognised"] for v in source_stats.values())
    eligible = sum(v["recommender_eligible"] for v in source_stats.values())
    filled = sum(v["genre_filled"] for v in source_stats.values())
    enriched = len(features)
    out = {
        "reportType": "GEM offline catalogue quality census — read only",
        "snapshot": "gem-combined-2026.11.19.1",
        "releaseCount": total,
        "shardCount": len(pins),
        "platformCount": len(platform_stats),
        "sourceStats": {k: dict(v) for k, v in sorted(source_stats.items())},
        "overall": {
            "genreFilled": filled,
            "genreFilledPercent": rate(filled, total),
            "genreRecognisedByCurrentRecommender": recognized,
            "genreRecognisedPercent": rate(recognized, total),
            "currentlyEligibleForRecommendations": eligible,
            "eligiblePercent": rate(eligible, total),
            "notEligibleUnderCurrentRules": total - eligible,
            "featuresEnrichedExactReleaseCount": enriched,
            "featuresEnrichedPercent": rate(enriched, total),
            "verifiedHashRows": sum(v["verified_sha256"] for v in source_stats.values()),
            "verifiedSerialRows": sum(v["verified_serials"] for v in source_stats.values()),
            "crossSourceSameSystemSameTitleKeyCount": len(cross_source),
            "withinSourceSameSystemSameTitleKeyCount": len(in_source_collision),
        },
        "platformStats": {k: dict(v) for k, v in sorted(platform_stats.items())},
        "topUnrecognisedGenres": excluded_genres.most_common(35),
        "topSourceGenres": raw_genres.most_common(40),
        "titleRegexBlockedExamples": filtered_examples,
        "unknownGenreExamples": unknown_genre_examples,
        "crossSourceCollisionExamples": cross_source[:40],
        "withinSourceCollisionExamples": in_source_collision[:20],
        "evidence": "Derived from currently bundled pinned release assets, current 11-label recommender allow-list, current title regex and exact-ID feature pack. Identical titles are collision candidates, not verified duplicate editions or canonical games.",
    }
    return out


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, help="Optional JSON output artifact path")
    args = parser.parse_args()
    report = census()
    print(json.dumps({
        "reportType": report["reportType"],
        "snapshot": report["snapshot"],
        "overall": report["overall"],
        "sourceStats": report["sourceStats"],
        "topUnrecognisedGenres": report["topUnrecognisedGenres"][:18],
        "titleRegexBlockedExamples": report["titleRegexBlockedExamples"][:15],
    }, indent=2, ensure_ascii=False))
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
