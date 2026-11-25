"""Regression tests for the read-only Xbox and Xbox 360 benchmark comparator."""
import dataclasses
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from xbox_perf_benchmark import Run, compare, metrics


REPORT = (
    "performance_schema=1 timing=CPU_MONOTONIC "
    "unique_completed_frames=UNAVAILABLE guest_simulation_rate=UNAVAILABLE\n"
    "guest_swap_interval count=512 p50_ms=27.000000 p95_ms=39.000000 "
    "p99_ms=42.000000 total_ms=15000.000 max_ms=50.000\n"
)


class Benchmarks(unittest.TestCase):
    def run_fixture(self, report=REPORT, integrity="PASS"):
        return Run.from_mapping({
            "console": "XBOX360",
            "title_id": "4541083B",
            "title_sha256": "a" * 64,
            "scene": "same match, 10-minute mark",
            "native_build_id": "native-build-a",
            "device": "SM-S721B",
            "driver": "stock Vulkan",
            "settings": "1x NORMAL",
            "cache_state": "warm",
            "run_seconds": 180,
            "report": report,
            "integrity": integrity,
        })

    def test_unverified_graphics_cannot_pass(self):
        result = compare(self.run_fixture(integrity="NOT_TESTED"), self.run_fixture())
        self.assertEqual("NOT_ACCEPTED", result["result"])

    def test_present_counters_are_not_fps(self):
        candidate = self.run_fixture(REPORT.replace("p95_ms=39.", "p95_ms=35."))
        result = compare(self.run_fixture(), candidate)
        self.assertEqual("DIAGNOSTIC_ONLY", result["result"])
        self.assertEqual(False, result["fps_verified"])
        self.assertEqual(10.26, result["change_pct"])

    def test_different_scene_refused(self):
        with self.assertRaisesRegex(ValueError, "fixtures"):
            compare(self.run_fixture(), dataclasses.replace(self.run_fixture(), scene="other"))

    def test_changed_renderer_mode_refused(self):
        with self.assertRaisesRegex(ValueError, "settings"):
            compare(self.run_fixture(), dataclasses.replace(self.run_fixture(), settings="SKIP_OFF"))

    def test_nonfinite_and_duplicate_counters_refused(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            metrics(REPORT + REPORT)
        with self.assertRaisesRegex(ValueError, "Malformed"):
            metrics(REPORT.replace("p50_ms=27.", "p50_ms=47."))

    def test_no_guest_samples_is_insufficient(self):
        result = compare(self.run_fixture(REPORT.replace("count=512", "count=0")), self.run_fixture())
        self.assertEqual("INSUFFICIENT", result["result"])

    def test_wrong_title_hash_rejected(self):
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            Run.from_mapping(dataclasses.asdict(self.run_fixture()) | {"title_sha256": "not-a-hash"})

    def test_xbox_host_report_does_not_create_fake_native_fps(self):
        xbox = dataclasses.replace(self.run_fixture(), console="XBOX", report="xbox_host_perf_schema=1")
        result = compare(xbox, xbox)
        self.assertEqual("INSUFFICIENT", result["result"])


if __name__ == "__main__":
    unittest.main()
