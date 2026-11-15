#!/usr/bin/env python3
"""GEM-W03R6: identify Wii U commercial works missing from existing GEM seeds.

No master mutations. Pin SHA-256, preserve regional and original-platform
evidence, classify likely Virtual Console/legacy releases for separate review.
"""
from __future__ import annotations
import csv,hashlib,io,json,re,unicodedata,urllib.request,zipfile
from collections import defaultdict,Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parent;OUT=ROOT/"generated";OUT.mkdir(exist_ok=True)
ZIP=Path("/tmp/gamedb-2026.10.09-csv.zip")
URL="https://github.com/hmofet/titlescreen-data/releases/download/v2026.10.09/gamedb-2026.10.09-csv.zip"
SHA="9797a394d465fe79e7d96a9cd08698ae9ce18bd842b304ffe65d27d1c7fcca47"
def norm(s):
 x=unicodedata.normalize("NFKD",str(s or ""))
 x="".join(c for c in x if not unicodedata.combining(c)).lower()
 return re.sub("[^a-z0-9]","",x)
def read_csv_zip(zf,name):
 with zf.open(name) as fh:return list(csv.DictReader(io.TextIOWrapper(fh,encoding="utf-8-sig",newline="")))
def main():
 if not ZIP.exists() or hashlib.sha256(ZIP.read_bytes()).hexdigest()!=SHA:
  h=hashlib.sha256();req=urllib.request.Request(URL,headers={"User-Agent":"CARTDECK-GEM-W03R6-CCBY4"})
  with urllib.request.urlopen(req,timeout=180) as f,ZIP.open("wb") as out:
   while chunk:=f.read(1<<20):h.update(chunk);out.write(chunk)
  if h.hexdigest()!=SHA:raise RuntimeError("Pinned archive hash mismatch")
 with zipfile.ZipFile(ZIP) as z:
  works={w["id"]:w for w in read_csv_zip(z,"works.csv")}
  releases=[r for r in read_csv_zip(z,"releases.csv") if r.get("platform_id")=="wiiu" and r.get("licensed")=="commercial"]
 represented=set()
 with (OUT/"wiiu_regional_eligibility.csv").open(newline="",encoding="utf-8") as f:
  matches=list(csv.DictReader(f))
 for m in matches:
  represented.update(x.strip() for x in m.get("work_ids","").split("|") if x.strip())
 listed={norm(m["original_title"]) for m in matches}
 bywork=defaultdict(list)
 for r in releases:bywork[r["work_id"]].append(r)
 rows=[];counts=Counter();regions=Counter()
 legacy={"nes","snes","n64","gb","gbc","gba","nds","wii","ps1","ps2","psp","pce","genesis","megadrive","sms","gg","arcade"}
 for work,rs in sorted(bywork.items()):
  if work in represented:continue
  w=works.get(work,{})
  kind=w.get("kind","unknown")
  if kind!="game":
   bucket="NON_GAME_ADDON_OR_COMPILATION"
  elif (w.get("first_platform_id") or "") in legacy:
   bucket="LEGACY_VC_OR_PORT_REVIEW"
  else:
   bucket="WIIU_OR_MODERN_CROSS_PLATFORM_GAME_REVIEW"
  regions_us=sorted({r["region"] for r in rs if r["region"] in {"US","USA"}})
  regs=sorted({r["region"] for r in rs})
  region_group="US_PRESENT" if regions_us else ("JP_ONLY" if regs==["JP"] else "EU_ONLY" if regs==["EU"] else "OTHER_NON_US_ONLY")
  titles=sorted({r.get("title","") for r in rs})
  candidate_title=titles[0] if titles else w.get("title","")
  name_collision=norm(candidate_title) in listed
  if name_collision:bucket="TITLE_COLLISION_REVIEW"
  counts[bucket]+=1;counts[region_group]+=1
  rows.append({
   "work_id":work,"display_title":candidate_title,"kind":kind,"bucket":bucket,
   "release_regions":" | ".join(regs),"us_release_present":bool(regions_us),
   "regional_group":region_group,"release_titles":" | ".join(titles)[:450],
   "release_ids":" | ".join(r["id"] for r in rs)[:600],
   "us_publishers":" | ".join(sorted({r.get("publisher","") for r in rs if r["region"] in {"US","USA"} and r.get("publisher")})),
   "us_years":" | ".join(sorted({r.get("year","") for r in rs if r["region"] in {"US","USA"} and r.get("year")})),
   "genres":" | ".join(sorted({r.get("genre","") for r in rs if r.get("genre")})),
   "first_platform_id":w.get("first_platform_id",""),"first_platforms":w.get("first_platforms",""),
   "title_collides_existing_gem":name_collision,
   "wiiu_commercial_release_count":len(rs),
   "action":"Research and reconcile native Wii U vs Virtual Console/legacy; no automatic import"
  })
 with (OUT/"wiiu_unrepresented_works.csv").open("w",newline="",encoding="utf-8") as f:
  writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
 summary={"archive_sha256":SHA,"license":"Data: Title Screen (https://titlescreen.org), CC BY 4.0",
          "source_wiiu_commercial_work_count":len(bywork),"gem_represented_unique_work_ids":len(represented),
          "work_candidates_not_represented":len(rows),"bucket_counts":dict(counts),
          "sample_us_original":[r for r in rows if r["bucket"]=="WIIU_OR_MODERN_CROSS_PLATFORM_GAME_REVIEW" and r["regional_group"]=="US_PRESENT"][:18],
          "sample_japan_only":[r for r in rows if r["regional_group"]=="JP_ONLY"][:12],
          "decision":"Staging only. Wii U native eligibility, title identity and licensing must be validated before GEM master promotion."}
 (OUT/"wiiu_coverage_gap_summary.json").write_text(json.dumps(summary,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
 print(json.dumps({k:v for k,v in summary.items() if not k.startswith("sample")},indent=2))
if __name__=="__main__":main()
