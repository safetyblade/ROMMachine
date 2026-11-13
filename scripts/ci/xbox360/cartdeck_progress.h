// SPDX-License-Identifier: BSD-3-Clause
#pragma once
#include <atomic>
#include <cstdio>
namespace cartdeck {
inline std::atomic<unsigned> stages{0}, error{0};
inline std::atomic<bool> exited{false};
inline std::atomic<unsigned> gpu_vendor{0}, gpu_device{0}, gpu_driver{0}, vulkan_api{0};
inline std::atomic<int> device_result{0}, present_result{0};
inline std::atomic<const char*> missing_capability{"NONE_RECORDED"};
inline std::atomic<unsigned long long> jit_blocks{0}, guest_swaps{0}, host_presents{0};
enum Stage { HOST_INIT=1, GPU_READY=2, CONTENT_OPENED=4, GUEST_BOOT=8, AUDIO_INPUT_READY=16 };
inline void mark(Stage stage) { stages.fetch_or(stage); }
inline void fail(unsigned code) { error.store(code ? code : 1); }
inline void report(char* out, size_t size) {
  const unsigned flags = stages.load();
  const char* last = (flags & GUEST_BOOT) ? "GUEST_BOOT" :
      (flags & GPU_READY) ? "GPU_READY" : (flags & HOST_INIT) ? "HOST_INIT" : "ENGINE_LOADED";
  snprintf(out,size,"last_verified_stage=%s stages=0x%x native_error=0x%x jit_blocks=%llu guest_swaps=%llu host_presents=%llu "
    "gpu_vendor=0x%x gpu_device=0x%x gpu_driver=0x%x vulkan_api=0x%x missing_capability=%s "
    "device_VkResult=%d present_VkResult=%d shutdown_complete=%d "
    "guest_image=UNVERIFIED submitted_frame=UNVERIFIED accepted_present=UNVERIFIED gameplay=UNVERIFIED driver=SYSTEM",
    last,flags,error.load(),jit_blocks.load(),guest_swaps.load(),host_presents.load(),
    gpu_vendor.load(),gpu_device.load(),gpu_driver.load(),vulkan_api.load(),missing_capability.load(),
    device_result.load(),present_result.load(),int(exited.load()));
}
}
