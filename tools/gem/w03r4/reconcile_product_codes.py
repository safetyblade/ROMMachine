#!/usr/bin/env python3
"""GEM-W03R5: non-destructive Wii U product-code identity resolution.
Use exact Wii U GameTDB four-character product code evidence, never fuzzy
title inference. Writes review output only; does not edit workbook or APK.
"""
from __future__ import annotations
import csv,gzip,hashlib,io,json,re,urllib.request,zipfile
from collections import defaultdict,Counter
from pathlib import Path
ROOT=Path(__file__).resolve().parent
OUT=ROOT/"generated";OUT.mkdir(exist_ok=True)
ZIP=Path("/tmp/gamedb-2026.10.09-csv.zip")
EVIDENCE=Path("/tmp/gamedb-2026.10.09-evidence.jsonl.gz")
ZIP_URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-csv.zip"
EVIDENCE_URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-evidence.jsonl.gz"
SHA_ZIP="9797a394d465fe79e7d96a9cd08698ae9ce18bd842b304ffe65d27d1c7fcca47"
SHA_EVIDENCE="88c33da6b766c804b40d825a2f79bb7b7e39b948cf7455db33425c69614e7859"
def fetch(dst,url,sha):
    h=hashlib.sha256()
    req=urllib.request.Request(url,headers={"User-Agent":"CARTDECK-GEM-W03R5-CCBY4/1"})
    with urllib.request.urlopen(req,timeout=180) as f,dst.open("wb") as out:
        while chunk:=f.read(1<<20):out.write(chunk);h.update(chunk)
    if h.hexdigest()!=sha:raise RuntimeError("Pinned archive SHA256 mismatch")
def csv_from_zip(zf,file):
    with zf.open(file) as handle:
        return list(csv.DictReader(io.TextIOWrapper(handle,encoding="utf-8-sig",newline="")))
def main():
    snap=json.loads((ROOT/"wiiu_master_snapshot.json").read_text(encoding="utf-8"))["records"]
    with (OUT/"wiiu_reconciliation.csv").open(newline="",encoding="utf-8") as f:
        old={r["catalogue_id"]:r for r in csv.DictReader(f)}
    fetch(ZIP,ZIP_URL,SHA_ZIP);fetch(EVIDENCE,EVIDENCE_URL,SHA_EVIDENCE)
    with zipfile.ZipFile(ZIP) as zf:
        releases=[r for r in csv_from_zip(zf,"releases.csv") if r.get("platform_id")=="wiiu"]
        dumps=[d for d in csv_from_zip(zf,"dumps.csv") if d.get("platform_id")=="wiiu"]
    releases_by_id={r["id"]:r for r in releases}
    dump_to_release={str(d["id"]):d["release_id"] for d in dumps}
    code_to_release=defaultdict(set); code_sources=defaultdict(set)
    counts=Counter()
    with gzip.open(EVIDENCE,"rt",encoding="utf-8") as f:
        for line in f:
            fact=json.loads(line);did=str(fact.get("subject_id",""))
            if fact.get("subject_kind")!="dump" or did not in dump_to_release:continue
            loc=str(fact.get("locator") or "");raw=str(fact.get("value_raw") or "")
            if fact.get("source")=="gametdb":
                codes=re.findall(r"wiiutdb#([A-Z0-9]{4})(?![A-Z0-9])",loc,flags=re.I)
                if fact.get("field")=="gametdb":
                    codes+=re.findall(r"(?<![A-Z0-9])([A-Z0-9]{4})(?![A-Z0-9])",raw,flags=re.I)
                for code in set(s.upper() for s in codes):
                    code_to_release[code].add(dump_to_release[did]);code_sources[(code,dump_to_release[did])].add(loc)
                    counts["code_genre_evidence"]+=1
    output=[]
    for rec in snap:
        prev=old[rec["catalogue_id"]]
        product=rec.get("product_code") or ""
        code=product[-4:] if re.fullmatch(r"WUP-[PN]-[A-Z0-9]{4}",product) else ""
        release_ids=sorted(code_to_release.get(code,set())) if code else []
        rows=[releases_by_id[i] for i in release_ids if i in releases_by_id]
        works=sorted({r.get("work_id") for r in rows if r.get("work_id")})
        regions=sorted({r.get("region") for r in rows if r.get("region")})
        us_rows=[r for r in rows if r.get("region") in {"US","USA"} and r.get("licensed")=="commercial"]
        us_works=sorted({r.get("work_id") for r in us_rows if r.get("work_id")})
        evidence_ok=len(us_works)==1 and len(us_rows)>=1
        source_title=" | ".join(sorted({r.get("title","") for r in us_rows}))
        source_genre=" | ".join(sorted({r.get("genre","") for r in us_rows if r.get("genre")}))
        titles_diff=source_title and source_title.lower()!=rec["title"].lower()
        status=("UNIQUE_US_COMMERCIAL_CODE_MATCH" if evidence_ok else
                "CODE_MATCH_NON_US_OR_AMBIGUOUS" if release_ids else "NO_CODE_EVIDENCE")
        counts[status]+=1
        if prev["match_verdict"]=="REVIEW_REQUIRED" and evidence_ok:counts["previously_unmatched_now_code_matched"]+=1
        if (not rec.get("normalized_genre") or rec.get("normalized_genre")=="Unknown") and evidence_ok and source_genre:counts["unknown_genre_with_code_match"]+=1
        output.append({
            "catalogue_id":rec["catalogue_id"],"master_row":rec["row"],"listed_title":rec["title"],
            "product_code":product,"gametdb_code":code,"source_status":status,
            "old_title_match":prev["match_verdict"],"matched_work_ids":" | ".join(us_works if evidence_ok else works),
            "matched_release_ids":" | ".join(r.get("id","") for r in (us_rows if evidence_ok else rows)),
            "source_title":source_title,"source_genre":source_genre,
            "source_publisher":" | ".join(sorted({r.get("publisher","") for r in us_rows if r.get("publisher")})),
            "source_us_year":" | ".join(sorted({r.get("year","") for r in us_rows if r.get("year")})),
            "source_regions":" | ".join(regions),
            "source_locators":" | ".join(sorted({x for r in rows for x in code_sources.get((code,r["id"]),set())})),
            "title_differs":bool(titles_diff),
            "review_required":True
        })
    with (OUT/"wiiu_product_code_review.csv").open("w",encoding="utf-8",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=list(output[0]));writer.writeheader();writer.writerows(output)
    summary={"data_source":"Title Screen v2026.10.09 CC BY 4.0, GameTDB source locators joined to Wii U dumps",
             "sha256_archive":SHA_ZIP,"sha256_evidence":SHA_EVIDENCE,
             "counts":dict(counts),"source_code_keys":len(code_to_release),
             "examples_resolved":[x for x in output if x["source_status"]=="UNIQUE_US_COMMERCIAL_CODE_MATCH" and x["old_title_match"]=="REVIEW_REQUIRED"][:25],
             "caution":"Only research identity; no unsupported genre transfer, no game QA, no master edits"}
    (OUT/"product_code_summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in summary.items() if k!="examples_resolved"},indent=2))
if __name__=="__main__":main()
