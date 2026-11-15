#!/usr/bin/env python3
"""GEM-W03R6: Wii U commercial identity, regional and eligibility audit.

Pinned Title Screen CC BY 4.0 archive; research-only outputs. No ROM access,
no emulation code changes, and no direct master catalogue mutations.
"""
from __future__ import annotations
import csv,hashlib,io,json,re,unicodedata,urllib.request,zipfile
from collections import Counter,defaultdict
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/"generated";OUT.mkdir(exist_ok=True)
ZIP=Path("/tmp/gamedb-2026.10.09-csv.zip")
URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-csv.zip"
SHA="9797a394d465fe79e7d96a9cd08698ae9ce18bd842b304ffe65d27d1c7fcca47"

def normalize(s):
    x=unicodedata.normalize("NFKD",str(s or "").replace("&"," and "))
    x="".join(c for c in x if not unicodedata.combining(c)).lower()
    x=re.sub(r"^(the)\s+", "",x)
    x=re.sub(r",\s*the\s*$","",x)
    x=re.sub(r"\b(?:wii u)\b","",x)
    return re.sub(r"[^a-z0-9]","",x)

def datafile(name):
    with (OUT/name).open(newline="",encoding="utf-8") as f:
        return list(csv.DictReader(f))

def read_zip(zf,name):
    with zf.open(name) as f:
        return list(csv.DictReader(io.TextIOWrapper(f,encoding="utf-8-sig",newline="")))

def main():
    # Verify or redownload source, without reliance on cached GitHub Action state.
    if not ZIP.exists() or hashlib.sha256(ZIP.read_bytes()).hexdigest()!=SHA:
        h=hashlib.sha256()
        req=urllib.request.Request(URL,headers={"User-Agent":"CARTDECK-GEM-W03R6-CCBY4"})
        with urllib.request.urlopen(req,timeout=180) as f,ZIP.open("wb") as out:
            while chunk:=f.read(1<<20):h.update(chunk);out.write(chunk)
        if h.hexdigest()!=SHA:raise RuntimeError("Unverified Title Screen source")
    original=json.loads((ROOT/"wiiu_master_snapshot.json").read_text(encoding="utf-8"))["records"]
    title_audit={r["catalogue_id"]:r for r in datafile("wiiu_reconciliation.csv")}
    code_audit={r["catalogue_id"]:r for r in datafile("wiiu_product_code_review.csv")}
    with zipfile.ZipFile(ZIP) as zf:
        all_rel=read_zip(zf,"releases.csv")
        all_works=read_zip(zf,"works.csv")
    works={r["id"]:r for r in all_works}
    wiiu=[r for r in all_rel if r.get("platform_id")=="wiiu"]
    by_work=defaultdict(list);by_title=defaultdict(list)
    for r in wiiu:
        by_work[r["work_id"]].append(r)
        by_title[normalize(r["title"])].append(r)
    output=[];counts=Counter();region_distrib=Counter();media_distrib=Counter();kind_distrib=Counter()
    for rel in wiiu:
        region_distrib[rel["region"]]+=1
        media_distrib[(rel["media"],rel["licensed"])]+=1
        kind_distrib[works.get(rel["work_id"],{}).get("kind","?")]+=1
    for item in original:
        id=item["catalogue_id"];from_title=title_audit[id];from_code=code_audit[id]
        twork=[x.strip() for x in from_title.get("source_work_id","").split("|") if x.strip()]
        cwork=[x.strip() for x in from_code.get("matched_work_ids","").split("|") if x.strip()] if from_code.get("source_status")=="UNIQUE_US_COMMERCIAL_CODE_MATCH" else []
        direct=by_title.get(normalize(item["title"]),[])
        # Existing exact product code link is an independent Wii U identity key.
        # If two independent keys conflict, do not auto-pick either.
        conflict=bool(twork and cwork and set(twork)!=set(cwork))
        if conflict:
            mode="IDENTITY_CONFLICT";chosen=[]
        elif len(set(cwork))==1:
            mode="CODE_US_WORK";chosen=cwork
        elif len(set(twork))==1:
            mode="TITLE_US_WORK";chosen=twork
        else:
            candidates={r["work_id"] for r in direct if r.get("licensed")=="commercial"}
            if len(candidates)==1:
                chosen=sorted(candidates);mode="TITLE_OTHER_REGION_WORK"
            else:
                chosen=[];mode="UNMATCHED_OR_AMBIGUOUS"
        selected=[r for w in chosen for r in by_work.get(w,[]) if r.get("licensed")=="commercial"]
        title_regions=sorted({r["region"] for r in selected})
        usa=[r for r in selected if r["region"] in {"US","USA"}]
        other=[r for r in selected if r["region"] not in {"US","USA"}]
        release_class=sorted({r["licensed"] for r in selected})
        media=sorted({r.get("media") or "" for r in selected})
        genres=sorted({r.get("genre") or "" for r in usa if r.get("genre")})
        pubs=sorted({r.get("publisher") or "" for r in usa if r.get("publisher")})
        years=sorted({r.get("year") or "" for r in usa if r.get("year")})
        origin_kind=sorted({works.get(w,{}).get("kind","unknown") for w in chosen})
        # An individual source can be commercial but still a utility or demo.
        name=item["title"].lower()
        suspect=bool(re.search(r"\b(?:demo|testfire|pre.launch|challenge[s]? app|notice|info|media|trailer|training|software|beta)\b",name))
        if "collection" in name or "pack" in name or "bundle" in name or "compilation" in name:
            category="COMPILATION_OR_BUNDLE_REVIEW"
        elif suspect or any(k not in {"game"} for k in origin_kind):
            category="POSSIBLE_NON_STANDALONE_REVIEW"
        elif not chosen:
            category="UNRESOLVED_IDENTITY"
        else:
            category="NATIVE_WIIU_GAME_CANDIDATE"
        regions={"US":sum(r["region"] in {"US","USA"} for r in selected),
                 "EU":sum(r["region"] in {"EU","EUR","Europe"} for r in selected),
                 "JP":sum(r["region"] in {"JP","JPN","Japan"} for r in selected)}
        counts[mode]+=1;counts[category]+=1
        if other:counts["WORK_WITH_NON_US_RELEASE"]+=1
        if chosen and not usa:counts["WORK_NON_US_ONLY"]+=1
        if chosen and usa:counts["WORK_US_RELEASE_PRESENT"]+=1
        output.append({
            "master_row":item["row"],"catalogue_id":id,"original_title":item["title"],
            "product_code":item.get("product_code",""),
            "work_identity_method":mode,"work_ids":" | ".join(chosen),
            "source_kind":" | ".join(origin_kind),
            "eligibility_candidate":category,
            "release_regions":" | ".join(title_regions),"us_releases":regions["US"],
            "eu_releases":regions["EU"],"jp_releases":regions["JP"],
            "all_commercial_release_count":len(selected),
            "us_source_titles":" | ".join(sorted({r["title"] for r in usa}))[:350],
            "non_us_source_titles":" | ".join(sorted({r["title"] for r in other}))[:350],
            "source_genres":" | ".join(genres),"us_publishers":" | ".join(pubs),
            "us_release_years":" | ".join(years),"media":" | ".join(media),
            "release_ids":" | ".join(r["id"] for r in selected)[:750],
            "source_tier":"Title Screen pinned 2026.10.09; CC BY 4.0",
            "decision":"research only; do not auto-count game or overwrite review"
        })
    output_path=OUT/"wiiu_regional_eligibility.csv"
    with output_path.open("w",newline="",encoding="utf-8") as f:
        wr=csv.DictWriter(f,fieldnames=list(output[0]))
        wr.writeheader();wr.writerows(output)
    summary={"source_url":URL,"sha256":SHA,"wiiu_archive_releases":len(wiiu),
             "wiiu_source_works":len({r["work_id"] for r in wiiu}),
             "regions":dict(region_distrib),
             "media_by_license":{":".join(x):n for x,n in media_distrib.items()},
             "work_kinds":dict(kind_distrib),
             "catalogue_rows":len(original),"outcomes":dict(counts),
             "unmatched_examples":[r for r in output if r["work_identity_method"]=="UNMATCHED_OR_AMBIGUOUS"][:15],
             "non_us_examples":[r for r in output if r["work_identity_method"]=="TITLE_OTHER_REGION_WORK"][:15],
             "source_credit":"Data: Title Screen (https://titlescreen.org), CC BY 4.0",
             "caution":"Only source-release identity and eligibility classification; no gameplay or licence/compatibility proof"}
    (OUT/"regional_eligibility_summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({k:v for k,v in summary.items() if k not in ("unmatched_examples","non_us_examples")},indent=2))
if __name__=="__main__":main()
