#pragma once
#include <algorithm>
#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>

// One serialized producer, one realtime consumer. Indices count stereo frames.
template<size_t Capacity> class StereoFifo {
    static_assert(Capacity > 0);
    static_assert(std::atomic<size_t>::is_always_lock_free);
public:
    size_t available() const {
        return written_.load(std::memory_order_acquire) - read_.load(std::memory_order_acquire);
    }
    size_t push(const int16_t* data, size_t frames) {
        const auto write = written_.load(std::memory_order_relaxed);
        const auto read = read_.load(std::memory_order_acquire);
        const auto count = std::min(frames, Capacity - (write - read));
        for (size_t i = 0; i < count; ++i) {
            samples_[((write + i) % Capacity) * 2] = data[i * 2];
            samples_[((write + i) % Capacity) * 2 + 1] = data[i * 2 + 1];
        }
        written_.store(write + count, std::memory_order_release);
        return count;
    }
    size_t pop(int16_t* data, size_t frames) {
        const auto read = read_.load(std::memory_order_relaxed);
        const auto write = written_.load(std::memory_order_acquire);
        const auto count = std::min(frames, write - read);
        for (size_t i = 0; i < count; ++i) {
            data[i * 2] = samples_[((read + i) % Capacity) * 2];
            data[i * 2 + 1] = samples_[((read + i) % Capacity) * 2 + 1];
        }
        read_.store(read + count, std::memory_order_release);
        return count;
    }
    // Consumer only; used to discard stale audio during a pause.
    void discard() { read_.store(written_.load(std::memory_order_acquire), std::memory_order_release); }
    // Only when stream and producer are stopped.
    void reset() { written_.store(0); read_.store(0); }
private:
    std::array<int16_t, Capacity * 2> samples_{};
    std::atomic<size_t> written_{0}, read_{0};
};
