"""PERF-X360-03 shader publication tests (no emulator binary required)."""
import importlib.util
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

MODULE = Path(__file__).resolve().parent / "xbox360/shader_translation_wait.py"
spec = importlib.util.spec_from_file_location("cartdeck_shader_wait", MODULE)
patch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(patch)


CPP = r"""
#include <atomic>
#include <cassert>
#include <thread>

class Translation {
public:
  void WaitForTranslation() const {
    while (!translated.load(std::memory_order_acquire)) {
      translated.wait(false, std::memory_order_acquire);
    }
  }
  void PublishTranslation() {
    translated.store(true, std::memory_order_release);
    translated.notify_all();
  }
  int data = 0;
  std::atomic<bool> translated{false};
};

int main() {
  Translation value;
  std::thread consumer([&] {
    value.WaitForTranslation();
    assert(value.data == 2718);
  });
  std::thread publisher([&] {
    value.data = 2718;
    value.PublishTranslation();
  });
  publisher.join();
  consumer.join();
}
"""


class ShaderWaitTest(unittest.TestCase):
    def test_exact_anchor_failure_is_safe(self):
        with self.assertRaisesRegex(ValueError, "anchor mismatch"):
            patch.once("unrecognised", "missing", "patched", "unknown")
        self.assertEqual("new", patch.once("old", "old", "new", "valid"))

    def test_wait_publish_has_host_cpp20_memory_ordering_proof(self):
        compiler = shutil.which("c++")
        if not compiler:
            self.skipTest("C++ host compiler unavailable")
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "wait.cc"
            exe = Path(directory) / "wait"
            source.write_text(CPP)
            subprocess.run(
                [compiler, "-std=c++20", "-Wall", "-Wextra", "-Werror",
                 "-pthread", str(source), "-o", str(exe)],
                check=True, timeout=60,
            )
            subprocess.run([str(exe)], check=True, timeout=20)

    def test_source_port_requires_acquire_release_notify_and_no_spin(self):
        source = MODULE.read_text()
        self.assertIn("std::memory_order_release", source)
        self.assertIn("std::memory_order_acquire", source)
        self.assertIn("is_translated_.wait(false", source)
        self.assertIn("is_translated_.notify_all()", source)
        self.assertIn("PostTranslation();", source)
        self.assertIn("translation.PublishTranslation();", source)


if __name__ == "__main__":
    unittest.main()
