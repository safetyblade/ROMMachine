#include "cartdeck_performance.h"
#include <cassert>
#include <cstring>
#include <thread>
#include <vector>
int main() {
  using namespace cartdeck::perf;
  Window test;
  assert(!capture_enabled.load());
  test.add(999);
  assert(test.count == 0); // Ordinary gameplay never records diagnostics.
  set_capture_enabled(true);
  for (uint64_t i = 1; i <= 100; ++i) test.add(i);
  assert((test.percentiles() == std::array<uint64_t,3>{50,95,99}));
  assert(test.count == 100 && test.sum == 5050 && test.maximum == 100);
  for (unsigned i = 0; i < 512; ++i) test.add(7);
  assert((test.percentiles() == std::array<uint64_t,3>{7,7,7}));
  Window concurrent;
  std::vector<std::thread> threads;
  for (unsigned t = 0; t < 8; ++t) threads.emplace_back([&] {
    for (unsigned n = 0; n < 10000; ++n) concurrent.add(n+1);
  });
  for (auto& thread : threads) thread.join();
  assert(concurrent.count == 80000 && concurrent.maximum == 10000);
  char report[16384] = "baseline";
  windows[Jit].add(123456789);
  presented(1); presented(1); presented(2);
  append(report, sizeof(report));
  assert(std::strstr(report,"jit_compile count=1 p50_ms=123.456789"));
  assert(std::strstr(report,"unique_completed_frames=UNAVAILABLE"));
  assert(std::strstr(report,"guest_sequence_repeats=1"));
  // Both settings and real refresh work are reported without changing a cvar.
  note_xgfx_settings(false, true, true, true, 1300);
  windows[XgfxPageRefresh].add(5000000);
  char xgfx_report[16384] = "baseline";
  append(xgfx_report, sizeof(xgfx_report));
  assert(std::strstr(xgfx_report, "xgfx03_schema=1 capture=ON frames=1"));
  assert(std::strstr(xgfx_report, "clear_memory_page_state=0"));
  assert(std::strstr(xgfx_report, "vulkan_cache_texture_descriptors=1"));
  assert(std::strstr(xgfx_report, "vulkan_mid_frame_submission_draws=1300"));
  assert(std::strstr(xgfx_report, "xgfx_page_state_refresh count=1 p50_ms=5.000000"));
  // Turning capture off retains the prior snapshot, but stops new samples.
  set_capture_enabled(false);
  const auto stopped_count = windows[XgfxPageRefresh].count.load();
  windows[XgfxPageRefresh].add(9000000);
  note_xgfx_settings(false, false, false, false, 999);
  assert(windows[XgfxPageRefresh].count.load() == stopped_count);
  assert(xgfx_settings_frames.load() == 1);
  set_capture_enabled(true);
  note_xgfx_settings(true, false, false, false, 200);
  char alternate[16384] = "baseline";
  append(alternate, sizeof(alternate));
  assert(std::strstr(alternate, "clear_memory_page_state=1"));
  assert(std::strstr(alternate, "vulkan_cache_texture_descriptors=0"));
  assert(std::strstr(alternate, "vulkan_mid_frame_submission_draws=200"));
  char tiny[5] = "abc";
  append(tiny,sizeof(tiny)); assert(tiny[4] == 0);
  const auto start=now();
  for (unsigned n=0;n<1000000;++n) windows[Jit].add(n+1);
  std::printf("synthetic_record_ns_per_event=%.2f (host-only, not gameplay)\n",(now()-start)/1000000.0);
}
