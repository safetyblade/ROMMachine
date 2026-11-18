"""Exercise bounded Citra capture state without GPU or Android dependencies."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/ci/patches/libretrodroid-core-frame-signal.patch"


class RenderProbeTest(unittest.TestCase):
    def test_bounded_and_session_scoped(self):
        section = PATCH.read_text().split(
            "diff --git a/libretrodroid/src/main/cpp/cartdeck_render_probe.h "
        )[1].split("\ndiff --git ", 1)[0]
        header = "\n".join(line[1:] for line in section.splitlines()
                           if line.startswith("+") and not line.startswith("+++")) + "\n"
        fixture = r'''
#include "cartdeck_render_probe.h"
#include <cassert>
int main() {
    auto& probe = cartdeck::RenderProbe::instance();
    probe.reset(false);
    assert(!probe.begin(true, false, 400, 480));
    assert(probe.report().empty());
    probe.reset(true);
    assert(probe.begin(true, false, 400, 480));
    probe.recordGuest(2);
    probe.recordOutput(1);
    assert(!probe.begin(false, true, 0, 0));
    for (int i = 2; i <= 300; ++i) {
        bool sample = probe.begin(true, false, 400, 480);
        assert(sample == (i == 60 || i == 300));
        if (sample) { probe.recordGuest(3); probe.recordOutput(4); }
    }
    assert(probe.report().find("guest_samples=2,3,3") != std::string::npos);
    assert(probe.report().find("frontend_samples=1,4,4") != std::string::npos);
    assert(probe.report().find("duplicate_callbacks=1") != std::string::npos);
    for (int i = 0; i < 5000; ++i) assert(!probe.begin(true, false, 400, 480));
    assert(probe.report().find("video_callbacks=4096;") == 0);
    probe.reset(true);
    assert(probe.report().find("guest_samples=0,0,0") != std::string::npos);
}
'''
        with tempfile.TemporaryDirectory() as tmp:
            directory = pathlib.Path(tmp)
            (directory / "cartdeck_render_probe.h").write_text(header)
            (directory / "fixture.cpp").write_text(fixture)
            subprocess.run(
                ["c++", "-std=c++17", "-Wall", "-Wextra", "-Werror",
                 "fixture.cpp", "-o", "fixture"], cwd=directory, check=True
            )
            subprocess.run([str(directory / "fixture")], check=True)


if __name__ == "__main__":
    unittest.main()
