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
    char out[512]; cartdeck::report(out, sizeof(out)); return env->NewStringUTF(out);
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
    for marker in ('j_cartdeck_report', 'cartdeck::HOST_INIT'):
        assert marker in jni.read_text(), marker
    assert 'cartdeck::GPU_READY' in host.read_text()
