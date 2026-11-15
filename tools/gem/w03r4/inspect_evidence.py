#!/usr/bin/env python3
"""Map CC-BY-4.0 source genre assertions through dump IDs to Wii U releases."""
from __future__ import annotations
import csv,gzip,hashlib,io,json,urllib.request,zipfile
from collections import defaultdict,Counter
from pathlib import Path
ROOT=Path(__file__).resolve().parent;OUT=ROOT/"generated";OUT.mkdir(exist_ok=True)
URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-evidence.jsonl.gz"
SHA="88c33da6b766c804b40d825a2f79bb7b7e39b948cf7455db33425c69614e7859"
TMP=Path("/tmp/gamedb-2026.10.09-evidence.jsonl.gz")
CSVZIP=Path("/tmp/gamedb-2026.10.09-csv.zip")
with (OUT/"wiiu_reconciliation.csv").open(newline="",encoding="utf-8") as handle:
    entries=list(csv.DictReader(handle))
ids={s.strip() for r in entries for s in r["source_release_ids"].split("|") if s.strip()}
dump_to_release={}
with zipfile.ZipFile(CSVZIP) as z:
    with z.open("dumps.csv") as handle:
        for row in csv.DictReader(io.TextIOWrapper(handle,encoding="utf-8-sig",newline="")):
            if row.get("platform_id")=="wiiu" and row.get("release_id") in ids:
                dump_to_release[row["id"]]=row["release_id"]
sha=hashlib.sha256()
req=urllib.request.Request(URL,headers={"User-Agent":"CARTDECK-GEM-W03R4-CCBY4/3"})
with urllib.request.urlopen(req,timeout=180) as response,TMP.open("wb") as out:
    while part:=response.read(1024*1024):
        sha.update(part);out.write(part)
if sha.hexdigest()!=SHA:raise RuntimeError("Evidence checksum mismatch")
counts=Counter();by_release=defaultdict(list);genre_total=0
with gzip.open(TMP,"rt",encoding="utf-8") as gz:
    for ln in gz:
        fact=json.loads(ln)
        counts[fact.get("subject_kind","")+"/"+fact.get("field","")]+=1
        if fact.get("subject_kind")!="dump" or fact.get("field")!="genre":continue
        genre_total+=1
        source_id=str(fact.get("subject_id",""))
        if source_id not in dump_to_release:continue
        release_id=dump_to_release[source_id]
        by_release[release_id].append({
            "source":fact.get("source"),"raw":fact.get("value_raw"),
            "normalized":fact.get("value"),"tier":fact.get("tier"),
            "how":fact.get("how"),"confidence":fact.get("confidence"),
            "locator":fact.get("locator")
        })
output=[]
for r in entries:
    linked=[i.strip() for i in r.get("source_release_ids","").split("|") if i.strip()]
    facts=[fact for release_id in linked for fact in by_release.get(release_id,[])]
    unique_sources=sorted({str(fact.get("source")) for fact in facts})
    normalized_by_source={}
    for fact in facts:
        k=str(fact.get("source"))
        normalized_by_source.setdefault(k,[])
        normalized_by_source[k].append(str(fact.get("normalized") or ""))
    source_support=Counter()
    for source,genres in normalized_by_source.items():
        for genre in set(genres):
            if genre:source_support[genre]+=1
    output.append({
        "catalogue_id":r["catalogue_id"],
        "source_work_id":r["source_work_id"],
        "source_release_ids":r["source_release_ids"],
        "compiled_genre":r["source_genre"],
        "distinct_sources":len(unique_sources),
        "source_names":" | ".join(unique_sources),
        "genre_source_votes":json.dumps(source_support,ensure_ascii=False),
        "sources_detail":json.dumps(facts[:40],ensure_ascii=False)
    })
with (OUT/"wiiu_genre_evidence.csv").open("w",newline="",encoding="utf-8") as out:
    w=csv.DictWriter(out,fieldnames=list(output[0]));w.writeheader();w.writerows(output)
summary={"evidence_sha256":sha.hexdigest(),"genre_statements_all_platforms":genre_total,
         "wiiu_dump_ids_linked":len(dump_to_release),"wiiu_release_ids_with_evidence":len(by_release),
         "wiiu_master_rows_with_evidence":sum(bool(x["distinct_sources"]) for x in output),
         "wiiu_master_rows_with_2plus_sources":sum(x["distinct_sources"]>=2 for x in output),
         "source_kind_field_counts":dict(counts),
         "case_examples":[x for x in output if x["catalogue_id"] in {
             "WIIU:WUB:0005000010128700","WIIU:WUB:0005000010162500","WIIU:WUB:000500001010FA00"
         }]}
(OUT/"evidence_schema.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps({k:v for k,v in summary.items() if k not in ("source_kind_field_counts","case_examples")},indent=2))
