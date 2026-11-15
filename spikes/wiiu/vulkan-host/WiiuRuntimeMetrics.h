#pragma once
#include <atomic>
#include <chrono>
#include <cstdint>
#include <string>

namespace WiiuMetrics {
inline uint64_t nowUs() {
    return std::chrono::duration_cast<std::chrono::microseconds>(
        std::chrono::steady_clock::now().time_since_epoch()).count();
}
inline std::atomic<uint64_t> run_calls{0}, run_returns{0}, run_started_us{0}, last_run_us{0};
inline std::atomic<unsigned> gpu_operation{0};
inline std::atomic<uint64_t> gpu_started_us{0}, fence_wait_us{0};
inline std::atomic<uint64_t> audio_accepted{0}, audio_played{0}, audio_underflow{0}, audio_overflow{0};
inline std::atomic<int> audio_rate{0}, audio_buffered{0}, audio_xruns{0}, audio_error{0};
inline std::atomic<bool> audio_paused{false};
inline std::atomic<double> target_fps{60.0};
inline void gpu(unsigned operation) { gpu_started_us.store(nowUs()); gpu_operation.store(operation); }
inline std::string report() {
    const char* names[] = {"idle", "acquire", "frame_fence", "submit", "present", "resize_device_idle", "shutdown_device_idle"};
    const auto op = gpu_operation.load();
    const auto start = gpu_started_us.load();
    const auto guestStart = run_started_us.load();
    return "\nrun_calls=" + std::to_string(run_calls.load()) +
        "\nrun_returns=" + std::to_string(run_returns.load()) +
        "\nrun_age_ms=" + std::to_string(guestStart ? (nowUs() - guestStart) / 1000 : 0) +
        "\nlast_run_us=" + std::to_string(last_run_us.load()) +
        "\ngpu_operation=" + (op < 7 ? std::string(names[op]) : "unknown") +
        "\ngpu_operation_age_ms=" + std::to_string(start ? (nowUs() - start) / 1000 : 0) +
        "\nlast_frame_fence_wait_us=" + std::to_string(fence_wait_us.load()) +
        "\ntarget_fps=" + std::to_string(target_fps.load()) +
        "\naudio_sample_rate=" + std::to_string(audio_rate.load()) +
        "\naudio_accepted_frames=" + std::to_string(audio_accepted.load()) +
        "\naudio_played_frames=" + std::to_string(audio_played.load()) +
        "\naudio_buffered_frames=" + std::to_string(audio_buffered.load()) +
        "\naudio_underrun_frames=" + std::to_string(audio_underflow.load()) +
        "\naudio_unaccepted_frames=" + std::to_string(audio_overflow.load()) +
        "\naudio_device_xruns=" + std::to_string(audio_xruns.load()) +
        "\naudio_device_error=" + std::to_string(audio_error.load());
}
}
