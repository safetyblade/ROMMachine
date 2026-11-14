// WIIU-02: embedded, private test-only Cemu Libretro frontend.
// This is a real Android native Libretro/Vulkan path, not an external APK.
// Gameplay, audio output and release readiness still require physical QA.
#include "CartdeckVulkanPresenter.h"
#include <android/native_window_jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <atomic>
#include <cstdarg>
#include <cstring>
#include <filesystem>
#include <string>
#include <unordered_map>
#include <vector>

namespace {
std::atomic<bool> stop_requested{false};
std::atomic<uint32_t> pressed_buttons{0};
std::atomic<unsigned> frame_count{0};

void coreLog(enum retro_log_level level, const char* fmt, ...) {
    va_list values;
    va_start(values, fmt);
    int priority = level == RETRO_LOG_ERROR ? ANDROID_LOG_ERROR :
                   level == RETRO_LOG_WARN ? ANDROID_LOG_WARN : ANDROID_LOG_INFO;
    __android_log_vprint(priority, "CARTDECK_WIIU_CORE", fmt, values);
    va_end(values);
}

struct CoreApi {
    void* library = nullptr;
    decltype(&retro_set_environment) set_environment = nullptr;
    decltype(&retro_set_video_refresh) set_video = nullptr;
    decltype(&retro_set_audio_sample) set_audio = nullptr;
    decltype(&retro_set_audio_sample_batch) set_audio_batch = nullptr;
    decltype(&retro_set_input_poll) set_input_poll = nullptr;
    decltype(&retro_set_input_state) set_input_state = nullptr;
    decltype(&retro_init) init = nullptr;
    decltype(&retro_deinit) deinit = nullptr;
    decltype(&retro_load_game) load_game = nullptr;
    decltype(&retro_unload_game) unload_game = nullptr;
    decltype(&retro_run) run = nullptr;

    template<typename F> bool loadSymbol(F& target, const char* name) {
        target = reinterpret_cast<F>(dlsym(library, name));
        return target != nullptr;
    }
    bool open() {
        // Android only loads the SHA-checked core packaged beside this library.
        library = dlopen("libcemu_libretro_android.so", RTLD_NOW | RTLD_LOCAL);
        if (!library) return false;
        return loadSymbol(set_environment, "retro_set_environment") &&
               loadSymbol(set_video, "retro_set_video_refresh") &&
               loadSymbol(set_audio, "retro_set_audio_sample") &&
               loadSymbol(set_audio_batch, "retro_set_audio_sample_batch") &&
               loadSymbol(set_input_poll, "retro_set_input_poll") &&
               loadSymbol(set_input_state, "retro_set_input_state") &&
               loadSymbol(init, "retro_init") &&
               loadSymbol(deinit, "retro_deinit") &&
               loadSymbol(load_game, "retro_load_game") &&
               loadSymbol(unload_game, "retro_unload_game") &&
               loadSymbol(run, "retro_run");
    }
    void close() {
        if (library) dlclose(library);
        library = nullptr;
    }
};

struct WiiuSession {
    std::string system;
    std::string save;
    std::string disc;
    std::string error;
    std::unordered_map<std::string, std::string> options;
    retro_hw_render_callback hw{};
    const retro_hw_render_context_negotiation_interface_vulkan* negotiation = nullptr;
    CartdeckVulkanPresenter presenter;
    CoreApi core;
    bool got_hw = false;
    bool got_video = false;
    bool loaded = false;
    bool initialized = false;
};

WiiuSession* active = nullptr;

bool environment(unsigned command, void* data) {
    auto* s = active;
    if (!s) return false;
    switch (command) {
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
            if (!data) return false;
            *static_cast<const char**>(data) = s->system.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            if (!data) return false;
            *static_cast<const char**>(data) = s->save.c_str();
            return true;
        case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
            if (!data) return false;
            static_cast<retro_log_callback*>(data)->log = coreLog;
            return true;
        case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
            if (!data) return false;
            *static_cast<unsigned*>(data) = 2;
            return true;
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2: {
            if (!data) return false;
            const auto* options = static_cast<const retro_core_options_v2*>(data);
            if (!options->definitions) return false;
            for (const auto* opt = options->definitions; opt && opt->key; ++opt) {
                if (opt->default_value) s->options[opt->key] = opt->default_value;
            }
            s->options["cemu_gpu_api"] = "Vulkan";
            return true;
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE: {
            if (!data) return false;
            auto* query = static_cast<retro_variable*>(data);
            if (!query->key) return false;
            auto it = s->options.find(query->key);
            query->value = it == s->options.end() ? nullptr : it->second.c_str();
            return it != s->options.end();
        }
        case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
            if (!data) return false;
            *static_cast<bool*>(data) = false;
            return true;
        case RETRO_ENVIRONMENT_SET_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE: {
            if (!data) return false;
            const auto* negotiation =
                static_cast<const retro_hw_render_context_negotiation_interface_vulkan*>(data);
            if (negotiation->interface_type !=
                    RETRO_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE_VULKAN ||
                negotiation->interface_version < 2 || !negotiation->create_device2)
                return false;
            s->negotiation = negotiation;
            return true;
        }
        case RETRO_ENVIRONMENT_SET_HW_RENDER:
            if (!data) return false;
            if (static_cast<retro_hw_render_callback*>(data)->context_type !=
                RETRO_HW_CONTEXT_VULKAN)
                return false;  // No desktop GL / GLES fallback for Cemu.
            s->hw = *static_cast<const retro_hw_render_callback*>(data);
            s->got_hw = true;
            return true;
        case RETRO_ENVIRONMENT_GET_HW_RENDER_INTERFACE:
            if (!data || !s->presenter.ready()) return false;
            *static_cast<const retro_hw_render_interface**>(data) =
                reinterpret_cast<const retro_hw_render_interface*>(s->presenter.interface());
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            return data && *static_cast<const retro_pixel_format*>(data) ==
                RETRO_PIXEL_FORMAT_XRGB8888;
        case RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE:
        case RETRO_ENVIRONMENT_GET_SENSOR_INTERFACE:
        case RETRO_ENVIRONMENT_GET_VFS_INTERFACE:
            return false;  // Never fabricate capabilities.
        case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
        case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
        case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
        case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_DISPLAY:
        case RETRO_ENVIRONMENT_SET_GEOMETRY:
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
            return true;
        case RETRO_ENVIRONMENT_SHUTDOWN:
            stop_requested.store(true);
            return true;
        default:
            return false;
    }
}
void video(const void* data, unsigned width, unsigned height, size_t) {
    auto* s = active;
    if (!s || stop_requested.load()) return;
    if (data != RETRO_HW_FRAME_BUFFER_VALID) {
        // A repeat/null frame is NOT proof of visible game output.
        return;
    }
    s->got_video = true;
    if (!s->presenter.present(width, height, &s->error))
        stop_requested.store(true);
    else
        frame_count.fetch_add(1);
}
void audio(int16_t, int16_t) {}
size_t audioBatch(const int16_t*, size_t frames) {
    // Audio device bridge not yet implemented. Count-only; not QA-ready audio.
    return frames;
}
void inputPoll() {}
int16_t input(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port || index || device != RETRO_DEVICE_JOYPAD || id >= 32) return 0;
    return (pressed_buttons.load() & (1u << id)) ? 1 : 0;
}
std::string jstringValue(JNIEnv* env, jstring str) {
    if (!str) return {};
    const char* value = env->GetStringUTFChars(str, nullptr);
    if (!value) return {};
    std::string output(value);
    env->ReleaseStringUTFChars(str, value);
    return output;
}
std::string run(ANativeWindow* window, const std::string& root,
                const std::string& disc, unsigned frames) {
    WiiuSession session;
    session.system = root + "/system";
    session.save = root + "/saves";
    session.disc = disc;
    stop_requested.store(false);
    frame_count.store(0);
    pressed_buttons.store(0);
    if (!window) return "WIIU_BLOCKED_NO_SURFACE";
    // The user must independently supply a lawful test image, keys and any
    // missing resources. Strict app-private staging; never arbitrary SAF access.
    try {
        const auto base = std::filesystem::canonical(root);
        const auto source = std::filesystem::canonical(disc);
        if (!std::filesystem::is_regular_file(source) || source.string().find(base.string() + "/") != 0)
            return "WIIU_BLOCKED_PRIVATE_FIXTURE";
        std::filesystem::create_directories(session.system);
        std::filesystem::create_directories(session.save);
    } catch (const std::exception&) {
        return "WIIU_BLOCKED_PRIVATE_FIXTURE";
    }
    if (!session.core.open()) return "WIIU_BLOCKED_NATIVE_CORE_OR_DEPENDENCIES";
    active = &session;
    session.core.set_environment(environment);
    session.core.set_video(video);
    session.core.set_audio(audio);
    session.core.set_audio_batch(audioBatch);
    session.core.set_input_poll(inputPoll);
    session.core.set_input_state(input);
    session.core.init();
    session.initialized = true;
    retro_game_info game{};
    game.path = session.disc.c_str();
    session.loaded = session.core.load_game(&game);
    if (!session.loaded) session.error = "WIIU_CORE_LOAD_FAILED";
    if (session.loaded && (!session.got_hw || !session.negotiation))
        session.error = "WIIU_VULKAN_RENDER_REQUEST_MISSING";
    if (session.error.empty() &&
        !session.presenter.initialize(window, session.negotiation, &session.error))
        session.error = "WIIU_VULKAN_INIT_" + session.error;
    if (session.error.empty() && session.hw.context_reset)
        session.hw.context_reset();
    if (session.error.empty()) {
        for (unsigned i = 0; i < frames && !stop_requested.load(); ++i) {
            if (!session.presenter.acquire(&session.error)) break;
            session.core.run();
            if (!session.error.empty()) break;
        }
    }
    if (session.loaded) session.core.unload_game();
    if (session.got_hw && session.hw.context_destroy) session.hw.context_destroy();
    session.presenter.stop();
    if (session.initialized) session.core.deinit();
    // Never treat a completed run loop as gameplay proof.
    session.core.close();
    active = nullptr;
    const unsigned presented = frame_count.load();
    if (!session.error.empty()) return session.error +
        " PRESENTED_GAME_IMAGE_CALLBACKS=" + std::to_string(presented);
    if (!presented) return "WIIU_NO_GAME_FRAMES";
    return "WIIU_VULKAN_FRAME_CALLBACKS=" + std::to_string(presented) +
        " (unverified game content/audio/input/save)";
}
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeRun(
    JNIEnv* env, jobject, jobject surface, jstring root, jstring disc, jint maxFrames) {
    if (maxFrames <= 0 || maxFrames > 36000)
        return env->NewStringUTF("WIIU_BLOCKED_UNBOUNDED_FRAME_REQUEST");
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    std::string outcome = run(window, jstringValue(env, root),
                              jstringValue(env, disc),
                              static_cast<unsigned>(maxFrames));
    if (window) ANativeWindow_release(window);
    return env->NewStringUTF(outcome.c_str());
}
extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeStop(
    JNIEnv*, jobject) {
    stop_requested.store(true);
}
extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeButton(
    JNIEnv*, jobject, jint id, jboolean pressed) {
    if (id < 0 || id >= 32) return;
    const uint32_t bit = 1u << static_cast<unsigned>(id);
    if (pressed) pressed_buttons.fetch_or(bit);
    else pressed_buttons.fetch_and(~bit);
}
