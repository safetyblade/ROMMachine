#include "StereoFifo.h"
#include "FramePacer.h"
#include "FrameFence.h"
#include "SurfaceMailbox.h"
#include "SurfacePresentation.h"
#include <future>
#include <cassert>
#include <thread>
#include <vector>

static VkResult wait_result = VK_SUCCESS;
static VkResult VKAPI_CALL wait(VkDevice, uint32_t count, const VkFence* fence, VkBool32 all, uint64_t timeout) {
    assert(count == 1 && fence && all && timeout == 5000000000ULL);
    return wait_result;
}
struct Window { std::atomic<int> refs{1}; };
static void retain(Window* w) { ++w->refs; }
static void release(Window* w) { --w->refs; }
int main() {
    // UI remains able to publish a new surface while the VM is stalled with
    // a snapshot. Both retained references have exactly one eventual release.
    Window oldWindow, newWindow;
    SurfaceMailbox<Window> mailbox(retain, release);
    mailbox.update(&oldWindow);
    std::promise<void> snapshotTaken, finishGpuWait;
    auto resume = finishGpuWait.get_future();
    std::thread vm([&] {
        auto snapshot = mailbox.snapshot(0);
        assert(snapshot.window.get() == &oldWindow && snapshot.generation == 1);
        snapshotTaken.set_value();
        resume.wait();
    });
    snapshotTaken.get_future().wait();
    auto ui = std::async(std::launch::async, [&] { mailbox.update(&newWindow); });
    assert(ui.wait_for(std::chrono::seconds(1)) == std::future_status::ready);
    assert(oldWindow.refs == 1 && newWindow.refs == 1);
    mailbox.clear();
    finishGpuWait.set_value();
    vm.join();
    assert(oldWindow.refs == 0 && newWindow.refs == 0);

    VkSurfaceCapabilitiesKHR caps{};
    caps.supportedTransforms = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR |
        VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR | VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR;
    VkSurfaceTransformFlagBitsKHR transform{};
    for (auto rotation : {VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR,
                         VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR,
                         VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR}) {
        caps.currentTransform = rotation;
        assert(uprightSurfaceTransform(caps, &transform));
        assert(transform == VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR);
    }
    caps.supportedTransforms = VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR;
    assert(!uprightSurfaceTransform(caps, &transform));

    FramePacer pacer;
    pacer.reset(1000000);
    assert(pacer.next(1000000, 20000) == 1020000); // PAL / 50fps
    assert(pacer.next(1010000, 20000) == 1040000); // no display-refresh speedup
    assert(pacer.next(2000000, 20000) == 2020000); // no catch-up burst after a stall
    pacer.reset(3000000);
    assert(pacer.next(3000000, 8333) == 3008333); // 120fps core, not hardcoded 60
    std::string error;
    assert(waitFrameFence({}, {}, wait, &error));
    wait_result = VK_TIMEOUT;
    assert(!waitFrameFence({}, {}, wait, &error) && error == "VULKAN_FRAME_FENCE_TIMEOUT_5S");
    wait_result = VK_ERROR_DEVICE_LOST;
    assert(!waitFrameFence({}, {}, wait, &error) && error == "VULKAN_FRAME_FENCE_DEVICE_ERROR");
    assert(!waitFrameFence({}, {}, wait, nullptr));

    StereoFifo<4> small;
    int16_t stereo[] = {1,-1,2,-2,3,-3,4,-4,5,-5};
    int16_t out[10]{};
    assert(small.push(stereo, 5) == 4); // partial acceptance is truthful
    assert(small.pop(out, 2) == 2 && out[0] == 1 && out[3] == -2);
    assert(small.push(stereo + 8, 1) == 1); // wraparound keeps stereo paired
    assert(small.pop(out, 4) == 3 && out[0] == 3 && out[5] == -5);
    assert(small.pop(out, 1) == 0);
    small.push(stereo, 4);
    small.discard();
    assert(small.available() == 0);

    StereoFifo<257> queue;
    constexpr int frames = 100000;
    std::thread producer([&] {
        for (int i = 0; i < frames; ++i) {
            int16_t pair[] = {static_cast<int16_t>(i % 30000), static_cast<int16_t>(-(i % 30000))};
            while (!queue.push(pair, 1)) std::this_thread::yield();
        }
    });
    for (int i = 0; i < frames; ++i) {
        int16_t pair[2];
        while (!queue.pop(pair, 1)) std::this_thread::yield();
        assert(pair[0] == i % 30000 && pair[1] == -(i % 30000));
    }
    producer.join();
    assert(queue.available() == 0);
}
