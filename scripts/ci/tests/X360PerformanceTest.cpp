#include "cartdeck_performance.h"
#include <cassert>
#include <cstring>
#include <thread>
#include <vector>
int main() {
  using namespace cartdeck::perf;
  Window test;
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
  char tiny[5] = "abc";
  append(tiny,sizeof(tiny)); assert(tiny[4] == 0);
  const auto start=now();
  for (unsigned n=0;n<1000000;++n) windows[Jit].add(n+1);
  std::printf("synthetic_record_ns_per_event=%.2f (host-only, not gameplay)\n",(now()-start)/1000000.0);
}
