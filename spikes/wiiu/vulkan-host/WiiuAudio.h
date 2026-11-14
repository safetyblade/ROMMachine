#pragma once
#include <aaudio/AAudio.h>
#include <cstddef>
#include <cstdint>
#include <string>
#include <mutex>

// Owned by the Libretro run thread. No UI-thread close while a write is active.
class WiiuAudio {
public:
    ~WiiuAudio() { stop(); }
    bool start(int32_t sampleRate, std::string* error);
    size_t write(const int16_t* stereo, size_t frames, std::string* error);
    void stop();
    uint64_t submittedFrames() const {
        std::lock_guard<std::recursive_mutex> lock(mutex_);
        return submitted_;
    }
private:
    mutable std::recursive_mutex mutex_;
    AAudioStream* stream_ = nullptr;
    uint64_t submitted_ = 0;
};
