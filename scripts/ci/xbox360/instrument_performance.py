"""Bounded probes for the pinned Xenia-owned renderer; no graphics policy hacks."""
from pathlib import Path
import shutil


def instrument(cpp, support):
    cpp, support = Path(cpp), Path(support)
    shutil.copyfile(support / 'cartdeck_performance.h', cpp / 'cartdeck_performance.h')
    root = cpp / 'xenia/src/xenia'

    def edit(path, old, new):
        text = path.read_text()
        assert text.count(old) == 1, 'Pinned Xenia performance hook drift: ' + str(path) + ':' + old[:80]
        path.write_text(text.replace(old, new))

    jit = root / 'cpu/backend/a64/a64_assembler.cc'
    edit(jit, '#include <climits>', '#include <climits>\n#include "cartdeck_performance.h"')
    edit(jit, '  // Reset when we leave.', '  cartdeck::perf::Scope cartdeck_jit(cartdeck::perf::Jit);\n  // Reset when we leave.')
    pipeline = root / 'gpu/vulkan/vulkan_pipeline_cache.cc'
    edit(pipeline, '#include "xenia/gpu/vulkan/vulkan_pipeline_cache.h"',
         '#include "xenia/gpu/vulkan/vulkan_pipeline_cache.h"\n#include "cartdeck_performance.h"')
    edit(pipeline, '  VkResult result = dfn.vkCreateGraphicsPipelines(',
         '  const auto cartdeck_pipeline_start = cartdeck::perf::now();\n  VkResult result = dfn.vkCreateGraphicsPipelines(')
    edit(pipeline, '  if (result == VK_SUCCESS) {\n    // Mark the persistent VkPipelineCache dirty',
         '  cartdeck::perf::windows[cartdeck::perf::Pipeline].add(cartdeck::perf::now() - cartdeck_pipeline_start);\n  if (result == VK_SUCCESS) {\n    // Mark the persistent VkPipelineCache dirty')
    edit(pipeline, '  VulkanShader& shader = static_cast<VulkanShader&>(translation.shader());',
         '  cartdeck::perf::Scope cartdeck_shader(cartdeck::perf::Shader);\n  VulkanShader& shader = static_cast<VulkanShader&>(translation.shader());')
    presenter = root / 'ui/vulkan/vulkan_presenter.cc'
    edit(presenter, '  paint_context_.completion_timeline\n      .AwaitMaxSubmissionsPendingAndUpdateCompleted(paint_submission_count);',
         '  { cartdeck::perf::Scope timer(cartdeck::perf::FrameFence);\n  paint_context_.completion_timeline\n      .AwaitMaxSubmissionsPendingAndUpdateCompleted(paint_submission_count); }')
    edit(presenter, '  VkResult acquire_result = dfn.vkAcquireNextImageKHR(',
         '  const auto cartdeck_acquire_start = cartdeck::perf::now();\n  VkResult acquire_result = dfn.vkAcquireNextImageKHR(')
    edit(presenter, '  switch (acquire_result) {',
         '  cartdeck::perf::windows[cartdeck::perf::Acquire].add(cartdeck::perf::now() - cartdeck_acquire_start);\n  switch (acquire_result) {')
    edit(presenter, '  const VkResult submit_result =\n      paint_context_.completion_timeline.AcquireFenceAndSubmit(',
         '  const auto cartdeck_submit_start = cartdeck::perf::now();\n  const VkResult submit_result =\n      paint_context_.completion_timeline.AcquireFenceAndSubmit(')
    edit(presenter, '      paint_context_.completion_timeline.AcquireFenceAndSubmit(\n          vulkan_device_->queue_family_graphics_compute(), 0, 1, &submit_info);',
         '      paint_context_.completion_timeline.AcquireFenceAndSubmit(\n          vulkan_device_->queue_family_graphics_compute(), 0, 1, &submit_info);\n  cartdeck::perf::windows[cartdeck::perf::Submit].add(cartdeck::perf::now() - cartdeck_submit_start);')
    edit(presenter, '    const VulkanDevice::Queue::Acquisition queue_acquisition =\n        vulkan_device_->AcquireQueue(paint_context_.present_queue_family, 0);',
         '    const auto cartdeck_lock_start = cartdeck::perf::now();\n    const VulkanDevice::Queue::Acquisition queue_acquisition =\n        vulkan_device_->AcquireQueue(paint_context_.present_queue_family, 0);\n    cartdeck::perf::windows[cartdeck::perf::QueueLock].add(cartdeck::perf::now() - cartdeck_lock_start);')
    edit(presenter, '    present_result =\n        dfn.vkQueuePresentKHR(queue_acquisition.queue(), &present_info);',
         '    const auto cartdeck_present_start = cartdeck::perf::now();\n    present_result =\n        dfn.vkQueuePresentKHR(queue_acquisition.queue(), &present_info);\n    cartdeck::perf::windows[cartdeck::perf::PresentCall].add(cartdeck::perf::now() - cartdeck_present_start);')
    edit(presenter, 'if (present_result == VK_SUCCESS || present_result == VK_SUBOPTIMAL_KHR) ++cartdeck::host_presents;',
         'if (present_result == VK_SUCCESS || present_result == VK_SUBOPTIMAL_KHR) { ++cartdeck::host_presents; cartdeck::perf::presented(cartdeck::guest_swaps.load()); }')
    gpu = root / 'gpu/vulkan/vulkan_command_processor.cc'
    edit(gpu, 'xe::RecordGuestPresent(); ++cartdeck::guest_swaps;',
         'xe::RecordGuestPresent(); ++cartdeck::guest_swaps; cartdeck::perf::windows[cartdeck::perf::GuestSwap].tick();')
    # XGFX-03A: compare the actual frame options and optional shared-memory
    # refresh time against XGFX-01/-02 owner results. No cvar mutations.
    edit(gpu, '''  if (is_closing_frame) {
    if (cvars::clear_memory_page_state) {
      shared_memory_->SetSystemPageBlocksValidWithGpuDataWritten();
    }''', '''  if (is_closing_frame) {
    if (cartdeck::perf::capture_enabled.load(std::memory_order_relaxed)) {
      cartdeck::perf::note_xgfx_settings(
          cvars::clear_memory_page_state,
          cvars::vulkan_cache_texture_descriptors,
          cvars::vulkan_skip_redundant_fetch_constant_writes,
          cvars::vulkan_fast_register_ranges,
          cvars::vulkan_mid_frame_submission_draws);
    }
    if (cvars::clear_memory_page_state) {
      cartdeck::perf::Scope cartdeck_page_refresh(
          cartdeck::perf::XgfxPageRefresh);
      shared_memory_->SetSystemPageBlocksValidWithGpuDataWritten();
    }''')
    # Unbounded per-draw formatting/log traffic is replaced with a buffered counter.
    # Rendering and async pipeline decisions are unchanged.
    for old in [
        '''    XELOGI(
        "Draw skipped (no interpreter placeholder, real pipeline not ready): "
        "VS {:016X}, PS {:016X}",
        vertex_shader->ucode_data_hash(),
        pixel_shader ? pixel_shader->ucode_data_hash() : 0);''',
        '''    XELOGI("Draw skipped (pipeline not ready yet): VS {:016X}, PS {:016X}",
           vertex_shader->ucode_data_hash(),
           pixel_shader ? pixel_shader->ucode_data_hash() : 0);''']:
        edit(gpu, old, '    if (cartdeck::perf::capture_enabled.load(std::memory_order_relaxed)) cartdeck::perf::skipped_draws.fetch_add(1, std::memory_order_relaxed);')
    audio = cpp / 'xe_aaudio_audio_driver.cpp'
    edit(audio, '#include "xe_aaudio_audio_driver.h"',
         '#include "xe_aaudio_audio_driver.h"\n#include "cartdeck_performance.h"')
    edit(audio, '    stat_gaps_.fetch_add(1, std::memory_order_relaxed);',
         '    if (cartdeck::perf::capture_enabled.load(std::memory_order_relaxed)) cartdeck::perf::audio_underrun_frames.fetch_add(channel_samples_, std::memory_order_relaxed);\n    stat_gaps_.fetch_add(1, std::memory_order_relaxed);')
