// SPDX-License-Identifier: BSD-3-Clause
#pragma once
#include <atomic>
#include <cstdio>
namespace cartdeck {
inline std::atomic<unsigned> stages{0}, error{0};
inline std::atomic<unsigned long long> jit_blocks{0};
enum Stage { HOST_INIT=1, GPU_READY=2, CONTENT_OPENED=4, GUEST_BOOT=8, AUDIO_INPUT_READY=16 };
inline void mark(Stage stage) { stages.fetch_or(stage); }
inline void fail(unsigned code) { error.store(code ? code : 1); }
inline void report(char* out, size_t size) {
  snprintf(out,size,"stages=0x%x native_error=0x%x jit_blocks=%llu "
    "guest_image=UNVERIFIED submitted_frame=UNVERIFIED accepted_present=UNVERIFIED gameplay=UNVERIFIED driver=SYSTEM",
    stages.load(),error.load(),jit_blocks.load());
}
}
