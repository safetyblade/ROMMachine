"""Offline tests for GEM-02B source/DAT importer (stdlib only)."""
from __future__ import annotations

import csv
import json
import tempfile
import unittest
from pathlib import Path

from gem_arcade_catalogue import build, parse_dat

MAME = '''<?xml version="1.0"?><mame>
<game name="neogeo" runnable="no"><description>Neo Geo BIOS</description></game>
<game name="kof98" romof="neogeo" sourcefile="neogeo.c"><description>The King of Fighters '98</description><year>1998</year><manufacturer>SNK</manufacturer><video orientation="horizontal"/><driver status="good"/></game>
<game name="kof98a" cloneof="kof98" romof="neogeo"><description>The King of Fighters '98 (set 2)</description><year>1998</year><manufacturer>SNK</manufacturer></game>
<game name="1944"><description>1944: The Loop Master</description><year>2000</year><manufacturer>Capcom</manufacturer></game>
<game name="proto"><description>Unknown Prototype</description><year>1999</year></game>
</mame>'''
FBNEO = '''<?xml version="1.0"?><datafile>
<game name="1944" sourcefile="capcom/d_cps2.cpp"><description>1944: The Loop Master (Europe)</description><year>2000</year><manufacturer>Capcom</manufacturer><video orientation="vertical"/></game>
<game name="1944a" cloneof="1944" romof="1944"><description>1944: alternate set</description><year>2000</year></game>
<game name="bios" isbios="yes"><description>Board BIOS</description></game>
<game name="romdep" romof="bios"><description>Real game with BIOS dependency</description><year>1992</year></game>
</datafile>'''


class TestGemArcadeCatalogue(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.mame = self.root / "mame.xml"
        self.fbneo = self.root / "fbneo.dat"
        self.mame.write_text(MAME, encoding="utf-8")
        self.fbneo.write_text(FBNEO, encoding="utf-8")

    def test_bios_romof_not_clone(self):
        parents, variants, excluded, stats = parse_dat("MAME 2003-Plus", self.mame)
        self.assertEqual({p["shortname"] for p in parents}, {"kof98", "1944", "proto"})
        self.assertEqual([p["shortname"] for p in variants], ["kof98a"])
        self.assertEqual([p["shortname"] for p in excluded], ["neogeo"])
        self.assertEqual(next(p for p in parents if p["shortname"] == "kof98")["romof_dependency"], "neogeo")
        self.assertEqual(stats["parent_candidates"], 3)

    def test_full_manifest_and_no_game_binary_metadata(self):
        existing = self.root / "existing.csv"
        existing.write_text("Game ID\nM3P:kof98\n", encoding="utf-8")
        out = self.root / "out"
        info = build(self.mame, self.fbneo, out, existing)
        counts = info["counts"]
        self.assertEqual(counts["parent_candidates"], 5)
        self.assertEqual(counts["new_standard_parent_candidates"], 3)
        self.assertEqual(counts["already_catalogued"], 1)
        self.assertEqual(counts["hold_parent_candidates"], 1)
        self.assertEqual(counts["clone_variants"], 2)
        self.assertEqual(counts["excluded_bios_devices"], 2)
        self.assertEqual(counts["cross_core_shortname_candidates"], 1)
        self.assertFalse(info["roms_or_bios_included"])
        self.assertFalse(info["catalogue_mutated"])
        with (out / "new_parent_candidates.csv").open(newline="", encoding="utf-8") as f:
            rows = list(csv.DictReader(f))
        self.assertTrue(any(r["catalogue_id"] == "FBN:romdep" for r in rows))
        self.assertFalse(any("download" in c.lower() or c.lower().startswith("rom_name") for c in rows[0]))
        self.assertTrue((out / "manifest.json").exists())
        self.assertEqual(json.loads((out / "manifest.json").read_text())["counts"], counts)


if __name__ == "__main__":
    unittest.main()
