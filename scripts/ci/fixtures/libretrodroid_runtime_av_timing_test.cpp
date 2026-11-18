#include "runtimeavtiming.h"
#include <cassert>
#include <cmath>
#include <limits>
using libretrodroid::RuntimeAvRate;
using libretrodroid::RuntimeAvTiming;
int main() {
    RuntimeAvTiming timing;
    RuntimeAvRate rate;
    timing.arm({59.9453, 44100.0});
    assert(!timing.take(rate));
    assert(timing.publish({59.9453, 44100.0}));
    assert(!timing.take(rate));
    for (double fps : {50.0, 30.0, 20.0, 60.0, 59.9453}) {
        assert(timing.publish({fps, 44100.0}));
        assert(timing.take(rate));
        assert(rate.fps == fps && rate.sampleRate == 44100.0);
        assert(!timing.take(rate));
    }
    assert(!timing.publish({0, 44100}));
    assert(!timing.publish({std::numeric_limits<double>::quiet_NaN(), 44100}));
    assert(!timing.publish({30, 48000})); // unsupported format, no fake acknowledgement
    assert(!timing.take(rate));
    assert(timing.publish({30, 44100}));
    assert(timing.publish({50, 44100})); // latest wins without queue growth
    assert(timing.take(rate) && rate.fps == 50);
    timing.reset();
    assert(!timing.take(rate));
    timing.arm({60, 48000});
    assert(timing.publish({30, 48000}));
    assert(timing.take(rate) && rate.fps == 30 && rate.sampleRate == 48000);
}
