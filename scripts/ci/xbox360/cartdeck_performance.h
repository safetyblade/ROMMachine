// SPDX-License-Identifier: BSD-3-Clause
#pragma once
#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdio>

namespace cartdeck::perf {
inline uint64_t now() {
  return std::chrono::duration_cast<std::chrono::nanoseconds>(
      std::chrono::steady_clock::now().time_since_epoch()).count();
}
struct Window {
  static constexpr size_t capacity = 512;
  std::array<std::atomic<uint64_t>, capacity> samples{};
  std::atomic<uint64_t> count{0}, sum{0}, maximum{0}, previous{0};
  void add(uint64_t ns) {
    const auto slot = count.fetch_add(1, std::memory_order_relaxed);
    samples[slot % capacity].store(ns, std::memory_order_relaxed);
    sum.fetch_add(ns, std::memory_order_relaxed);
    auto old = maximum.load(std::memory_order_relaxed);
    while (old < ns && !maximum.compare_exchange_weak(old, ns, std::memory_order_relaxed)) {}
  }
  void tick() {
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
enum Metric { GuestSwap, HostPresent, Jit, Shader, Pipeline, Acquire, PresentCall,
              QueueLock, FrameFence, Submit, MetricCount };
inline std::array<Window, MetricCount> windows;
inline constexpr const char* names[] = {"guest_swap_interval", "host_present_interval",
  "jit_compile", "shader_translate", "pipeline_compile", "swapchain_acquire",
  "present_call", "present_queue_lock", "frame_fence_wait", "gpu_submit_cpu"};
struct Scope {
  Metric metric;
  uint64_t start = now();
  explicit Scope(Metric m) : metric(m) {}
  ~Scope() { windows[metric].add(now() - start); }
};
inline std::atomic<uint64_t> skipped_draws{0}, audio_underrun_frames{0};
inline std::atomic<uint64_t> last_present_guest_sequence{0}, guest_sequence_repeats{0};
inline void presented(uint64_t guest_sequence) {
  windows[HostPresent].tick();
  const auto previous = last_present_guest_sequence.exchange(guest_sequence, std::memory_order_relaxed);
  if (guest_sequence && previous == guest_sequence) guest_sequence_repeats.fetch_add(1, std::memory_order_relaxed);
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
  write("skipped_draws=%llu audio_underrun_frames=%llu guest_sequence_repeats=%llu\n",
    static_cast<unsigned long long>(skipped_draws.load()),
    static_cast<unsigned long long>(audio_underrun_frames.load()),
    static_cast<unsigned long long>(guest_sequence_repeats.load()));
}
}
