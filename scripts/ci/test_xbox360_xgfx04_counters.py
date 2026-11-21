"""Compile and execute the opt-in XGFX-04 diagnostics against the real header.

The tests are deliberately independent of the Android emulator binary: no
game-specific configuration, graphics policy, guest saves or network access.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


HEADER_ROOT = Path(__file__).resolve().parents[2]
SOURCE = r"""
#include <cassert>
#include <cstring>
#include "scripts/ci/xbox360/cartdeck_performance.h"

int main() {
  using namespace cartdeck::perf;
  // Normal gameplay must not record any of the opt-in counters.
  set_capture_enabled(false);
  count(xgfx04_deferred_pipeline_unready);
  count(xgfx04_replay_dropped_indexed_draw);
  count(xgfx04_gpu_to_cpu_blocks);
  assert(xgfx04_deferred_pipeline_unready.load() == 0);
  assert(xgfx04_replay_dropped_indexed_draw.load() == 0);
  assert(xgfx04_gpu_to_cpu_blocks.load() == 0);
  // Capturing does not set any renderer cvars or pause the guest.
  set_capture_enabled(true);
  count(xgfx04_deferred_pipeline_unready);
  count(xgfx04_replay_dropped_indexed_draw);
  count(xgfx04_replay_submitted_indexed_draw);
  count(xgfx04_gpu_range_writes);
  count(xgfx04_gpu_to_cpu_blocks);
  assert(xgfx04_deferred_pipeline_unready.load() == 1);
  assert(xgfx04_replay_dropped_indexed_draw.load() == 1);
  assert(xgfx04_gpu_to_cpu_blocks.load() == 1);
  char report[16384] = {};
  append(report, sizeof(report));
  assert(std::strstr(report, "xgfx04_schema=1 capture=ON") != nullptr);
  assert(std::strstr(report, "deferred_pipeline_unready=1") != nullptr);
  assert(std::strstr(report, "replay_dropped_indexed_draw=1") != nullptr);
  assert(std::strstr(report, "gpu_range_writes=1") != nullptr);
  assert(std::strstr(report, "gpu_to_cpu_blocks=1") != nullptr);
  assert(std::strstr(report, "xgfx_pipeline_queue_wait") != nullptr);
  assert(std::strstr(report, "guest_simulation_rate=UNAVAILABLE") != nullptr);
  set_capture_enabled(false);
  count(xgfx04_deferred_pipeline_unready);
  assert(xgfx04_deferred_pipeline_unready.load() == 1);
  return 0;
}
"""


class Xbox360Xgfx04CounterTest(unittest.TestCase):
    def test_opt_in_counters_compile_and_report_without_graphics_changes(self):
        compiler = shutil.which("c++")
        if compiler is None:
            self.skipTest("No host C++ compiler available")
        with tempfile.TemporaryDirectory(prefix="cartdeck-xgfx04-") as tmp:
            source = Path(tmp) / "counters.cc"
            binary = Path(tmp) / "counters"
            source.write_text(SOURCE)
            subprocess.run(
                [compiler, "-std=c++20", "-O1", "-pthread", "-I", str(HEADER_ROOT),
                 str(source), "-o", str(binary)],
                check=True, timeout=120,
            )
            subprocess.run([str(binary)], check=True, timeout=30)


if __name__ == "__main__":
    unittest.main()
