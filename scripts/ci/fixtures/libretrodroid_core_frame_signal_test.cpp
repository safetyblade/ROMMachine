#include <cassert>
#include <thread>
#include "coreframesignal.h"
int main() {
    libretrodroid::CoreFrameSignal frames;
    assert(frames.count() == 0);
    frames.record(nullptr, 640, 480); // Duplicate is not a new core frame.
    frames.record(reinterpret_cast<void*>(-1), 0, 480);
    frames.record(reinterpret_cast<void*>(-1), 640, 0);
    assert(frames.count() == 0);
    int pixels = 0;
    frames.record(&pixels, 320, 240); // Software core frame.
    frames.record(reinterpret_cast<void*>(-1), 640, 480); // Hardware ABI sentinel.
    assert(frames.count() == 2);
    std::thread callback([&] { for (int i=0; i<1000; ++i) frames.record(&pixels, 1, 1); });
    callback.join();
    assert(frames.count() == 1002);
    frames.reset();
    assert(frames.count() == 0); // Next game does not inherit prior success.
}
