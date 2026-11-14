#include "VulkanNegotiation.h"
#include <cassert>
#include <cstring>

static bool v1_called = false, v2_called = false;
static bool v1(retro_vulkan_context*, VkInstance, VkPhysicalDevice, VkSurfaceKHR,
    PFN_vkGetInstanceProcAddr, const char** extensions, unsigned count,
    const char**, unsigned layers, const VkPhysicalDeviceFeatures* features) {
    assert(count == 1 && std::strcmp(extensions[0], VK_KHR_SWAPCHAIN_EXTENSION_NAME) == 0);
    assert(layers == 0 && features);
    v1_called = true;
    return true;
}
static bool v2(retro_vulkan_context*, VkInstance, VkPhysicalDevice, VkSurfaceKHR,
    PFN_vkGetInstanceProcAddr, retro_vulkan_create_device_wrapper_t, void*) {
    v2_called = true;
    return true;
}
int main() {
    // Exact shape of pinned Cemu: version 2 advertised, only v1 callbacks populated.
    retro_hw_render_context_negotiation_interface_vulkan n{
        RETRO_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_VULKAN,
        RETRO_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_VULKAN_VERSION, nullptr, v1, nullptr};
    retro_vulkan_context context{};
    assert(supportsVulkanNegotiation(&n));
    assert(negotiateVulkanDevice(&n, &context, {}, {}, {}, nullptr, nullptr, nullptr));
    assert(v1_called && !v2_called);
    n.create_device2 = v2;
    v1_called = false;
    assert(negotiateVulkanDevice(&n, &context, {}, {}, {}, nullptr, nullptr, nullptr));
    assert(v2_called && !v1_called);
    n.interface_version = 1;
    v1_called = false;
    assert(negotiateVulkanDevice(&n, &context, {}, {}, {}, nullptr, nullptr, nullptr));
    assert(v1_called);
    n.create_device = nullptr;
    assert(!supportsVulkanNegotiation(&n));
    assert(!supportsVulkanNegotiation(nullptr));
}
