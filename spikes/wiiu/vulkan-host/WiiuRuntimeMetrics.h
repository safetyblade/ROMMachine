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
inline std::atomic<uint64_t> max_run_us{0}, max_fence_us{0};
inline std::atomic<uint64_t> video_callback_us{0}, audio_callback_us{0};
inline std::atomic<uint64_t> max_run_video_us{0}, max_run_audio_us{0};
struct CallbackTimer {
    std::atomic<uint64_t>& metric;
    uint64_t begin = nowUs();
    ~CallbackTimer() { metric.fetch_add(nowUs() - begin); }
};
inline void maximum(std::atomic<uint64_t>& metric, uint64_t value) {
    auto old = metric.load();
    while (old < value && !metric.compare_exchange_weak(old, value)) {}
}
inline std::atomic<unsigned> gpu_operation{0};
inline std::atomic<uint64_t> gpu_started_us{0}, fence_wait_us{0};
inline std::atomic<uint64_t> shutdown_idle_us{0};
inline std::atomic<int> shutdown_idle_result{0};
inline std::atomic<uint64_t> audio_accepted{0}, audio_played{0}, audio_underflow{0}, audio_overflow{0};
inline std::atomic<uint64_t> audio_callbacks{0};
inline std::atomic<uint64_t> audio_recovery_attempts{0}, audio_recoveries{0};
inline std::atomic<int> audio_recovery_result{0};
inline std::atomic<int> audio_rate{0}, audio_buffered{0}, audio_xruns{0}, audio_error{0};
inline std::atomic<bool> audio_paused{false};
inline std::atomic<double> target_fps{60.0};
inline void gpu(unsigned operation) { gpu_started_us.store(nowUs()); gpu_operation.store(operation); }
inline std::string report() {
    const char* names[] = {"idle", "acquire", "frame_fence", "submit", "present", "surface_rebuild", "shutdown_device_idle"};
    const auto op = gpu_operation.load();
    const auto start = gpu_started_us.load();
    const auto guestStart = run_started_us.load();
    return "\nrun_calls=" + std::to_string(run_calls.load()) +
        "\nrun_returns=" + std::to_string(run_returns.load()) +
        "\nrun_age_ms=" + std::to_string(guestStart ? (nowUs() - guestStart) / 1000 : 0) +
        "\nlast_run_us=" + std::to_string(last_run_us.load()) +
        "\nmax_run_us=" + std::to_string(max_run_us.load()) +
        "\nmax_run_video_callback_us=" + std::to_string(max_run_video_us.load()) +
        "\nmax_run_audio_callback_us=" + std::to_string(max_run_audio_us.load()) +
        "\nrun_timing_scope=inclusive_core_call_with_callback_wall_times_not_gpu_execution" +
        "\nmax_frame_fence_wait_us=" + std::to_string(max_fence_us.load()) +
        "\ngpu_operation=" + (op < 7 ? std::string(names[op]) : "unknown") +
        "\ngpu_operation_age_ms=" + std::to_string(start ? (nowUs() - start) / 1000 : 0) +
        "\nshutdown_idle_completed_us=" + std::to_string(shutdown_idle_us.load()) +
        "\nshutdown_idle_result=" + std::to_string(shutdown_idle_result.load()) +
        "\nlast_frame_fence_wait_us=" + std::to_string(fence_wait_us.load()) +
        "\ntarget_fps=" + std::to_string(target_fps.load()) +
        "\naudio_sample_rate=" + std::to_string(audio_rate.load()) +
        "\naudio_accepted_frames=" + std::to_string(audio_accepted.load()) +
        "\naudio_callback_frames=" + std::to_string(audio_played.load()) +
        "\naudio_callbacks=" + std::to_string(audio_callbacks.load()) +
        "\naudio_buffered_frames=" + std::to_string(audio_buffered.load()) +
        "\naudio_underrun_frames=" + std::to_string(audio_underflow.load()) +
        "\naudio_unaccepted_frames=" + std::to_string(audio_overflow.load()) +
        "\naudio_device_xruns=" + std::to_string(audio_xruns.load()) +
        "\naudio_device_error=" + std::to_string(audio_error.load()) +
        "\naudio_recovery_attempts=" + std::to_string(audio_recovery_attempts.load()) +
        "\naudio_recoveries=" + std::to_string(audio_recoveries.load()) +
        "\naudio_recovery_result=" + std::to_string(audio_recovery_result.load());
}
}
