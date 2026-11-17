// WIIU-02: minimal real Vulkan image handoff for the pinned Cemu Libretro ABI.
// No software test can replace physical GPU / presentation / save QA.
#include "CartdeckVulkanPresenter.h"
#include "VulkanNegotiation.h"
#include "WiiuRuntimeMetrics.h"
#include "FrameFence.h"
#include "SurfacePresentation.h"
#include <utility>
#include <algorithm>
#include <cstring>

namespace {
bool includes(const std::vector<const char*>& values, const char* needle) {
    return std::any_of(values.begin(), values.end(),
                       [needle](const char* value) {
                           return value && std::strcmp(value, needle) == 0;
                       });
}
VkExtent2D clampExtent(const VkSurfaceCapabilitiesKHR& caps, ANativeWindow* window) {
    if (caps.currentExtent.width != UINT32_MAX) return caps.currentExtent;
    return {
        std::clamp(static_cast<uint32_t>(std::max(1, ANativeWindow_getWidth(window))),
                   caps.minImageExtent.width, caps.maxImageExtent.width),
        std::clamp(static_cast<uint32_t>(std::max(1, ANativeWindow_getHeight(window))),
                   caps.minImageExtent.height, caps.maxImageExtent.height),
    };
}
}  // namespace

CartdeckVulkanPresenter::~CartdeckVulkanPresenter() { stop(); }

std::string CartdeckVulkanPresenter::deviceDescription() const {
    if (!gpu_) return "vulkan_device=UNAVAILABLE";
    VkPhysicalDeviceProperties properties{};
    vkGetPhysicalDeviceProperties(gpu_, &properties);
    return std::string("renderer=Vulkan; driver_selection=STOCK\nvulkan_device=") + properties.deviceName +
        "\nvendor_id=" + std::to_string(properties.vendorID) +
        "\ndevice_id=" + std::to_string(properties.deviceID) +
        "\ndriver_version=" + std::to_string(properties.driverVersion) +
        "\nvulkan_api=" + std::to_string(properties.apiVersion) +
        "\nsurface_current_transform=" + std::to_string(surface_current_transform_) +
        "\nswapchain_pre_transform=" + std::to_string(surface_pre_transform_) +
        "\nswapchain_width=" + std::to_string(extent_.width) +
        "\nswapchain_height=" + std::to_string(extent_.height);
}

bool CartdeckVulkanPresenter::fail(std::string* err, const char* message) {
    if (err) *err = message;
    return false;
}

VkInstance CartdeckVulkanPresenter::createInstanceWrapper(
    void* opaque, const VkInstanceCreateInfo* request) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(opaque);
    if (!self || !request) return VK_NULL_HANDLE;
    std::vector<const char*> extensions;
    if (request->enabledExtensionCount)
        extensions.assign(request->ppEnabledExtensionNames,
            request->ppEnabledExtensionNames + request->enabledExtensionCount);
    for (const char* name : {VK_KHR_SURFACE_EXTENSION_NAME,
                             VK_KHR_ANDROID_SURFACE_EXTENSION_NAME}) {
        if (!includes(extensions, name)) extensions.push_back(name);
    }
    VkInstanceCreateInfo created = *request;
    created.ppEnabledExtensionNames = extensions.data();
    created.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    VkInstance instance = VK_NULL_HANDLE;
    if (vkCreateInstance(&created, nullptr, &instance) != VK_SUCCESS)
        return VK_NULL_HANDLE;
    self->instance_ = instance;
    return instance;
}

VkDevice CartdeckVulkanPresenter::createDeviceWrapper(
    VkPhysicalDevice gpu, void* opaque, const VkDeviceCreateInfo* request) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(opaque);
    if (!self || !request || gpu != self->gpu_) return VK_NULL_HANDLE;
    std::vector<const char*> extensions;
    if (request->enabledExtensionCount)
        extensions.assign(request->ppEnabledExtensionNames,
            request->ppEnabledExtensionNames + request->enabledExtensionCount);
    if (!includes(extensions, VK_KHR_SWAPCHAIN_EXTENSION_NAME))
        extensions.push_back(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
    VkDeviceCreateInfo created = *request;
    // Preserve all of Cemu's required Vulkan pNext features and queues.
    created.ppEnabledExtensionNames = extensions.data();
    created.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    VkDevice device = VK_NULL_HANDLE;
    if (vkCreateDevice(gpu, &created, nullptr, &device) != VK_SUCCESS)
        return VK_NULL_HANDLE;
    self->device_ = device;
    return device;
}

void CartdeckVulkanPresenter::setImage(
    void* handle, const retro_vulkan_image* image, uint32_t count,
    const VkSemaphore* semaphores, uint32_t family) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(handle);
    if (!self) return;
    self->saw_image_ = image != nullptr;
    self->source_image_ = image ? *image : retro_vulkan_image{};
    self->source_family_ = family;
    self->source_semaphores_.clear();
    if (semaphores && count <= 32)
        self->source_semaphores_.assign(semaphores, semaphores + count);
    if (count > 32) self->unsupported_command_buffers_ = true;
}

uint32_t CartdeckVulkanPresenter::getSyncIndex(void* h) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    return self ? self->image_index_ : 0;
}
uint32_t CartdeckVulkanPresenter::getSyncMask(void* h) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    if (!self || self->images_.empty() || self->images_.size() >= 32) return 0;
    return (1u << self->images_.size()) - 1;
}
void CartdeckVulkanPresenter::setCommandBuffers(
    void* h, uint32_t count, const VkCommandBuffer*) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    // Explicitly reject unsupported externally submitted command buffers.
    // Cemu's currently pinned Vulkan path uses set_image, not this callback.
    if (self && count) self->unsupported_command_buffers_ = true;
}
void CartdeckVulkanPresenter::waitSync(void* h) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    if (self && !self->waitFrame(nullptr)) self->sync_failed_ = true;
}
void CartdeckVulkanPresenter::lockQueue(void* h) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    if (self) self->queue_mutex_.lock();
}
void CartdeckVulkanPresenter::unlockQueue(void* h) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    if (self) self->queue_mutex_.unlock();
}
void CartdeckVulkanPresenter::setSignalSemaphore(void* h, VkSemaphore semaphore) {
    auto* self = static_cast<CartdeckVulkanPresenter*>(h);
    if (self) self->reuse_signal_ = semaphore;
}

bool CartdeckVulkanPresenter::initialize(
    ANativeWindow* window,
    const retro_hw_render_context_negotiation_interface_vulkan* negotiation,
    std::string* error) {
    if (ready() || window_) return fail(error, "VULKAN_ALREADY_INITIALISED");
    if (!window || !supportsVulkanNegotiation(negotiation))
        return fail(error, "VULKAN_CONTEXT_NEGOTIATION_CALLBACK_MISSING");
    window_ = window;
    ANativeWindow_acquire(window_);
    negotiation_ = negotiation;
    VkApplicationInfo fallback{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    fallback.pApplicationName = "CARTDECK Wii U";
    fallback.apiVersion = VK_API_VERSION_1_1;
    const VkApplicationInfo* requested = negotiation_->get_application_info ?
        negotiation_->get_application_info() : &fallback;
    if (!requested) requested = &fallback;
    if (negotiation_->interface_version >= 2 && negotiation_->create_instance) {
        instance_ = negotiation_->create_instance(vkGetInstanceProcAddr, requested, &createInstanceWrapper, this);
    } else {
        VkInstanceCreateInfo info{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
        info.pApplicationInfo = requested;
        instance_ = createInstanceWrapper(this, &info);
    }
    if (!instance_) return fail(error, "VULKAN_INSTANCE_CREATE_FAILED");

    VkAndroidSurfaceCreateInfoKHR sci{VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR};
    sci.window = window_;
    if (vkCreateAndroidSurfaceKHR(instance_, &sci, nullptr, &surface_) != VK_SUCCESS)
        return fail(error, "VULKAN_ANDROID_SURFACE_FAILED");
    uint32_t gpu_count = 0;
    if (vkEnumeratePhysicalDevices(instance_, &gpu_count, nullptr) != VK_SUCCESS ||
        gpu_count == 0)
        return fail(error, "VULKAN_NO_GPU");
    std::vector<VkPhysicalDevice> gpus(gpu_count);
    if (vkEnumeratePhysicalDevices(instance_, &gpu_count, gpus.data()) != VK_SUCCESS)
        return fail(error, "VULKAN_ENUM_GPU_FAILED");
    for (auto gpu : gpus) {
        uint32_t families = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(gpu, &families, nullptr);
        std::vector<VkQueueFamilyProperties> props(families);
        vkGetPhysicalDeviceQueueFamilyProperties(gpu, &families, props.data());
        for (uint32_t i = 0; i < families; ++i) {
            VkBool32 present = VK_FALSE;
            vkGetPhysicalDeviceSurfaceSupportKHR(gpu, i, surface_, &present);
            if ((props[i].queueFlags & (VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_COMPUTE_BIT)) ==
                (VK_QUEUE_GRAPHICS_BIT | VK_QUEUE_COMPUTE_BIT) && present) {
                gpu_ = gpu;
                family_ = i;
                break;
            }
        }
        if (gpu_) break;
    }
    if (!gpu_) return fail(error, "VULKAN_NO_GRAPHICS_PRESENT_QUEUE");

    VkResult properties = VK_SUCCESS;
    uint32_t n = 0;
    properties = vkEnumerateDeviceExtensionProperties(gpu_, nullptr, &n, nullptr);
    if (properties != VK_SUCCESS) return fail(error, "VULKAN_EXTENSION_QUERY_FAILED");
    std::vector<VkExtensionProperties> supported(n);
    vkEnumerateDeviceExtensionProperties(gpu_, nullptr, &n, supported.data());
    if (std::none_of(supported.begin(), supported.end(), [](const auto& value) {
            return std::strcmp(value.extensionName, VK_KHR_SWAPCHAIN_EXTENSION_NAME) == 0;
        }))
        return fail(error, "VULKAN_NO_SWAPCHAIN_EXTENSION");

    context_ = {};
    if (!negotiateVulkanDevice(negotiation_,
            &context_, instance_, gpu_, surface_, vkGetInstanceProcAddr,
            &createDeviceWrapper, this))
        return fail(error, "VULKAN_CEMU_DEVICE_NEGOTIATION_FAILED");
    device_ = context_.device;
    // Never fall back to another GPU/queue silently, or present on a queue
    // that cannot own Cemu's frame image.
    if (!context_.device || context_.gpu != gpu_ ||
        context_.queue_family_index != family_ ||
        context_.presentation_queue_family_index != family_)
        return fail(error, "VULKAN_QUEUE_OR_DEVICE_INCOMPATIBLE");
    device_ = context_.device;
    queue_ = context_.queue;
    if (!queue_) return fail(error, "VULKAN_NO_RENDER_QUEUE");

    frontend_ = {};
    frontend_.interface_type = RETRO_HW_RENDER_INTERFACE_VULKAN;
    frontend_.interface_version = RETRO_HW_RENDER_INTERFACE_VULKAN_VERSION;
    frontend_.handle = this;
    frontend_.instance = instance_;
    frontend_.gpu = gpu_;
    frontend_.device = device_;
    frontend_.get_device_proc_addr = vkGetDeviceProcAddr;
    frontend_.get_instance_proc_addr = vkGetInstanceProcAddr;
    frontend_.queue = queue_;
    frontend_.queue_index = family_;
    frontend_.set_image = &setImage;
    frontend_.get_sync_index = &getSyncIndex;
    frontend_.get_sync_index_mask = &getSyncMask;
    frontend_.set_command_buffers = &setCommandBuffers;
    frontend_.wait_sync_index = &waitSync;
    frontend_.lock_queue = &lockQueue;
    frontend_.unlock_queue = &unlockQueue;
    frontend_.set_signal_semaphore = &setSignalSemaphore;
    return createSwapchain(error);
}

bool CartdeckVulkanPresenter::createSwapchain(std::string* error) {
    VkSurfaceCapabilitiesKHR caps{};
    if (vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpu_, surface_, &caps) != VK_SUCCESS)
        return fail(error, "VULKAN_SURFACE_CAPABILITIES_FAILED");
    if (!(caps.supportedUsageFlags & VK_IMAGE_USAGE_TRANSFER_DST_BIT))
        return fail(error, "VULKAN_SWAPCHAIN_MISSING_TRANSFER_DST");
    uint32_t formats = 0;
    if (vkGetPhysicalDeviceSurfaceFormatsKHR(gpu_, surface_, &formats, nullptr) != VK_SUCCESS ||
        !formats)
        return fail(error, "VULKAN_SURFACE_FORMATS_MISSING");
    std::vector<VkSurfaceFormatKHR> candidates(formats);
    vkGetPhysicalDeviceSurfaceFormatsKHR(gpu_, surface_, &formats, candidates.data());
    VkSurfaceFormatKHR chosen = candidates.front();
    for (const auto& candidate : candidates) {
        if (candidate.format == VK_FORMAT_R8G8B8A8_UNORM ||
            candidate.format == VK_FORMAT_B8G8R8A8_UNORM) {
            chosen = candidate;
            break;
        }
    }
    if (chosen.format == VK_FORMAT_UNDEFINED)
        return fail(error, "VULKAN_SWAPCHAIN_UNDEFINED_FORMAT");
    surface_format_ = chosen.format;
    extent_ = clampExtent(caps, window_);
    uint32_t count = std::max<uint32_t>(2, caps.minImageCount);
    if (caps.maxImageCount && count > caps.maxImageCount)
        count = caps.maxImageCount;
    VkSwapchainCreateInfoKHR req{VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR};
    req.surface = surface_;
    req.minImageCount = count;
    req.imageFormat = chosen.format;
    req.imageColorSpace = chosen.colorSpace;
    req.imageExtent = extent_;
    req.imageArrayLayers = 1;
    req.imageUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT;
    req.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    if (!uprightSurfaceTransform(caps, &req.preTransform))
        return fail(error, "VULKAN_SURFACE_IDENTITY_TRANSFORM_UNSUPPORTED");
    surface_current_transform_ = caps.currentTransform;
    surface_pre_transform_ = req.preTransform;
    req.compositeAlpha = (caps.supportedCompositeAlpha &
                          VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) ?
                         VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR :
                         VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR;
    if (!(caps.supportedCompositeAlpha & req.compositeAlpha))
        return fail(error, "VULKAN_COMPOSITE_ALPHA_UNSUPPORTED");
    req.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    req.clipped = VK_TRUE;
    continuity_.configure(req);
    const VkResult created = vkCreateSwapchainKHR(device_, &req, nullptr, &swapchain_);
    if (created != VK_SUCCESS) {
        if (error) *error = "VULKAN_SWAPCHAIN_CREATE_FAILED_RESULT_" + std::to_string(created);
        return false;
    }
    continuity_.created();
    uint32_t image_count = 0;
    if (vkGetSwapchainImagesKHR(device_, swapchain_, &image_count, nullptr) != VK_SUCCESS ||
        image_count < 2)
        return fail(error, "VULKAN_SWAPCHAIN_IMAGES_MISSING");
    images_.resize(image_count);
    if (vkGetSwapchainImagesKHR(device_, swapchain_, &image_count,
                               images_.data()) != VK_SUCCESS)
        return fail(error, "VULKAN_SWAPCHAIN_IMAGE_ENUM_FAILED");
    VkCommandPoolCreateInfo cp{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};
    cp.queueFamilyIndex = family_;
    cp.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    if (vkCreateCommandPool(device_, &cp, nullptr, &pool_) != VK_SUCCESS)
        return fail(error, "VULKAN_COMMAND_POOL_FAILED");
    commands_.resize(image_count);
    VkCommandBufferAllocateInfo alloc{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};
    alloc.commandPool = pool_;
    alloc.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    alloc.commandBufferCount = image_count;
    if (vkAllocateCommandBuffers(device_, &alloc, commands_.data()) != VK_SUCCESS)
        return fail(error, "VULKAN_COMMAND_ALLOCATE_FAILED");
    VkSemaphoreCreateInfo sem{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    if (vkCreateSemaphore(device_, &sem, nullptr, &acquired_sem_) != VK_SUCCESS)
        return fail(error, "VULKAN_SEMAPHORE_FAILED");
    // Presentation can outlive a submit fence. Reuse a present semaphore only
    // after WSI reacquires its image, not merely after the blit finished.
    rendered_sems_.resize(image_count, VK_NULL_HANDLE);
    for (auto& semaphore : rendered_sems_)
        if (vkCreateSemaphore(device_, &sem, nullptr, &semaphore) != VK_SUCCESS)
            return fail(error, "VULKAN_PRESENT_SEMAPHORE_FAILED");
    VkFenceCreateInfo fence{VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
    if (vkCreateFence(device_, &fence, nullptr, &frame_fence_) != VK_SUCCESS)
        return fail(error, "VULKAN_FRAME_FENCE_CREATE_FAILED");
    frame_inflight_ = false;
    return true;
}

bool CartdeckVulkanPresenter::waitFrame(std::string* error) {
    if (!frame_inflight_) return true;
    WiiuMetrics::gpu(2);
    const auto started = WiiuMetrics::nowUs();
    // Never hold the shared queue lock while waiting: Cemu may need that
    // lock to submit work that allows an earlier guest dependency to finish.
    const auto completed = waitFrameFence(device_, frame_fence_, vkWaitForFences, error);
    const auto elapsed = WiiuMetrics::nowUs() - started;
    WiiuMetrics::fence_wait_us.store(elapsed);
    WiiuMetrics::maximum(WiiuMetrics::max_fence_us, elapsed);
    if (!completed) return false;
    frame_inflight_ = false;
    WiiuMetrics::gpu(0);
    return true;
}

bool CartdeckVulkanPresenter::acquire(std::string* error) {
    if (!ready()) return fail(error, "VULKAN_RENDERER_NOT_READY");
    if (acquired_) return fail(error, "VULKAN_FRAME_ALREADY_ACQUIRED");
    if (sync_failed_) return fail(error, "VULKAN_SYNC_FENCE_FAILED");
    if (!waitFrame(error)) return false;
    if (resize_pending_) {
        WiiuMetrics::gpu(5);
        if (!retireSwapchain(false, error)) return false;
        if (!createSwapchain(error)) return false;
        resize_pending_ = false;
    }
    saw_image_ = false;
    unsupported_command_buffers_ = false;
    source_semaphores_.clear();
    reuse_signal_ = VK_NULL_HANDLE;
    WiiuMetrics::gpu(1);
    VkResult ret = vkAcquireNextImageKHR(
        device_, swapchain_, 1000000000ULL, acquired_sem_, VK_NULL_HANDLE, &image_index_);
    WiiuMetrics::gpu(0);
    if (ret == VK_TIMEOUT || ret == VK_NOT_READY) return fail(error, "VULKAN_ACQUIRE_TIMEOUT_1S");
    if (ret == VK_ERROR_OUT_OF_DATE_KHR) {
        resize_pending_ = true;
        return fail(error, "VULKAN_ACQUIRE_OUT_OF_DATE_RETRY");
    }
    // SUBOPTIMAL remains usable. Surface callbacks or OUT_OF_DATE drive recreation.
    if (ret != VK_SUCCESS && ret != VK_SUBOPTIMAL_KHR) return fail(error, "VULKAN_ACQUIRE_FAILED");
    acquired_ = true;
    return true;
}

bool CartdeckVulkanPresenter::present(
    unsigned width, unsigned height, std::string* error) {
    if (!acquired_ || !ready()) return fail(error, "VULKAN_NO_ACQUIRED_FRAME");
    // Never submit unowned queue-family images or pretend a NULL frame exists.
    if (!saw_image_ || unsupported_command_buffers_ ||
        (source_family_ != VK_QUEUE_FAMILY_IGNORED && source_family_ != family_))
        return fail(error, "VULKAN_CORE_FRAME_UNSUPPORTED");
    if (!source_image_.create_info.image || width == 0 || height == 0)
        return fail(error, "VULKAN_INVALID_CORE_IMAGE");
    VkFormatProperties srcFormat{}, dstFormat{};
    vkGetPhysicalDeviceFormatProperties(gpu_, source_image_.create_info.format, &srcFormat);
    vkGetPhysicalDeviceFormatProperties(gpu_, surface_format_, &dstFormat);
    if (!(srcFormat.optimalTilingFeatures & VK_FORMAT_FEATURE_BLIT_SRC_BIT) ||
        !(dstFormat.optimalTilingFeatures & VK_FORMAT_FEATURE_BLIT_DST_BIT))
        return fail(error, "VULKAN_FRAME_FORMAT_CANNOT_BLIT");

    VkCommandBuffer cmd = commands_[image_index_];
    if (vkResetCommandBuffer(cmd, 0) != VK_SUCCESS)
        return fail(error, "VULKAN_COMMAND_RESET_FAILED");
    VkCommandBufferBeginInfo begin{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
    begin.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    if (vkBeginCommandBuffer(cmd, &begin) != VK_SUCCESS)
        return fail(error, "VULKAN_COMMAND_BEGIN_FAILED");

    VkImageMemoryBarrier bars[2]{};
    for (auto& b : bars) {
        b.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        b.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        b.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
        b.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        b.subresourceRange.levelCount = 1;
        b.subresourceRange.layerCount = 1;
    }
    bars[0].image = source_image_.create_info.image;
    bars[0].oldLayout = source_image_.image_layout;
    bars[0].newLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
    bars[0].srcAccessMask = VK_ACCESS_MEMORY_WRITE_BIT;
    bars[0].dstAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
    bars[1].image = images_[image_index_];
    bars[1].oldLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    bars[1].newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    bars[1].dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                         VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr,
                         0, nullptr, 2, bars);

    // Clear only the uncovered bars. This never counts as guest image evidence.
    VkClearColorValue black{};
    black.float32[3] = 1.0f;
    vkCmdClearColorImage(cmd, bars[1].image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        &black, 1, &bars[1].subresourceRange);
    VkImageMemoryBarrier afterClear = bars[1];
    afterClear.oldLayout = afterClear.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    afterClear.srcAccessMask = afterClear.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                         0, 0, nullptr, 0, nullptr, 1, &afterClear);
    uint32_t fitWidth = extent_.width;
    uint32_t fitHeight = static_cast<uint32_t>(uint64_t(fitWidth) * height / width);
    if (fitHeight > extent_.height) {
        fitHeight = extent_.height;
        fitWidth = static_cast<uint32_t>(uint64_t(fitHeight) * width / height);
    }
    fitWidth = std::max(1u, fitWidth);
    fitHeight = std::max(1u, fitHeight);
    const int32_t left = static_cast<int32_t>((extent_.width - fitWidth) / 2);
    const int32_t top = static_cast<int32_t>((extent_.height - fitHeight) / 2);

    VkImageBlit blit{};
    blit.srcSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    blit.srcSubresource.layerCount = 1;
    blit.dstSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    blit.dstSubresource.layerCount = 1;
    blit.srcOffsets[1] = {static_cast<int32_t>(width), static_cast<int32_t>(height), 1};
    blit.dstOffsets[0] = {left, top, 0};
    blit.dstOffsets[1] = {left + static_cast<int32_t>(fitWidth), top + static_cast<int32_t>(fitHeight), 1};
    vkCmdBlitImage(cmd, bars[0].image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                   bars[1].image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                   1, &blit, VK_FILTER_NEAREST);

    bars[0].oldLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
    bars[0].newLayout = source_image_.image_layout;
    bars[0].srcAccessMask = VK_ACCESS_TRANSFER_READ_BIT;
    bars[0].dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
    bars[1].oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    bars[1].newLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    bars[1].srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    bars[1].dstAccessMask = 0;
    vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT,
                         VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, 0, 0, nullptr,
                         0, nullptr, 2, bars);
    if (vkEndCommandBuffer(cmd) != VK_SUCCESS)
        return fail(error, "VULKAN_COMMAND_END_FAILED");

    std::vector<VkSemaphore> waiting{acquired_sem_};
    waiting.insert(waiting.end(), source_semaphores_.begin(), source_semaphores_.end());
    std::vector<VkPipelineStageFlags> stages(waiting.size(), VK_PIPELINE_STAGE_TRANSFER_BIT);
    std::vector<VkSemaphore> signalling{rendered_sems_[image_index_]};
    if (reuse_signal_ && reuse_signal_ != rendered_sems_[image_index_]) signalling.push_back(reuse_signal_);

    VkSubmitInfo submit{VK_STRUCTURE_TYPE_SUBMIT_INFO};
    submit.waitSemaphoreCount = static_cast<uint32_t>(waiting.size());
    submit.pWaitSemaphores = waiting.data();
    submit.pWaitDstStageMask = stages.data();
    submit.commandBufferCount = 1;
    submit.pCommandBuffers = &cmd;
    submit.signalSemaphoreCount = static_cast<uint32_t>(signalling.size());
    submit.pSignalSemaphores = signalling.data();
    if (vkResetFences(device_, 1, &frame_fence_) != VK_SUCCESS)
        return fail(error, "VULKAN_FRAME_FENCE_RESET_FAILED");
    WiiuMetrics::gpu(3);
    {
        std::lock_guard<std::mutex> guard(queue_mutex_);
        if (vkQueueSubmit(queue_, 1, &submit, frame_fence_) != VK_SUCCESS)
            return fail(error, "VULKAN_GPU_SUBMIT_FAILED");
    }
    frame_inflight_ = true;
    VkPresentInfoKHR present_req{VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
    present_req.waitSemaphoreCount = 1;
    present_req.pWaitSemaphores = &rendered_sems_[image_index_];
    present_req.swapchainCount = 1;
    present_req.pSwapchains = &swapchain_;
    present_req.pImageIndices = &image_index_;
    VkResult result;
    WiiuMetrics::gpu(4);
    {
        std::lock_guard<std::mutex> guard(queue_mutex_);
        result = vkQueuePresentKHR(queue_, &present_req);
    }
    if (!waitFrame(error)) return false;
    acquired_ = false;
    if (result == VK_ERROR_OUT_OF_DATE_KHR) {
        resize_pending_ = true;
        return true;  // No presentation counted; recreate at the next frame boundary.
    }
    // Keep a usable SUBOPTIMAL swapchain until an actual surface change.
    if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) return fail(error, "VULKAN_PRESENT_FAILED");
    ++presented_frames_;
    return true;
}


bool CartdeckVulkanPresenter::presentBlank(std::string* error) {
    if (!acquired_ || !ready()) return fail(error, "VULKAN_NO_ACQUIRED_BLANK_FRAME");
    VkCommandBuffer cmd = commands_[image_index_];
    if (vkResetCommandBuffer(cmd, 0) != VK_SUCCESS)
        return fail(error, "VULKAN_BLANK_RESET_FAILED");
    VkCommandBufferBeginInfo begin{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
    begin.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    if (vkBeginCommandBuffer(cmd, &begin) != VK_SUCCESS)
        return fail(error, "VULKAN_BLANK_BEGIN_FAILED");

    VkImageMemoryBarrier barrier{VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER};
    barrier.image = images_[image_index_];
    barrier.oldLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    barrier.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    barrier.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
    barrier.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    barrier.subresourceRange.levelCount = 1;
    barrier.subresourceRange.layerCount = 1;
    barrier.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                         VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr,
                         0, nullptr, 1, &barrier);
    const VkClearColorValue black = {{0.0f, 0.0f, 0.0f, 1.0f}};
    vkCmdClearColorImage(cmd, barrier.image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                         &black, 1, &barrier.subresourceRange);
    barrier.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
    barrier.newLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    barrier.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
    barrier.dstAccessMask = 0;
    vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT,
                         VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, 0, nullptr,
                         0, nullptr, 1, &barrier);
    if (vkEndCommandBuffer(cmd) != VK_SUCCESS)
        return fail(error, "VULKAN_BLANK_END_FAILED");
    const VkPipelineStageFlags stage = VK_PIPELINE_STAGE_TRANSFER_BIT;
    VkSubmitInfo submit{VK_STRUCTURE_TYPE_SUBMIT_INFO};
    submit.waitSemaphoreCount = 1;
    submit.pWaitSemaphores = &acquired_sem_;
    submit.pWaitDstStageMask = &stage;
    submit.commandBufferCount = 1;
    submit.pCommandBuffers = &cmd;
    submit.signalSemaphoreCount = 1;
    submit.pSignalSemaphores = &rendered_sems_[image_index_];
    VkPresentInfoKHR present_req{VK_STRUCTURE_TYPE_PRESENT_INFO_KHR};
    present_req.waitSemaphoreCount = 1;
    present_req.pWaitSemaphores = &rendered_sems_[image_index_];
    present_req.swapchainCount = 1;
    present_req.pSwapchains = &swapchain_;
    present_req.pImageIndices = &image_index_;
    VkResult result;
    if (vkResetFences(device_, 1, &frame_fence_) != VK_SUCCESS)
        return fail(error, "VULKAN_FRAME_FENCE_RESET_FAILED");
    WiiuMetrics::gpu(3);
    {
        std::lock_guard<std::mutex> guard(queue_mutex_);
        if (vkQueueSubmit(queue_, 1, &submit, frame_fence_) != VK_SUCCESS)
            return fail(error, "VULKAN_BLANK_SUBMIT_FAILED");
        frame_inflight_ = true;
        WiiuMetrics::gpu(4);
        result = vkQueuePresentKHR(queue_, &present_req);
    }
    if (!waitFrame(error)) return false;
    acquired_ = false;
    if (result == VK_ERROR_OUT_OF_DATE_KHR) { resize_pending_ = true; return true; }
    // Keep a usable SUBOPTIMAL swapchain until an actual surface change.
    if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR)
        return fail(error, "VULKAN_BLANK_PRESENT_FAILED");
    // A black loading frame MUST NOT increment confirmed game-image callbacks.
    return true;
}

bool CartdeckVulkanPresenter::retireSwapchain(bool includeSurface, std::string* error) {
    if (!waitFrame(error)) return false;
    // A submit fence does not prove that WSI consumed a present semaphore.
    // Keep the old WSI resources until guest shutdown, avoiding a device-wide
    // idle wait while Cemu's GPU thread still needs the shared queue.
    if (retired_.size() >= 16) return fail(error, "VULKAN_SURFACE_RESTART_REQUIRED_AFTER_16_RECREATIONS");
    continuity_.retire(swapchain_, !includeSurface);
    retired_.push_back({swapchain_, pool_, frame_fence_, acquired_sem_, std::move(rendered_sems_),
        includeSurface ? surface_ : VK_NULL_HANDLE, includeSurface ? window_ : nullptr});
    swapchain_ = VK_NULL_HANDLE;
    pool_ = VK_NULL_HANDLE;
    frame_fence_ = VK_NULL_HANDLE;
    acquired_sem_ = VK_NULL_HANDLE;
    if (includeSurface) { surface_ = VK_NULL_HANDLE; window_ = nullptr; }
    commands_.clear();
    images_.clear();
    acquired_ = false;
    frame_inflight_ = false;
    saw_image_ = false;
    return true;
}

void CartdeckVulkanPresenter::destroySwapchain() {
    if (acquired_sem_) vkDestroySemaphore(device_, acquired_sem_, nullptr);
    for (auto semaphore : rendered_sems_)
        if (semaphore) vkDestroySemaphore(device_, semaphore, nullptr);
    rendered_sems_.clear();
    if (frame_fence_) vkDestroyFence(device_, frame_fence_, nullptr);
    frame_fence_ = VK_NULL_HANDLE;
    frame_inflight_ = false;
    acquired_sem_ = VK_NULL_HANDLE;
    if (pool_) vkDestroyCommandPool(device_, pool_, nullptr);
    pool_ = VK_NULL_HANDLE;
    if (swapchain_) vkDestroySwapchainKHR(device_, swapchain_, nullptr);
    swapchain_ = VK_NULL_HANDLE;
    images_.clear();
    commands_.clear();
    acquired_ = false;
    saw_image_ = false;
}

bool CartdeckVulkanPresenter::replaceSurface(ANativeWindow* window, std::string* error) {
    if (!window || !device_) return fail(error, "VULKAN_REPLACEMENT_SURFACE_INVALID");
    WiiuMetrics::gpu(5);
    if (window == window_) {
        if (!retireSwapchain(false, error)) return false;
        resize_pending_ = false;
        return createSwapchain(error);
    }
    if (!retireSwapchain(true, error)) return false;
    ANativeWindow_acquire(window);
    window_ = window;
    VkAndroidSurfaceCreateInfoKHR info{VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR};
    info.window = window_;
    if (vkCreateAndroidSurfaceKHR(instance_, &info, nullptr, &surface_) != VK_SUCCESS)
        return fail(error, "VULKAN_SURFACE_RECREATE_FAILED");
    VkBool32 supported = VK_FALSE;
    if (vkGetPhysicalDeviceSurfaceSupportKHR(gpu_, family_, surface_, &supported) != VK_SUCCESS || !supported)
        return fail(error, "VULKAN_REPLACEMENT_QUEUE_CANNOT_PRESENT");
    resize_pending_ = false;
    return createSwapchain(error);
}

void CartdeckVulkanPresenter::stop() {
    if (device_) {
        WiiuMetrics::gpu(6);
        const auto begin = WiiuMetrics::nowUs();
        const auto result = vkDeviceWaitIdle(device_);
        WiiuMetrics::shutdown_idle_us.store(WiiuMetrics::nowUs() - begin);
        WiiuMetrics::shutdown_idle_result.store(result);
        WiiuMetrics::gpu(0);
    }
    destroySwapchain();
    for (const auto& old : retired_) {
        if (old.acquired) vkDestroySemaphore(device_, old.acquired, nullptr);
        for (auto semaphore : old.rendered)
            if (semaphore) vkDestroySemaphore(device_, semaphore, nullptr);
        if (old.fence) vkDestroyFence(device_, old.fence, nullptr);
        if (old.pool) vkDestroyCommandPool(device_, old.pool, nullptr);
        if (old.swapchain) vkDestroySwapchainKHR(device_, old.swapchain, nullptr);
        if (old.surface) vkDestroySurfaceKHR(instance_, old.surface, nullptr);
        if (old.window) ANativeWindow_release(old.window);
    }
    retired_.clear();
    if (negotiation_ && negotiation_->destroy_device && device_)
        negotiation_->destroy_device();
    if (device_) vkDestroyDevice(device_, nullptr);
    device_ = VK_NULL_HANDLE;
    queue_ = VK_NULL_HANDLE;
    if (surface_) vkDestroySurfaceKHR(instance_, surface_, nullptr);
    surface_ = VK_NULL_HANDLE;
    if (instance_) vkDestroyInstance(instance_, nullptr);
    instance_ = VK_NULL_HANDLE;
    if (window_) ANativeWindow_release(window_);
    window_ = nullptr;
    negotiation_ = nullptr;
    acquired_ = false;
    saw_image_ = false;
}
