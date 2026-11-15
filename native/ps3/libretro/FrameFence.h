#pragma once
#include <vulkan/vulkan.h>
#include <string>

inline bool waitFrameFence(VkDevice device, VkFence fence, PFN_vkWaitForFences wait,
                           std::string* error) {
    const auto result = wait(device, 1, &fence, VK_TRUE, 5000000000ULL);
    if (result == VK_SUCCESS) return true;
    if (error) *error = result == VK_TIMEOUT ? "VULKAN_FRAME_FENCE_TIMEOUT_5S" : "VULKAN_FRAME_FENCE_DEVICE_ERROR";
    return false;
}
