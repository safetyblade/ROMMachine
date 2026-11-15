#pragma once
#include <vulkan/vulkan.h>

// The transfer blit writes upright logical pixels, with no vertex rotation.
// IDENTITY asks Android's compositor to handle display rotation. Advertising
// currentTransform instead claims we already pre-rotated pixels (we did not).
inline bool uprightSurfaceTransform(const VkSurfaceCapabilitiesKHR& caps,
                                    VkSurfaceTransformFlagBitsKHR* selected) {
    if (!(caps.supportedTransforms & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)) return false;
    *selected = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    return true;
}
