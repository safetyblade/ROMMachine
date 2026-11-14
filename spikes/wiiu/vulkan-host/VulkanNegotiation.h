#pragma once
#include "libretro_vulkan.h"

inline bool supportsVulkanNegotiation(const retro_hw_render_context_negotiation_interface_vulkan* n) {
    return n && n->interface_version >= 1 &&
        (n->create_device || (n->interface_version >= 2 && n->create_device2));
}

inline bool negotiateVulkanDevice(const retro_hw_render_context_negotiation_interface_vulkan* n,
    retro_vulkan_context* context, VkInstance instance, VkPhysicalDevice gpu, VkSurfaceKHR surface,
    PFN_vkGetInstanceProcAddr proc, retro_vulkan_create_device_wrapper_t wrapper, void* opaque) {
    if (!supportsVulkanNegotiation(n)) return false;
    if (n->interface_version >= 2 && n->create_device2)
        return n->create_device2(context, instance, gpu, surface, proc, wrapper, opaque);
    const char* extensions[] = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};
    VkPhysicalDeviceFeatures required{};
    return n->create_device(context, instance, gpu, surface, proc, extensions, 1, nullptr, 0, &required);
}
