// SPDX-License-Identifier: BSD-3-Clause
#pragma once
#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdio>

namespace cartdeck::perf {
// Never capture performance metrics during ordinary gameplay. Explicitly enabled
// from the paused Advanced > Diagnostics screen, with no on-screen HUD.
inline std::atomic<bool> capture_enabled{false};
inline std::atomic<uint64_t> capture_sessions{0};
inline void set_capture_enabled(bool enabled) {
  const bool was_enabled = capture_enabled.exchange(enabled, std::memory_order_acq_rel);
  if (enabled && !was_enabled) {
    capture_sessions.fetch_add(1, std::memory_order_relaxed);
  }
}
inline uint64_t now() {
  return std::chrono::duration_cast<std::chrono::nanoseconds>(
      std::chrono::steady_clock::now().time_since_epoch()).count();
}
struct Window {
  static constexpr size_t capacity = 512;
  std::array<std::atomic<uint64_t>, capacity> samples{};
  std::atomic<uint64_t> count{0}, sum{0}, maximum{0}, previous{0};
  void add(uint64_t ns) {
    if (!capture_enabled.load(std::memory_order_relaxed)) return;
    const auto slot = count.fetch_add(1, std::memory_order_relaxed);
    samples[slot % capacity].store(ns, std::memory_order_relaxed);
    sum.fetch_add(ns, std::memory_order_relaxed);
    auto old = maximum.load(std::memory_order_relaxed);
    while (old < ns && !maximum.compare_exchange_weak(old, ns, std::memory_order_relaxed)) {}
  }
  void tick() {
    if (!capture_enabled.load(std::memory_order_relaxed)) {
      previous.store(0, std::memory_order_relaxed);
      return;
    }
    const auto time = now();
    const auto before = previous.exchange(time, std::memory_order_relaxed);
    if (before && time >= before) add(time - before);
  }
  std::array<uint64_t, 3> percentiles() const {
    std::array<uint64_t, capacity> copy{};
    size_t n = 0;
    for (const auto& value : samples) {
      const auto ns = value.load(std::memory_order_relaxed);
      if (ns) copy[n++] = ns;
    }
    if (!n) return {};
    std::sort(copy.begin(), copy.begin() + n);
    return {copy[(n * 50 + 99) / 100 - 1], copy[(n * 95 + 99) / 100 - 1],
            copy[(n * 99 + 99) / 100 - 1]};
  }
};
// S6: avoid even reading the clock on the Vulkan hot path unless a user has
// explicitly opened diagnostics. This is a performance optimization, not a
// change to the emulator guest, frame scheduling, or rendering policy.
inline uint64_t capture_start() {
  return capture_enabled.load(std::memory_order_relaxed) ? now() : 0;
}
inline void capture_finish(Window& window, uint64_t start) {
  if (start) window.add(now() - start);
}
enum Metric { GuestSwap, HostPresent, Jit, Shader, Pipeline, Acquire, PresentCall,
              QueueLock, FrameFence, Submit, XgfxPageRefresh, XgfxPipelineQueueWait, MetricCount };
inline std::array<Window, MetricCount> windows;
inline constexpr const char* names[] = {"guest_swap_interval", "host_present_interval",
  "jit_compile", "shader_translate", "pipeline_compile", "swapchain_acquire",
  "present_call", "present_queue_lock", "frame_fence_wait", "gpu_submit_cpu",
  "xgfx_page_state_refresh", "xgfx_pipeline_queue_wait"};
struct Scope {
  Metric metric;
  uint64_t start;
  explicit Scope(Metric m) : metric(m), start(capture_start()) {}
  ~Scope() { if (start) windows[metric].add(now() - start); }
};
// 03A: read-only frame-boundary snapshot of the effective Xenia settings.
// One bounded write per frame, never a render policy override or per-draw log.
inline std::atomic<uint32_t> xgfx_settings{0};
inline std::atomic<int> xgfx_mid_frame_draws{0};
inline std::atomic<uint64_t> xgfx_settings_frames{0};
inline void note_xgfx_settings(bool clear_pages, bool cache_descriptors,
                               bool skip_fetch_writes, bool fast_registers,
                               int mid_frame_draws) {
  if (!capture_enabled.load(std::memory_order_relaxed)) return;
  const uint32_t bits = (clear_pages ? 1u : 0u) |
      (cache_descriptors ? 2u : 0u) |
      (skip_fetch_writes ? 4u : 0u) | (fast_registers ? 8u : 0u);
  xgfx_settings.store(bits, std::memory_order_relaxed);
  xgfx_mid_frame_draws.store(mid_frame_draws, std::memory_order_relaxed);
  xgfx_settings_frames.fetch_add(1, std::memory_order_relaxed);
}
// XGFX-04: replay-time counts, unlike skipped_draws (immediate draw checks).
// Never inspect or log guest buffers; only aggregate atomics when user opts in.
inline std::atomic<uint64_t> xgfx04_deferred_pipeline_unready{0};
inline std::atomic<uint64_t> xgfx04_replay_dropped_draw{0};
inline std::atomic<uint64_t> xgfx04_replay_dropped_indexed_draw{0};
inline std::atomic<uint64_t> xgfx04_replay_submitted_draw{0};
inline std::atomic<uint64_t> xgfx04_replay_submitted_indexed_draw{0};
inline std::atomic<uint64_t> xgfx04_gpu_range_writes{0};
inline std::atomic<uint64_t> xgfx04_valid_changed_blocks{0};
inline std::atomic<uint64_t> xgfx04_gpu_to_cpu_blocks{0};
inline std::atomic<uint64_t> xgfx04_cpu_to_gpu_blocks{0};
inline void count(std::atomic<uint64_t>& metric) {
  if (capture_enabled.load(std::memory_order_relaxed)) {
    metric.fetch_add(1, std::memory_order_relaxed);
  }
}
inline std::atomic<uint64_t> skipped_draws{0}, audio_underrun_frames{0};
inline std::atomic<uint64_t> last_present_guest_sequence{0}, guest_sequence_repeats{0};
inline std::atomic<uint64_t> guest_sequence_advances{0}, guest_sequence_regressions{0};
inline void presented(uint64_t guest_sequence) {
  if (!capture_enabled.load(std::memory_order_relaxed)) return;
  windows[HostPresent].tick();
  const auto previous = last_present_guest_sequence.exchange(guest_sequence, std::memory_order_relaxed);
  if (guest_sequence && previous == guest_sequence) {
    guest_sequence_repeats.fetch_add(1, std::memory_order_relaxed);
  } else if (guest_sequence > previous) {
    guest_sequence_advances.fetch_add(1, std::memory_order_relaxed);
  } else if (guest_sequence && previous > guest_sequence) {
    guest_sequence_regressions.fetch_add(1, std::memory_order_relaxed);
  }
}
inline void append(char* out, size_t size) {
  size_t used = 0;
  while (used < size && out[used]) ++used;
  if (used == size) return;
  auto write = [&](const char* format, auto... args) {
    if (used + 1 >= size) return;
    const int n = std::snprintf(out + used, size - used, format, args...);
    if (n > 0) used += std::min(size - used - 1, static_cast<size_t>(n));
  };
  write("%s", "\nperformance_schema=1 timing=CPU_MONOTONIC window_samples_max=512 "
        "unique_completed_frames=UNAVAILABLE guest_simulation_rate=UNAVAILABLE "
        "gpu_execution_time=UNAVAILABLE imagery=UNVERIFIED\n");
  for (unsigned i = 0; i < MetricCount; ++i) {
    const auto p = windows[i].percentiles();
    write("%s count=%llu p50_ms=%.6f p95_ms=%.6f p99_ms=%.6f total_ms=%.3f max_ms=%.3f\n",
      names[i], static_cast<unsigned long long>(windows[i].count.load()),
      p[0] / 1e6, p[1] / 1e6, p[2] / 1e6, windows[i].sum.load() / 1e6,
      windows[i].maximum.load() / 1e6);
  }
  const uint32_t xgfx = xgfx_settings.load(std::memory_order_relaxed);
  write("xgfx03_schema=1 capture=%s frames=%llu measured=FRAME_BOUNDARY "
        "clear_memory_page_state=%d vulkan_cache_texture_descriptors=%d "
        "vulkan_skip_redundant_fetch_constant_writes=%d "
        "vulkan_fast_register_ranges=%d vulkan_mid_frame_submission_draws=%d\n",
        capture_enabled.load(std::memory_order_relaxed) ? "ON" : "OFF",
        static_cast<unsigned long long>(xgfx_settings_frames.load(std::memory_order_relaxed)),
        int(bool(xgfx & 1u)), int(bool(xgfx & 2u)), int(bool(xgfx & 4u)),
        int(bool(xgfx & 8u)), xgfx_mid_frame_draws.load(std::memory_order_relaxed));
  write("skipped_draws=%llu audio_underrun_frames=%llu guest_sequence_repeats=%llu\n",
    static_cast<unsigned long long>(skipped_draws.load()),
    static_cast<unsigned long long>(audio_underrun_frames.load()),
    static_cast<unsigned long long>(guest_sequence_repeats.load()));
  write("perf_xb_schema=1 capture_sessions=%llu scope=PROCESS_CUMULATIVE "
        "guest_sequence_advances=%llu guest_sequence_regressions=%llu "
        "guest_image_integrity=UNVERIFIED unique_completed_frames=UNAVAILABLE\n",
    static_cast<unsigned long long>(capture_sessions.load()),
    static_cast<unsigned long long>(guest_sequence_advances.load()),
    static_cast<unsigned long long>(guest_sequence_regressions.load()));
  write("xgfx04_schema=1 capture=%s "
        "deferred_pipeline_unready=%llu replay_dropped_draw=%llu "
        "replay_dropped_indexed_draw=%llu replay_submitted_draw=%llu "
        "replay_submitted_indexed_draw=%llu gpu_range_writes=%llu "
        "valid_changed_blocks=%llu gpu_to_cpu_blocks=%llu "
        "cpu_to_gpu_blocks=%llu (block=64_pages, counters=sessions)\n",
        capture_enabled.load(std::memory_order_relaxed) ? "ON" : "OFF",
        static_cast<unsigned long long>(xgfx04_deferred_pipeline_unready.load()),
        static_cast<unsigned long long>(xgfx04_replay_dropped_draw.load()),
        static_cast<unsigned long long>(xgfx04_replay_dropped_indexed_draw.load()),
        static_cast<unsigned long long>(xgfx04_replay_submitted_draw.load()),
        static_cast<unsigned long long>(xgfx04_replay_submitted_indexed_draw.load()),
        static_cast<unsigned long long>(xgfx04_gpu_range_writes.load()),
        static_cast<unsigned long long>(xgfx04_valid_changed_blocks.load()),
        static_cast<unsigned long long>(xgfx04_gpu_to_cpu_blocks.load()),
        static_cast<unsigned long long>(xgfx04_cpu_to_gpu_blocks.load()));
}
}
