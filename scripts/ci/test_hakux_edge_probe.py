"""XB-01/02: verify pinned source transform and corner/edge addressing."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from hakux_edge_probe import OLD, NEW, patch


def sample(width, height, index):
    side, ring, step = index // 16, (index % 16) // 8, index % 8
    x = step * (width - 1) // 7
    y = step * (height - 1) // 7
    ix = 1 if ring and width > 1 else 0
    iy = 1 if ring and height > 1 else 0
    px = ix if side == 2 else width - 1 - ix if side == 3 else x
    py = iy if side == 0 else height - 1 - iy if side == 1 else y
    return px, py


class EdgeProbeTests(unittest.TestCase):
    def test_exact_source_only_and_no_normal_render_change(self):
        fixture = "header\nstatic void probe_copy(\n" + OLD + "\nfooter"
        output = patch(fixture)
        self.assertEqual(output, fixture.replace(OLD, NEW))
        self.assertIn("VkBufferImageCopy regions[64]", output)
        self.assertNotIn("vkCmdDraw", NEW)
        for old in ("VK_FILTER_LINEAR", "vkQueueSubmit2", "vkCmdPipelineBarrier"):
            self.assertNotIn(old, NEW)
        with self.assertRaisesRegex(ValueError, "anchor"):
            patch(fixture + OLD)
        with self.assertRaisesRegex(ValueError, "anchor"):
            patch(fixture.replace(OLD, ""))

    def test_outer_and_inner_sample_both_stages_and_small_images(self):
        for width, height in ((640, 480), (720, 480), (1080, 2400), (1, 1), (2, 2)):
            samples = [sample(width, height, i) for i in range(64)]
            self.assertTrue(all(0 <= x < width and 0 <= y < height for x, y in samples))
            for side in range(4):
                outer = samples[side * 16:side * 16 + 8]
                inner = samples[side * 16 + 8:side * 16 + 16]
                for x, y in outer:
                    self.assertEqual(0 if side == 2 else width - 1 if side == 3 else None,
                                     x if side in (2, 3) else None)
                    self.assertEqual(0 if side == 0 else height - 1 if side == 1 else None,
                                     y if side in (0, 1) else None)
                if width > 2 and height > 2:
                    self.assertNotEqual(outer, inner)

    def test_sampling_is_sparse_not_a_claim_of_full_border_coverage(self):
        self.assertEqual(64, len([sample(640, 480, i) for i in range(64)]))
        self.assertEqual((0, 0), sample(640, 480, 0))
        self.assertEqual((639, 479), sample(640, 480, 23))


if __name__ == "__main__":
    unittest.main()
