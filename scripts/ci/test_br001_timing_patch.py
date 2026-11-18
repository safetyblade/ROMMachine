"""Compile and exercise the AV timing helper carried in the pinned host patch."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
PATCH = ROOT / "scripts/ci/patches/libretrodroid-core-frame-signal.patch"

class AvTimingPatchTest(unittest.TestCase):
    def test_timing_mailbox_and_stereo_bounds(self):
        section = PATCH.read_text().split(
            "diff --git a/libretrodroid/src/main/cpp/cartdeck_av_timing.h "
        )[1]
        header = "\n".join(line[1:] for line in section.splitlines()
                           if line.startswith("+") and not line.startswith("+++")) + "\n"
        fixture = r'''
#include "cartdeck_av_timing.h"
#include <cassert>
#include <limits>
int main() {
    using cartdeck::AvTiming;
    cartdeck::AvTimingMailbox mailbox;
    AvTiming value;
    assert(!mailbox.take(value));
    for (double fps : {50.0, 59.945, 60.0, 30.0, 20.0}) {
        assert(mailbox.publish({fps, 44100.0}));
        assert(mailbox.take(value) && value.fps == fps);
        assert(!mailbox.take(value));
    }
    assert(mailbox.publish({50, 44100}));
    assert(!mailbox.publish({0, 44100}));
    assert(!mailbox.publish({60, 0}));
    assert(!mailbox.publish({std::numeric_limits<double>::infinity(), 44100}));
    assert(!mailbox.publish({60, std::numeric_limits<double>::quiet_NaN()}));
    assert(mailbox.take(value) && value.fps == 50);
    assert((AvTiming{59.945, 44100}.sameFps({59.945, 48000})));
    assert(!(AvTiming{59.945, 44100}.sameFps({60, 44100})));
    assert(!(AvTiming{60, 44100}.sameRate({60, 48000})));
    assert(mailbox.publish({50, 44100}));
    assert(mailbox.publish({60, 48000}));
    assert(mailbox.take(value) && value.sampleRate == 48000);
    assert(mailbox.publish({20, 44100}));
    mailbox.clear();
    assert(!mailbox.take(value));
    auto& stats = cartdeck::PerfCounters::instance();
    stats.reset(false);
    assert(stats.report().empty());
    stats.reset(true);
    stats.recordAudioWrite(100, 80);
    stats.recordAudioRead(100, 90);
    stats.recordVideo(true);
    stats.recordTiming(59.945, 44100);
    stats.recordRun(50000);
    assert(stats.report().find("fifo_dropped=20") != std::string::npos);
    assert(stats.report().find("fifo_missing=10") != std::string::npos);
    assert(stats.report().find("last_fps_milli=59945") != std::string::npos);
    for (int i = 1; i < 4096; ++i) stats.recordRun(8000);
    assert(!stats.shouldSample());
    stats.recordRun(1000);
    assert(stats.report().find("runs=4096;") == 0);
    stats.reset(true);
    assert(stats.report().find("runs=0;") == 0);
    assert(cartdeck::boundedStereoFrames(0, 2048) == 0);
    assert(cartdeck::boundedStereoFrames(200, 2048) == 200);
    assert(cartdeck::boundedStereoFrames(1000000, 2048) == 1024);
    assert(cartdeck::boundedStereoFrames(50, 0) == 0);
    assert(cartdeck::boundedStereoFrames(std::numeric_limits<double>::infinity(), 2048) == 0);
}
'''
        with tempfile.TemporaryDirectory() as tmp:
            directory = pathlib.Path(tmp)
            (directory / "cartdeck_av_timing.h").write_text(header)
            (directory / "fixture.cpp").write_text(fixture)
            subprocess.run(
                ["c++", "-std=c++17", "-Wall", "-Wextra", "-Werror", "-pthread",
                 "fixture.cpp", "-o", "fixture"], cwd=directory, check=True
            )
            subprocess.run([str(directory / "fixture")], check=True)

if __name__ == "__main__":
    unittest.main()
