"""Synthetic tests for verify_pcr03.py (PC-R03 preparation).

Run: python3 -m unittest spikes.pc.tests.test_verify_pcr03
     (or: python3 spikes/pc/tests/test_verify_pcr03.py)
All fixtures are synthetic; no DOS, game or third-party content.
"""
import pathlib
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
import verify_pcr03 as v  # noqa: E402
import make_synthetic_dosz as builder  # noqa: E402  (path added by verify_pcr03)

GOOD_GS = 'GameSystem(SystemID.DOS, "DOS", listOf(SystemCoreConfig(CoreID.DOSBOX_PURE)), uniqueExtensions = listOf("dosz"))'
GOOD_CI = 'DOSBOX_PURE("libdosbox_pure_libretro_android.so")'


def _write_repo(root: pathlib.Path, gs_text: str, ci_text: str) -> pathlib.Path:
    gs = root / v.GAME_SYSTEM
    ci = root / v.CORE_ID
    gs.parent.mkdir(parents=True, exist_ok=True)
    gs.write_text(gs_text)
    ci.write_text(ci_text)
    return root


class RegistryTests(unittest.TestCase):
    def test_positive(self):
        with tempfile.TemporaryDirectory() as td:
            out = v.check_registry(_write_repo(pathlib.Path(td), GOOD_GS, GOOD_CI))
        self.assertTrue(out["dos_scans_dosz"])
        self.assertTrue(out["dos_routes_dosbox_pure"])
        self.assertTrue(out["coreid_names_audited_binary"])

    def test_negative_extension(self):
        with tempfile.TemporaryDirectory() as td:
            out = v.check_registry(_write_repo(pathlib.Path(td), GOOD_GS.replace('"dosz"', '"iso"'), GOOD_CI))
        self.assertTrue(out["dos_block_found"])
        self.assertFalse(out["dos_scans_dosz"])

    def test_negative_core(self):
        with tempfile.TemporaryDirectory() as td:
            out = v.check_registry(_write_repo(pathlib.Path(td), GOOD_GS.replace("DOSBOX_PURE", "OTHER"), GOOD_CI))
        self.assertFalse(out["dos_routes_dosbox_pure"])


class PackageTests(unittest.TestCase):
    def test_roundtrip(self):
        with tempfile.TemporaryDirectory() as td:
            pkg = builder.build(pathlib.Path(td) / "synthetic_hello.dosz")
            out = v.check_package(pkg)
        self.assertTrue(out["zip_magic"])
        self.assertTrue(out["entries_exact"])
        self.assertTrue(out["entries_at_root"])
        self.assertTrue(out["bat_marker"])
        self.assertTrue(out["txt_marker"])
        self.assertTrue(out["zip_integrity"])

    def test_rejects_extra_entry(self):
        with tempfile.TemporaryDirectory() as td:
            pkg = pathlib.Path(td) / "bad.dosz"
            with zipfile.ZipFile(pkg, "w") as z:
                z.writestr("HELLO.BAT", b"x")
                z.writestr("HELLO.TXT", b"x")
                z.writestr("EXTRA.EXE", b"MZ")
            out = v.check_package(pkg)
        self.assertFalse(out["entries_exact"])

    def test_rejects_non_zip(self):
        with tempfile.TemporaryDirectory() as td:
            pkg = pathlib.Path(td) / "bad.dosz"
            pkg.write_bytes(b"not a zip")
            out = v.check_package(pkg)
        self.assertFalse(out["zip_magic"])
        self.assertFalse(out["zip_integrity"])

    def test_deterministic(self):
        self.assertTrue(v.check_deterministic()["deterministic_rebuild"])


if __name__ == "__main__":
    unittest.main()
