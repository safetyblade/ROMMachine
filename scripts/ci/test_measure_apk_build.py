"""Timing evidence must retain failures and reject changed warm outputs."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("measure_apk_build.py").resolve()


class ApkTimingEvidenceTests(unittest.TestCase):
    def run_case(self, code):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "--report", "report.json", "--repeat", "true",
             "--", sys.executable, "-c", code], cwd=root,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        return result, json.loads((root / "report.json").read_text())

    def test_failure_stops_before_warm_build_and_records_exit(self):
        result, report = self.run_case("raise SystemExit(7)")
        self.assertEqual(result.returncode, 7)
        self.assertEqual(len(report["measurements"]), 1)
        self.assertEqual(report["measurements"][0]["exit_code"], 7)
        self.assertNotIn("apk_sha256", report["measurements"][0])

    def test_repeat_requires_identical_apk(self):
        result, report = self.run_case(
            "from pathlib import Path; "
            "p=Path('lemuroid-app/build/outputs/apk/freeBundle/debug/fixture.apk'); "
            "p.parent.mkdir(parents=True,exist_ok=True); "
            "p.write_bytes(b'test fixture only')")
        self.assertEqual(result.returncode, 0)
        self.assertEqual(len(report["measurements"]), 2)
        self.assertTrue(report["same_apk_sha256"])

    def test_changed_warm_apk_is_rejected(self):
        result, report = self.run_case(
            "from pathlib import Path; "
            "p=Path('lemuroid-app/build/outputs/apk/freeBundle/debug/fixture.apk'); "
            "p.parent.mkdir(parents=True,exist_ok=True); "
            "p.write_bytes((p.read_bytes() if p.exists() else b'test fixture') + b'x')")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(report["same_apk_sha256"])


if __name__ == "__main__":
    unittest.main()
