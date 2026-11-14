#pragma once
// WIIU-02: isolated CARTDECK Vulkan frame presenter, *not* a player release.
// Headers are compiled against the pinned Cemu Libretro source checkout.
#define VK_USE_PLATFORM_ANDROID_KHR 1
#include <android/native_window.h>
#include <vulkan/vulkan.h>
#include "libretro.h"
#include "libretro_vulkan.h"
#include <cstdint>
#include <mutex>
#include <string>
#include <vector>

class CartdeckVulkanPresenter final {
public:
    CartdeckVulkanPresenter() = default;
    ~CartdeckVulkanPresenter();

    // Android ANativeWindow lifetime is retained until stop().
    bool initialize(ANativeWindow* window,
                    const retro_hw_render_context_negotiation_interface_vulkan* negotiation,
                    std::string* error);
    bool acquire(std::string* error);
    bool present(unsigned width, unsigned height, std::string* error);
    bool presentBlank(std::string* error);
    bool hasAcquiredFrame() const { return acquired_; }
    void stop();

    retro_hw_render_interface_vulkan* interface() { return &frontend_; }
    bool ready() const { return device_ != VK_NULL_HANDLE && swapchain_ != VK_NULL_HANDLE; }
    unsigned presentedFrames() const { return presented_frames_; }

private:
    static VkInstance createInstanceWrapper(void* opaque, const VkInstanceCreateInfo* request);
    static VkDevice createDeviceWrapper(VkPhysicalDevice gpu, void* opaque,
                                        const VkDeviceCreateInfo* request);
    static void setImage(void* handle, const retro_vulkan_image* image,
                         uint32_t num_semaphores, const VkSemaphore* semaphores,
                         uint32_t src_queue_family);
    static uint32_t getSyncIndex(void* handle);
    static uint32_t getSyncMask(void* handle);
    static void setCommandBuffers(void* handle, uint32_t count, const VkCommandBuffer* cmd);
    static void waitSync(void* handle);
    static void lockQueue(void* handle);
    static void unlockQueue(void* handle);
    static void setSignalSemaphore(void* handle, VkSemaphore semaphore);

    bool createSwapchain(std::string* error);
    static bool fail(std::string* error, const char* message);

    ANativeWindow* window_ = nullptr;
    const retro_hw_render_context_negotiation_interface_vulkan* negotiation_ = nullptr;
    retro_hw_render_interface_vulkan frontend_{};
    retro_vulkan_context context_{};
    VkInstance instance_ = VK_NULL_HANDLE;
    VkSurfaceKHR surface_ = VK_NULL_HANDLE;
    VkPhysicalDevice gpu_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue queue_ = VK_NULL_HANDLE;
    uint32_t family_ = UINT32_MAX;
    VkSwapchainKHR swapchain_ = VK_NULL_HANDLE;
    VkFormat surface_format_ = VK_FORMAT_UNDEFINED;
    VkExtent2D extent_{};
    std::vector<VkImage> images_;
    VkCommandPool pool_ = VK_NULL_HANDLE;
    std::vector<VkCommandBuffer> commands_;
    VkSemaphore acquired_sem_ = VK_NULL_HANDLE;
    VkSemaphore rendered_sem_ = VK_NULL_HANDLE;
    uint32_t image_index_ = 0;
    bool acquired_ = false;
    bool unsupported_command_buffers_ = false;
    bool saw_image_ = false;
    retro_vulkan_image source_image_{};
    std::vector<VkSemaphore> source_semaphores_;
    uint32_t source_family_ = VK_QUEUE_FAMILY_IGNORED;
    VkSemaphore reuse_signal_ = VK_NULL_HANDLE;
    std::mutex queue_mutex_;
    unsigned presented_frames_ = 0;
};
