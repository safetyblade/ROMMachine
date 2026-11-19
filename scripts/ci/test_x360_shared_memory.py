"""Compile the pinned actual SharedMemory method bodies, with mocked host services."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parent / "xbox360"))
from repair_shared_memory import patch

PIN = "779680ade075d2d2a4c589c6a8d57a8e26c116c0"
REL = "emulator-core/src/main/cpp/xenia/src/xenia/gpu/shared_memory.cc"


def method(text, name):
    start = text.index(name)
    brace = text.index("{", start)
    level, end = 1, brace + 1
    while level:
        level += (text[end] == "{") - (text[end] == "}")
        end += 1
    return text[start:end]


def fixture(text, patched):
    return r"""
#include <algorithm>
#include <cstdint>
#include <utility>
#include <vector>
namespace xe {
uint32_t bit_count(uint32_t value) { return __builtin_popcount(value); }
bool bit_scan_forward(uint32_t value, uint32_t* index) {
    if (!value) return false;
    *index = __builtin_ctz(value); return true;
}
}
struct FakeLock { int Acquire() const { return 0; } };
struct FakeMemory {
    void EnablePhysicalMemoryAccessCallbacks(uint32_t, uint32_t, bool, bool) {}
};
class SharedMemory {
public:
    static constexpr uint32_t kBufferSize = 512u * 1024u * 1024u;
    uint32_t page_size_log2_;
    unsigned num_system_page_flags_;
    std::vector<uint64_t> valid, gpu, invalid;
    uint64_t *system_page_flags_valid_, *system_page_flags_valid_and_gpu_written_;
    uint64_t *system_page_flags_invalidated_in_submission_;
    bool gpu_written_data_dirty_ = false;
    uint32_t dirty_blocks_ = 0;
    bool memory_invalidation_callback_handle_ = false;
    FakeLock global_critical_region_;
    FakeMemory mem;
    FakeMemory& memory() { return mem; }
    explicit SharedMemory(unsigned log2)
        : page_size_log2_(log2), num_system_page_flags_((kBufferSize >> log2) / 64),
          valid(num_system_page_flags_), gpu(num_system_page_flags_), invalid(num_system_page_flags_),
          system_page_flags_valid_(valid.data()), system_page_flags_valid_and_gpu_written_(gpu.data()),
          system_page_flags_invalidated_in_submission_(invalid.data()) {}
    void MakeRangeValid(uint32_t start, uint32_t length, bool written_by_gpu);
    void SetSystemPageBlocksValidWithGpuDataWritten();
    bool AnyPageInvalidatedSinceSubmissionOpen(const std::pair<uint32_t, uint32_t>*, uint32_t) const;
};
""" + method(text, "void SharedMemory::MakeRangeValid(") + "\n" + method(
        text, "void SharedMemory::SetSystemPageBlocksValidWithGpuDataWritten("
    ) + "\n" + method(
        text, "bool SharedMemory::AnyPageInvalidatedSinceSubmissionOpen("
    ) + r"""
int main() {
    for (unsigned log2 : {12u, 14u}) {
        SharedMemory sm(log2);
        uint32_t page = 1u << log2;
        sm.MakeRangeValid(0, page, true);
        sm.SetSystemPageBlocksValidWithGpuDataWritten();
        if (sm.valid[0] != 1) return 11;
        // CPU-only uploads after a clean refresh must be tracked too.
        sm.MakeRangeValid(page, page, false);
        sm.SetSystemPageBlocksValidWithGpuDataWritten();
        if (sm.valid[0] != 1 || !(sm.invalid[0] & 2)) return 12;
        // Repeated use across chunk boundaries, including the last page.
        for (uint32_t n = 0; n < 10000; ++n) {
            uint32_t block = n % sm.num_system_page_flags_;
            uint32_t address = (block * 64 + 63) << log2;
            sm.MakeRangeValid(address, page, true);
            sm.SetSystemPageBlocksValidWithGpuDataWritten();
            sm.MakeRangeValid(address, page, false);
            sm.SetSystemPageBlocksValidWithGpuDataWritten();
            if (sm.valid[block] != sm.gpu[block]) return 13;
        }
        std::pair<uint32_t,uint32_t> empty{0,0};
        std::pair<uint32_t,uint32_t> out{sm.kBufferSize >> log2, 1};
        std::pair<uint32_t,uint32_t> overflow{1, UINT32_MAX};
        if (sm.AnyPageInvalidatedSinceSubmissionOpen(&empty,1)) return 14;
        if (!sm.AnyPageInvalidatedSinceSubmissionOpen(&out,1)) return 15;
        if (!sm.AnyPageInvalidatedSinceSubmissionOpen(&overflow,1)) return 16;
        std::pair<uint32_t,uint32_t> valid{1,1};
        if (!sm.AnyPageInvalidatedSinceSubmissionOpen(&valid,1)) return 17;
    }
    return 0;
}
"""


class SharedMemoryRegression(unittest.TestCase):
    def test_pinned_cpu_and_gpu_page_transitions_and_bounds(self):
        root = os.environ.get("X360_SOURCE_CHECKOUT")
        if root:
            original = (Path(root) / REL).read_text()
        else:
            url = f"https://raw.githubusercontent.com/rfandango/XenDroid/{PIN}/{REL}"
            original = urllib.request.urlopen(url, timeout=60).read().decode()
        changed = patch(original)
        with tempfile.TemporaryDirectory() as directory:
            root_path = Path(directory)
            for name, source, expected in (("old", original, 12), ("fixed", changed, 0)):
                cpp = root_path / (name + ".cpp")
                exe = root_path / name
                cpp.write_text(fixture(source, name == "fixed"))
                subprocess.run(
                    ["c++", "-std=c++17", "-O2", "-fsanitize=address,undefined",
                     "-fno-sanitize-recover=all", "-fno-pie", "-no-pie", str(cpp), "-o", str(exe)],
                    check=True, timeout=120,
                )
                self.assertEqual(subprocess.run([str(exe)], timeout=30).returncode, expected, name)


if __name__ == "__main__":
    unittest.main()
