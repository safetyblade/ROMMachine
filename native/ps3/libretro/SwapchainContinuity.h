#pragma once
#include <vulkan/vulkan.h>

// A live Android native window remains associated with its existing swapchain.
// Replacement must name that swapchain, even while its WSI resources remain
// retained until shutdown. A swapchain from a different surface is invalid here.
class SwapchainContinuity {
public:
    void retire(VkSwapchainKHR old, bool sameSurface) { previous_ = sameSurface ? old : VK_NULL_HANDLE; }
    void configure(VkSwapchainCreateInfoKHR& request) const { request.oldSwapchain = previous_; }
    void created() { previous_ = VK_NULL_HANDLE; }
private:
    VkSwapchainKHR previous_ = VK_NULL_HANDLE;
};
