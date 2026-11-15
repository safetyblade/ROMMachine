#!/usr/bin/env python3
"""Read Title Screen's pinned, checksum-verified evidence and report per-title genre support."""
from __future__ import annotations
import csv, gzip, hashlib, json, urllib.request
from collections import Counter, defaultdict
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/"generated";OUT.mkdir(exist_ok=True)
URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-evidence.jsonl.gz"
SHA="88c33da6b766c804b40d825a2f79bb7b7e39b948cf7455db33425c69614e7859"
TMP=Path("/tmp/gamedb-2026.10.09-evidence.jsonl.gz")
TARGET_IDS={"r3bnbfdy","rh6c5mrt","r8zn23q6","r3t2k8k3","w01wbbr7","w85855xd","wq2edqee","wda0debr"}
with (OUT/"wiiu_reconciliation.csv").open(newline="",encoding="utf-8") as f:
    index=list(csv.DictReader(f))
for item in index:
    TARGET_IDS.update(s.strip() for s in item.get("source_work_id","").split("|") if s.strip())
    TARGET_IDS.update(s.strip() for s in item.get("source_release_ids","").split("|") if s.strip())
req=urllib.request.Request(URL,headers={"User-Agent":"CARTDECK-GEM-W03R4-CCBY4/2"})
h=hashlib.sha256()
with urllib.request.urlopen(req,timeout=180) as source,TMP.open("wb") as dest:
    while chunk:=source.read(1024*1024):
        h.update(chunk);dest.write(chunk)
if h.hexdigest()!=SHA:raise RuntimeError("Unverified Title Screen evidence checksum")
schema=Counter();facts=Counter();kinds=Counter();sample=[];by_id=defaultdict(list);total=0
with gzip.open(TMP,"rt",encoding="utf-8") as handle:
    for line in handle:
        total+=1
        entry=json.loads(line)
        kinds[(str(entry.get("subject_kind")),str(entry.get("field")))]+=1
        if len(sample)<8 and entry.get("field")=="genre":sample.append(entry)
        if str(entry.get("subject_id")) in TARGET_IDS and entry.get("field")=="genre":
            by_id[str(entry["subject_id"])].append({k:entry.get(k) for k in ("subject_kind","subject_id","field","value","value_raw","source","tier","how","scope","locator","confidence")})
with (OUT/"wiiu_genre_evidence.csv").open("w",encoding="utf-8",newline="") as f:
    keys=["catalogue_id","source_work_id","source_release_ids","source_genre","evidence_sources","distinct_source_count","source_evidence_json"]
    w=csv.DictWriter(f,fieldnames=keys);w.writeheader()
    for item in index:
        ids=[i.strip() for k in ("source_work_id","source_release_ids") for i in item.get(k,"").split("|") if i.strip()]
        statements=[line for i in ids for line in by_id.get(i,[])]
        sources=sorted({str(line.get("source")) for line in statements})
        w.writerow({"catalogue_id":item["catalogue_id"],"source_work_id":item["source_work_id"],
                    "source_release_ids":item["source_release_ids"],"source_genre":item["source_genre"],
                    "evidence_sources":" | ".join(sources),"distinct_source_count":len(sources),
                    "source_evidence_json":json.dumps(statements[:40],ensure_ascii=False)})
out={"source":URL,"sha256":h.hexdigest(),"total":total,"target_ids":len(TARGET_IDS),
     "kinds_by_fact":{"/".join(k):v for k,v in kinds.items()},
     "sample_genre_evidence":sample,
     "genre_evidence_matched_ids":len(by_id),
     "case_examples":{i:by_id.get(i,[])[:6] for i in sorted({"w01wbbr7","w85855xd","wq2edqee","wda0debr"})}}
(OUT/"evidence_schema.json").write_text(json.dumps(out,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps({"count":total,"target_ids":len(TARGET_IDS),"genre_evidence_matched_ids":len(by_id),"kinds":out["kinds_by_fact"],"examples":out["case_examples"]},indent=2))
