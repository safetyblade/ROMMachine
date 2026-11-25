"""Offline source-bound test for XenDroid+ audio-yield fix."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from xbox360.remove_audio_stub_yield import OLD, NEW, patch


class XeniaAudioYieldTests(unittest.TestCase):
    def test_removes_only_unnecessary_query_yield(self):
        original = OLD + "\n  *out_ptr = 0;\n  return X_ERROR_SUCCESS;\n}"
        result = patch(original)
        self.assertIn(NEW, result)
        self.assertNotIn("MaybeYield", result)
        self.assertIn("  *out_ptr = 0;", result)
        self.assertIn("  return X_ERROR_SUCCESS;", result)

    def test_rejects_source_drift_or_double_application(self):
        with self.assertRaisesRegex(ValueError, "anchor"):
            patch(NEW)
        with self.assertRaisesRegex(ValueError, "anchor"):
            patch(OLD + "\n" + OLD)

    def test_does_not_remove_other_scheduler_yields(self):
        other = "void UnrelatedHostScheduler() { xe::threading::MaybeYield(); }"
        result = patch(OLD + "\n" + other)
        self.assertIn(other, result)


if __name__ == "__main__":
    unittest.main()
