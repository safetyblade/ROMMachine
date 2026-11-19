"""Compile the real patched scheduler, resampler and frame signal on CI, without Android."""
import pathlib
import shutil
import subprocess
import sys
import tempfile

FIXTURE = r'''
#include "fpssync.h"
#include "resamplers/linearresampler.h"
#include "coreframesignal.h"
#include "cartdeck_av_timing.h"
#include <cassert>
#include <cmath>
#include <limits>
#include <stdexcept>
int main() {
    using namespace libretrodroid;
    const auto nan = std::numeric_limits<double>::quiet_NaN();
    assert(cartdeck::validLaunchTiming(59.945, 44100));
    assert(cartdeck::validLaunchTiming(60, 8000));
    assert(!cartdeck::validLaunchTiming(0, 44100));
    assert(!cartdeck::validLaunchTiming(nan, 44100));
    assert(!cartdeck::validLaunchTiming(60, nan));
    assert(!cartdeck::validLaunchTiming(60, 0));
    assert(cartdeck::validDimensions(400, 480));
    assert(!cartdeck::validDimensions(0, 480));
    assert(!cartdeck::validDimensions(400, 0));
    assert(cartdeck::reportedAspect(0.0f) == -1.0f);
    assert(cartdeck::reportedAspect(static_cast<float>(nan)) == -1.0f);
    FPSSync sync(60, 60);
    assert(sync.advanceFrames() == 1);
    sync.wait();
    for (double rate : {50.0, 59.945, 30.0, 20.0, 60.0}) {
        sync.updateContentRate(rate);
        sync.wait();
        assert(sync.advanceFrames() >= 1);
        sync.reset();
        sync.wait(); // sentinel must not be passed to sleep_until
        assert(sync.advanceFrames() >= 1);
        assert(std::isfinite(sync.getTimeStretchFactor()));
    }
    sync.updateContentRate(nan);
    assert(sync.advanceFrames() == 1);
    FPSSync fallback(60, nan);
    assert(fallback.getTimeStretchFactor() == 1.0);
    bool rejected = false;
    try { FPSSync invalid(0, 60); } catch (const std::invalid_argument&) { rejected = true; }
    assert(rejected);
    LinearResampler resampler;
    int16_t out[10] = {99,99,99,99,99,99,99,99,99,99};
    int16_t in[4] = {10,-10,20,-20};
    resampler.resample(nullptr, 0, out + 1, 4);
    assert(out[0] == 99 && out[9] == 99);
    for (int i=1;i<9;++i) assert(out[i] == 0);
    resampler.resample(in, 2, out + 1, 4);
    assert(out[0] == 99 && out[9] == 99);
    assert(out[1] == 10 && out[2] == -10);
    assert(out[7] == 20 && out[8] == -20);
    CoreFrameSignal frames;
    frames.record(nullptr, 400, 480);
    frames.record(in, 0, 480);
    assert(frames.count() == 0);
    frames.record(in, 400, 480);
    frames.record(reinterpret_cast<void*>(-1), 400, 480);
    assert(frames.count() == 2);
    frames.reset();
    assert(frames.count() == 0);
}
'''


def main(source):
    cpp = pathlib.Path(source) / "libretrodroid/src/main/cpp"
    with tempfile.TemporaryDirectory(prefix="cartdeck-perf03-") as tmp:
        work = pathlib.Path(tmp)
        for name in ("fpssync.cpp", "fpssync.h", "coreframesignal.h", "cartdeck_av_timing.h"):
            shutil.copy2(cpp / name, work / name)
        shutil.copytree(cpp / "resamplers", work / "resamplers")
        (work / "log.h").write_text("#pragma once\n#define LOGI(...) ((void)0)\n")
        (work / "fixture.cpp").write_text(FIXTURE)
        subprocess.run(
            ["c++", "-std=c++17", "-Wall", "-Wextra", "-Werror", "-pthread",
             "fixture.cpp", "fpssync.cpp", "resamplers/linearresampler.cpp", "-o", "fixture"],
            cwd=work, check=True, timeout=120,
        )
        subprocess.run([str(work / "fixture")], check=True, timeout=15)
        print("PERF-03 patched scheduler, resampler, geometry and frame-signal contracts PASS")


if __name__ == "__main__":
    main(sys.argv[1])
