"""Deterministic adaptation of the pinned Android runtime, not its frontend."""
import pathlib
import re
import shutil


def prepare(source, support):
    source, support = pathlib.Path(source), pathlib.Path(support)
    cpp = source / 'emulator-core/src/main/cpp'
    shutil.copyfile(support / 'cartdeck_progress.h', cpp / 'cartdeck_progress.h')
    # Native-only Gradle build; the upstream branded application is never built.
    settings = source / 'settings.gradle'
    settings.write_text(settings.read_text().replace("include ':app'", ''))
    cmake = cpp / 'CMakeLists.txt'
    cmake.write_text(cmake.read_text().replace('OUTPUT_NAME "e"', 'OUTPUT_NAME "cartdeck_xenia"'))
    gradle = source / 'emulator-core/build.gradle'
    gradle.write_text(gradle.read_text().replace("abiFilters 'arm64-v8a'", "abiFilters 'arm64-v8a'\n                arguments '-DANDROID_STL=c++_static'"))
    jni = cpp / 'emulator_xendroid.cpp'
    text = jni.read_text()
    text = '#include "cartdeck_progress.h"\n' + text
    text = text.replace('    g_context = env->NewGlobalRef(context);',
                        '    g_context = env->NewGlobalRef(context);\n    cartdeck::mark(cartdeck::HOST_INIT);')
    # Register only the retained ABI. Metadata/install/delete/profile UIs remain outside this adapter.
    start = text.index('    static const JNINativeMethod methods[] = {', text.index('int register_xendroid_Emulator'))
    end = text.index('    return env->RegisterNatives', start)
    text = text[:start] + '''    static const JNINativeMethod methods[] = {
        {"setup_context", "(Landroid/content/Context;)V", (void*)j_setup_context},
        {"setup_launch_args", "([Ljava/lang/String;)V", (void*)j_setup_launch_args},
        {"setup_uri_info_list_file", "(Ljava/lang/String;)V", (void*)j_setup_uri_info_list_file},
        {"cartdeck_report", "()Ljava/lang/String;", (void*)j_cartdeck_report},
    };
''' + text[end:]
    text = text.replace('int register_xendroid_Emulator(JNIEnv* env)', '''static jstring j_cartdeck_report(JNIEnv* env, jobject) {
    char out[2048]; cartdeck::report(out, sizeof(out)); return env->NewStringUTF(out);
}
int register_xendroid_Emulator(JNIEnv* env)''')
    # Release JNI strings; do not pin per-launch arrays indefinitely.
    text = text.replace('g_launch_args.push_back(env->GetStringUTFChars(arg,NULL) );',
                        'const char* p = env->GetStringUTFChars(arg,nullptr);\n'
                        '        g_launch_args.emplace_back(p); env->ReleaseStringUTFChars(arg,p); env->DeleteLocalRef(arg);')
    jni.write_text(text)
    host = cpp / 'xendroid_emu.cpp'
    text = '#include "cartdeck_progress.h"\n' + host.read_text()
    text = text.replace('DECLARE_bool(host_present_from_non_ui_thread);', 'DECLARE_bool(host_present_from_non_ui_thread);\nDECLARE_string(vulkan_lib_path);')
    text = text.replace('cvars::host_present_from_non_ui_thread = true;', 'cvars::host_present_from_non_ui_thread = true;\n    cvars::vulkan_lib_path.clear(); // CARTDECK baseline is always the system Vulkan loader.\n    cvars::android_soft_keyboard = false; cvars::android_disc_swap = false; cvars::android_message_box = false;')
    text = text.replace('XELOGE("Failed to setup emulator: {:08X}", result);',
                        'cartdeck::fail(result); XELOGE("Failed to setup emulator: {:08X}", result);')
    text = text.replace('XELOGE("Failed to setup subsystems: {:08X}", result);',
                        'cartdeck::fail(result); XELOGE("Failed to setup subsystems: {:08X}", result);')
    text = text.replace('    app_context().CallInUIThread(\n            [this]() { emu_window->SetupGraphicsSystemPresenterPainting(); });',
                        '    cartdeck::mark(cartdeck::GPU_READY);\n    cartdeck::mark(cartdeck::AUDIO_INPUT_READY);\n'
                        '    app_context().CallInUIThread(\n            [this]() { emu_window->SetupGraphicsSystemPresenterPainting(); });')
    text = text.replace('xe::FatalError(fmt::format("Failed to launch target: {:08X}", result));',
                        'cartdeck::fail(result); xe::FatalError(fmt::format("Failed to launch target: {:08X}", result));')
    text = text.replace('''    auto xam = emu->kernel_state()->GetKernelModule''',
                        '''    cartdeck::mark(cartdeck::CONTENT_OPENED); cartdeck::mark(cartdeck::GUEST_BOOT);
    auto xam = emu->kernel_state()->GetKernelModule''')
    # Upstream quit is empty. Request the existing UI loop to drain, then destroy the guest.
    text = text.replace('    void quit(){\n    }',
                        '    void quit(){\n        if (g_app_context) g_app_context->RequestDeferredQuit();\n    }')
    text = text.replace('''        if(g_windowed_app->OnInitialize()){
            wnd_ctx.main_loop();
        }''', '''        if(g_windowed_app->OnInitialize()){
            wnd_ctx.main_loop();
        }
        g_windowed_app.reset();
        g_windowed_app_ref = nullptr; g_app_context = nullptr;
        cartdeck::exited = true;''')
    host.write_text(text)
    jit = cpp / 'xenia/src/xenia/cpu/backend/a64/a64_code_cache.cc'
    text = '#include "cartdeck_progress.h"\n' + jit.read_text()
    text = text.replace('''#if defined(__linux__)
  if (!cvars::a64_perf_map)''', '''  cartdeck::jit_blocks.fetch_add(1); // JIT compilation, not guest instruction retirement.
#if defined(__linux__)
  if (!cvars::a64_perf_map)''')
    jit.write_text(text)
    # Header must also be visible to the separately compiled ARM64 backend target.
    cmake.write_text(cmake.read_text() + '\ntarget_include_directories(xenia-cpu-backend-a64 PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")\n')
    # Device evidence comes from the actual selected physical device, not Android feature flags.
    device = cpp / 'xenia/src/xenia/ui/vulkan/vulkan_device.cc'
    text = '#include "cartdeck_progress.h"\n' + device.read_text()
    text = text.replace('ifn.vkGetPhysicalDeviceProperties(physical_device, &properties);',
        'ifn.vkGetPhysicalDeviceProperties(physical_device, &properties);\n'
        '  cartdeck::gpu_vendor = properties.vendorID; cartdeck::gpu_device = properties.deviceID;\n'
        '  cartdeck::gpu_driver = properties.driverVersion; cartdeck::vulkan_api = properties.apiVersion;')
    text = text.replace('if (!supported_features.independentBlend) {',
        'if (!supported_features.independentBlend) {\n'
        '      cartdeck::missing_capability = "independentBlend"; cartdeck::fail(0x36000001);')
    text = text.replace('if (device_create_result != VK_SUCCESS) {',
        'cartdeck::device_result = device_create_result;\n  if (device_create_result != VK_SUCCESS) {\n'
        '    cartdeck::fail(0x36000002);')
    device.write_text(text)
    presenter = cpp / 'xenia/src/xenia/ui/vulkan/vulkan_presenter.cc'
    text = '#include "cartdeck_progress.h"\n' + presenter.read_text()
    text = text.replace('switch (present_result) {',
        'cartdeck::present_result = present_result;\n'
        '  if (present_result == VK_SUCCESS || present_result == VK_SUBOPTIMAL_KHR) ++cartdeck::host_presents;\n'
        '  if (present_result == VK_ERROR_DEVICE_LOST) cartdeck::fail(0x36000003);\n'
        '  switch (present_result) {')
    presenter.write_text(text)
    gpu = cpp / 'xenia/src/xenia/gpu/vulkan/vulkan_command_processor.cc'
    text = '#include "cartdeck_progress.h"\n' + gpu.read_text()
    text = text.replace('xe::RecordGuestPresent();', 'xe::RecordGuestPresent(); ++cartdeck::guest_swaps;')
    gpu.write_text(text)
    cmake.write_text(cmake.read_text() + '\ntarget_include_directories(xenia-ui-vulkan PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")\n'
                     'target_include_directories(xenia-gpu-vulkan PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")\n')
    # Android trigger values were ignored upstream. Preserve the full 0..255 range.
    driver = cpp / 'xe_android_input_driver.cpp'
    text = driver.read_text().replace('                                left_trigger = 0xFF;', '                                left_trigger = static_cast<uint8_t>(std::clamp(ks.value, 0, 255));')
    text = text.replace('                                right_trigger = 0xFF;', '                                right_trigger = static_cast<uint8_t>(std::clamp(ks.value, 0, 255));')
    text = '#include <algorithm>\n' + text
    # GetState also reads key_status_; protect it with the same mutex as OnKey.
    text = text.replace('packet_number_++;', 'std::lock_guard<std::mutex> state_lock(key_status_mutex_);\n                packet_number_++;')
    assert text.count('std::clamp(ks.value, 0, 255)') == 2
    assert 'out_caps->gamepad.left_trigger = 0xFF;' in text
    assert 'out_caps->gamepad.right_trigger = 0xFF;' in text
    driver.write_text(text)
    for marker in ('j_cartdeck_report', 'cartdeck::HOST_INIT'):
        assert marker in jni.read_text(), marker
    assert 'cartdeck::GPU_READY' in host.read_text()
