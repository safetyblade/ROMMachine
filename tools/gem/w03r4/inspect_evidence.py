#!/usr/bin/env python3
"""Inspect exact Title Screen evidence fields; never change master catalogue."""
import gzip, hashlib, json, urllib.request
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/"generated";OUT.mkdir(exist_ok=True)
URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-evidence.jsonl.gz"
SHA="88c33da6b766c804b40d825a2f79bb7b7e39b948cf7455db33425c69614e7859"
TMP=Path("/tmp/gamedb-2026.10.09-evidence.jsonl.gz")
SAMPLE_RELEASES={"r3bnbfdy","rh6c5mrt","r8zn23q6","r3t2k8k3"}
req=urllib.request.Request(URL,headers={"User-Agent":"CARTDECK-GEM-W03R4-CCBY4/1"})
h=hashlib.sha256()
with urllib.request.urlopen(req,timeout=180) as source,TMP.open("wb") as dest:
    while chunk:=source.read(1024*1024):
        h.update(chunk);dest.write(chunk)
if h.hexdigest()!=SHA:raise RuntimeError("Unverified evidence checksum")
schema=Counter();facts=Counter();sample=[];picked=[];total=0
with gzip.open(TMP,"rt",encoding="utf-8") as handle:
    for line in handle:
        total+=1
        entry=json.loads(line)
        for key in entry:schema[key]+=1
        if len(sample)<3:sample.append(entry)
        fact=entry.get("fact") or entry.get("field") or entry.get("predicate") or ""
        facts[str(fact)]+=1
        if any(r in json.dumps(entry,ensure_ascii=False) for r in SAMPLE_RELEASES):
            if len(picked)<45:picked.append(entry)
out={"source":URL,"sha256":h.hexdigest(),"count":total,"keys":dict(schema),"facts":facts.most_common(25),"sample":sample,"selected_release_evidence":picked}
(OUT/"evidence_schema.json").write_text(json.dumps(out,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(json.dumps({"total":total,"keys":dict(schema),"fact_types":facts.most_common(15),"sample":sample[:1],"selected_count":len(picked)},ensure_ascii=False,indent=2))
