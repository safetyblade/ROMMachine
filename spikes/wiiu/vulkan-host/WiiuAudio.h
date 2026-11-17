#pragma once
#include <aaudio/AAudio.h>
#include <cstddef>
#include <cstdint>
#include <string>
#include <mutex>
#include <atomic>
#include "StereoFifo.h"

// Producer never waits for the Android audio device. Callback never takes a lock.
class WiiuAudio {
public:
    ~WiiuAudio() { stop(); }
    bool start(int32_t sampleRate, std::string* error);
    size_t write(const int16_t* stereo, size_t frames, std::string* error);
    void stop();
    // VM/control thread only; never invoked by the AAudio callbacks.
    void service();
    uint64_t submittedFrames() const {
        std::lock_guard<std::recursive_mutex> lock(mutex_);
        return submitted_;
    }
private:
    static aaudio_data_callback_result_t render(AAudioStream*, void*, void*, int32_t);
    static void disconnected(AAudioStream*, void*, aaudio_result_t);
    bool openStream(std::string* error);
    void closeStream();
    int32_t sample_rate_ = 0;
    uint64_t retry_at_us_ = 0;
    bool enabled_ = false;
    StereoFifo<8192> fifo_;
    bool played_once_ = false; // audio callback thread only
    bool primed_ = false; // audio callback thread only
    int32_t prebuffer_ = 0; // immutable while stream runs
    std::atomic<int> error_{0};
    mutable std::recursive_mutex mutex_;
    AAudioStream* stream_ = nullptr;
    uint64_t submitted_ = 0;
};
