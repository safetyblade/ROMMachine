"""Synthetic regression gates for PC-only input overlay and native symbol audit."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import sys
from test_verify_legacy_core import fake_aarch64_elf

PC = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PC))
from audit_core_binary import audit, REQUIRED

spec = importlib.util.spec_from_file_location("pc_overlay", PC / "native-host/apply_pc_input.py")
overlay = importlib.util.module_from_spec(spec)
spec.loader.exec_module(overlay)


class BinaryAuditTests(unittest.TestCase):
    def test_symbols_defined_and_alignment(self):
        with tempfile.TemporaryDirectory() as tmp:
            core = Path(tmp) / "core.so"
            core.write_bytes(fake_aarch64_elf(16384))
            symbols = "\n".join(f"{n}: 000000 1 FUNC GLOBAL DEFAULT 5 {name}"
                                for n, name in enumerate(sorted(REQUIRED)))
            with patch("audit_core_binary.subprocess.check_output", return_value=symbols):
                self.assertTrue(audit(core, "readelf")["binary_gate_pass"])
                core.write_bytes(fake_aarch64_elf(4096))
                self.assertFalse(audit(core, "readelf")["binary_gate_pass"])

    def test_imported_symbols_not_counted(self):
        with tempfile.TemporaryDirectory() as tmp:
            core = Path(tmp) / "core.so"
            core.write_bytes(fake_aarch64_elf(16384))
            with patch("audit_core_binary.subprocess.check_output",
                       return_value="1: 000000 0 FUNC GLOBAL DEFAULT UND retro_init"):
                self.assertIn("retro_init", audit(core, "readelf")["missing_libretro_exports"])


class OverlayTests(unittest.TestCase):
    def fixture(self, root):
        cpp = root / "libretrodroid/src/main/cpp"
        cpp.mkdir(parents=True)
        for name, edits in overlay.EDITS.items():
            (cpp / name).write_text("\n/* separate anchor */\n".join(before for before, after in edits))
        return cpp

    def test_all_changes_and_repeat_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            cpp = self.fixture(root)
            overlay.apply(root)
            self.assertIn("pollPcInput", (cpp / "input.cpp").read_text())
            with self.assertRaises(ValueError):
                overlay.apply(root)

    def test_anchor_failure_does_not_write_any_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            cpp = self.fixture(root)
            bad = cpp / "environment.cpp"
            bad.write_text("unexpected source")
            snapshot = {p: p.read_bytes() for p in cpp.iterdir()}
            with self.assertRaises(ValueError):
                overlay.apply(root)
            self.assertEqual(snapshot, {p: p.read_bytes() for p in cpp.iterdir()})
