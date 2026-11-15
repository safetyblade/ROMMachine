#pragma once
#include <cstdint>

// Frame deadlines use the core's timing, not a 60/120Hz Android display clock.
class FramePacer {
public:
    void reset(uint64_t now) { deadline_ = now; }
    uint64_t next(uint64_t now, uint64_t period) {
        if (!deadline_ || now > deadline_ + period * 4) deadline_ = now;
        deadline_ += period;
        return deadline_;
    }
private:
    uint64_t deadline_ = 0;
};
