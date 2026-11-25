"""Offline S5 source-patch tests; native Android build is a separate gate."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from hakux_pipeline_cache_commit import REPLACEMENTS, patch


class HakuXPipelinePersistence(unittest.TestCase):
    @staticmethod
    def fixture():
        # Covers every exact pinned source anchor; the native build validates
        # this transform against the real upstream checkout and compiles it.
        return "\n".join(old for _, old, _ in REPLACEMENTS)

    def test_persist_only_after_success_and_retry_failed_writes(self):
        result = patch(self.fixture())
        self.assertIn("hle_atomic_set(&g_pc.saved, snapshot);", result)
        self.assertIn("if (g_file_set_contents(", result)
        self.assertLess(
            result.index("if (g_file_set_contents("),
            result.index("hle_atomic_set(&g_pc.saved, snapshot);"),
        )
        self.assertIn("pipeline cache write failed", result)
        self.assertIn("hle_atomic_read(&g_pc.added)", result)
        self.assertIn("hle_atomic_read(&g_pc.saved)", result)
        self.assertNotIn("g_pc.saved = g_pc.added;", result)

    def test_fail_closed_on_missing_or_duplicate_source(self):
        with self.assertRaisesRegex(ValueError, "pinned|Pinned"):
            patch(self.fixture().replace("static void write_blob(void)", "static int write_blob(void)"))
        with self.assertRaisesRegex(ValueError, "anchor"):
            patch(self.fixture() + "\n" + REPLACEMENTS[-1][1])

    def test_no_changes_to_pipeline_or_render_settings(self):
        result = patch(self.fixture())
        for disallowed in ("vkCreateGraphicsPipelines", "vkCmdDraw", "vkCmdPipelineBarrier"):
            self.assertNotIn(disallowed, result)
        self.assertIn("g_clear_error(&error);", result)


if __name__ == "__main__":
    unittest.main()
