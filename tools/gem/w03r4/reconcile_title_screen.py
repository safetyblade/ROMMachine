#!/usr/bin/env python3
"""GEM-W03R4: pinned, research-only Wii U title/release reconciliation.
The script never edits Google Sheets, ROM files, player data, ratings or APKs.
"""
from __future__ import annotations
import csv, hashlib, io, json, re, unicodedata, urllib.request, zipfile
from collections import Counter, defaultdict
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/"generated"; OUT.mkdir(parents=True, exist_ok=True)
ARCHIVE=Path("/tmp/gamedb-2026.10.09-csv.zip")
URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-csv.zip"
EXPECTED_SHA="9797a394d465fe79e7d96a9cd08698ae9ce18bd842b304ffe65d27d1c7fcca47"
CREDIT="Data: Title Screen (https://titlescreen.org), CC BY 4.0"
FILENAME="wiiu_reconciliation.csv"

def norm(s):
    s=unicodedata.normalize("NFKD",str(s or "").replace("&"," and "))
    s="".join(c for c in s if not unicodedata.combining(c)).casefold()
    s=re.sub(r"(?:, )the$", "", s) if s.endswith(", the") else s
    return re.sub("[^a-z0-9]+","",s)

def genus(s):
    return {
        "Role-playing":"RPG", "Role-playing game":"RPG", "Platformer":"Platformer",
        "Action-adventure":"Action-adventure", "Beat 'em up":"Fighting",
        "Board, card and tabletop":"Board / Tabletop", "Music":"Music / Rhythm",
        "Party":"Party", "Educational":"Educational", "Gambling":"Gambling",
        "Quiz and trivia":"Trivia", "Racing":"Racing", "Puzzle":"Puzzle",
        "Sports":"Sports", "Shooter":"Shooter", "Simulation":"Simulation",
        "Strategy":"Strategy", "Action":"Action", "Fighting":"Fighting",
        "Adventure":"Adventure", "Sandbox":"Simulation",
        "Miscellaneous":"Miscellaneous"
    }.get(s, s)

def fetch():
    h=hashlib.sha256()
    req=urllib.request.Request(URL,headers={"User-Agent":"CARTDECK-GEM-W03R4-CCBY4/2"})
    with urllib.request.urlopen(req,timeout=180) as response,ARCHIVE.open("wb") as out:
        while part:=response.read(1024*1024):
            h.update(part);out.write(part)
    if h.hexdigest()!=EXPECTED_SHA:
        raise RuntimeError("SHA256 mismatch; refuse source ingestion")
    return {"source":URL,"sha256":h.hexdigest(),"bytes":ARCHIVE.stat().st_size,
            "license":"CC BY 4.0","attribution":CREDIT}

def read_csv(zf,name):
    with zf.open(name) as f:
        return list(csv.DictReader(io.TextIOWrapper(f,encoding="utf-8-sig",errors="replace",newline="")))

def main():
    proof=fetch()
    master=json.loads((ROOT/"wiiu_master_snapshot.json").read_text(encoding="utf-8"))["records"]
    with zipfile.ZipFile(ARCHIVE) as z:
        # Source is the exact, SHA-verified release CSV; no guessing from titles
        releases=read_csv(z,"releases.csv")
        works={w["id"]:w for w in read_csv(z,"works.csv")}
        table_inventory={n:len(read_csv(z,n)) for n in ["releases.csv","works.csv"]}
    us_by_name=defaultdict(list);all_by_name=defaultdict(list);us_by_serial=defaultdict(list)
    native=0
    for r in releases:
        if (r.get("platform_id") or "")!="wiiu":
            continue
        native+=1
        if (r.get("licensed") or "").lower()!="commercial":
            continue
        key=norm(r.get("title"))
        all_by_name[key].append(r)
        if (r.get("region") or "").upper() not in ("US","USA"):
            continue
        us_by_name[key].append(r)
        code=(r.get("serial") or "").upper()
        for token in re.findall(r"WUP-[PN]-[A-Z0-9]{4}",code):
            us_by_serial[token].append(r)
    out=[];tallies=Counter()
    for rec in master:
        label=rec["title"];key=norm(label)
        product=(rec.get("product_code") or "").upper()
        named=us_by_name.get(key,[])
        coded=us_by_serial.get(product,[]) if product else []
        # Strongest evidence first; identity joins outrank title joins.
        identical=[r for r in coded if norm(r.get("title"))==key]
        if identical:
            candidates=identical;match_kind="ID_AND_TITLE_US"
        elif named:
            candidates=named;match_kind="TITLE_EXACT_US"
        elif coded:
            candidates=coded;match_kind="ID_ONLY_TITLE_CONFLICT"
        elif all_by_name.get(key):
            candidates=all_by_name[key];match_kind="EXACT_OTHER_REGION_ONLY"
        else:
            candidates=[];match_kind="UNMATCHED"
        work_ids=sorted({r.get("work_id","") for r in candidates if r.get("work_id")})
        genres=sorted({(r.get("genre") or works.get(r.get("work_id"),{}).get("genre") or "").strip()
                       for r in candidates if (r.get("genre") or works.get(r.get("work_id"),{}).get("genre") or "").strip()})
        pubs=sorted({(r.get("publisher") or "").strip() for r in candidates if (r.get("publisher") or "").strip()})
        years=sorted({(r.get("year") or "").strip() for r in candidates if (r.get("year") or "").strip()})
        good=match_kind in {"ID_AND_TITLE_US","TITLE_EXACT_US"} and len(work_ids)==1
        if good and len(genres)>1:
            match_kind="CONFLICT_SOURCE_GENRE";good=False
        if good and len(pubs)>1:
            # Same work different publishers needs release-specific disambiguation.
            # Keep genre eligible; publisher remains blank until manual confirmation.
            pubs=[]
        if good and len(years)>1:
            years=[]
        genre=genus(genres[0]) if good and len(genres)==1 else ""
        pub=pubs[0] if good and len(pubs)==1 else ""
        year=years[0] if good and len(years)==1 else ""
        previous=(rec.get("normalized_genre") or "").strip()
        verdict=("SOURCE_MATCH_ID" if good and match_kind=="ID_AND_TITLE_US" else
                 "SOURCE_MATCH_TITLE" if good else "REVIEW_REQUIRED")
        disagreement=bool(genre and previous and previous!="Unknown" and genus(previous)!=genre)
        same=bool(genre and previous and genus(previous)==genre)
        outcome={
            "master_row":rec["row"],"catalogue_id":rec["catalogue_id"],"title":label,
            "product_code":product,"source_match":match_kind,"match_verdict":verdict,
            "source_title":" | ".join(sorted({r.get("title","") for r in candidates}))[:250],
            "source_work_id":" | ".join(work_ids),
            "source_release_ids":" | ".join(sorted({r.get("id","") for r in candidates})),
            "source_genre":genres[0] if len(genres)==1 else "",
            "candidate_normalized_genre":genre,
            "existing_normalized_genre":previous,"genre_disagreement":disagreement,"genre_agreement":same,
            "source_publisher":pub,"source_us_release_year":year,
            "source_region":" | ".join(sorted({r.get("region","") for r in candidates})),
            "release_class":"commercial" if candidates else "",
            "existing_publisher":rec.get("publisher",""),"existing_release_year":rec.get("year",""),
            "research_only":True
        }
        tallies[verdict]+=1
        if genre:tallies["genre_candidates"]+=1
        if genre and (not previous or previous=="Unknown"):tallies["fills_unknown_genre"]+=1
        if same:tallies["genre_agreements"]+=1
        if disagreement:tallies["genre_disagreements"]+=1
        if pub:tallies["publisher_candidates"]+=1
        if year:tallies["us_year_candidates"]+=1
        out.append(outcome)
    with (OUT/FILENAME).open("w",encoding="utf-8",newline="") as f:
        writer=csv.DictWriter(f,fieldnames=list(out[0]))
        writer.writeheader();writer.writerows(out)
    summary={"run":"GEM-W03R4 pinned Title Screen archive v2","archive":proof,
             "platform_releases_all_classes":native,
             "source_row_counts":table_inventory,"master_candidates":len(master),
             "summary_counts":dict(tallies),
             "assurance":"Source-matched candidates require editorial review. Original source genre and manual ratings untouched.",
             "license_notice":CREDIT,
             "output_file":FILENAME}
    (OUT/"verification_summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(summary,indent=2))
if __name__=="__main__":main()
