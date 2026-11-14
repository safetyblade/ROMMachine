// WIIU-02: embedded, private test-only Cemu Libretro frontend.
// This is a real Android native Libretro/Vulkan path, not an external APK.
// Gameplay, audio output and release readiness still require physical QA.
#include "CartdeckVulkanPresenter.h"
#include "WiiuAudio.h"
#include <android/native_window_jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <atomic>
#include <array>
#include <cmath>
#include <chrono>
#include <mutex>
#include <thread>
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
std::atomic<unsigned> image_width{0}, image_height{0};
std::atomic<uint64_t> submitted_audio{0};
std::atomic<bool> frontend_paused{false};
std::mutex surface_mutex;
ANativeWindow* pending_window = nullptr;
unsigned surface_generation = 0;

void clearPendingSurface() {
    std::lock_guard<std::mutex> lock(surface_mutex);
    if (pending_window) ANativeWindow_release(pending_window);
    pending_window = nullptr;
    surface_generation = 0;
}
std::atomic<unsigned> requested_controller{1};
std::atomic<unsigned> applied_controller{1};
std::atomic<unsigned> screen_requests{0};
std::atomic<bool> tab_down{false};
std::atomic<unsigned> host_stage{0};
std::mutex diagnostics_mutex;
std::string gpu_diagnostics;
std::array<std::atomic<int16_t>, 4> axes{};
std::atomic<int16_t> pointer_x{0}, pointer_y{0};
std::atomic<bool> pointer_down{false};

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
    decltype(&retro_get_system_av_info) get_av_info = nullptr;
    decltype(&retro_set_controller_port_device) set_controller = nullptr;

    template<typename F> bool loadSymbol(F& target, const char* name) {
        target = reinterpret_cast<F>(dlsym(library, name));
        return target != nullptr;
    }
    bool open(const std::string& path) {
        // Android only loads the SHA-checked core packaged beside this library.
        library = dlopen(path.c_str(), RTLD_NOW | RTLD_LOCAL);
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
               loadSymbol(run, "retro_run") &&
               loadSymbol(get_av_info, "retro_get_system_av_info") &&
               loadSymbol(set_controller, "retro_set_controller_port_device");
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
    WiiuAudio audio;
    bool got_hw = false;
    bool got_video = false;
    bool loaded = false;
    bool initialized = false;
    bool audio_started = false;
    std::atomic<bool> audio_failed{false};
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
            s->options["cemu_next_screen_layout_button"] = "Tab";
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
            return true;
        case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO: {
            if (!data) return false;
            const auto rate = static_cast<const retro_system_av_info*>(data)->timing.sample_rate;
            if (!std::isfinite(rate) || rate < 8000 || rate > 192000) return false;
            if (!s->audio_started) return true; // Initial rate is read after load_game.
            return s->audio.start(static_cast<int32_t>(rate), &s->error);
        }
        case RETRO_ENVIRONMENT_SET_KEYBOARD_CALLBACK:
            return false;
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
        // A blank/repeat frame is NOT proof of visible game output.
        if (!s->presenter.presentBlank(&s->error)) stop_requested.store(true);
        return;
    }
    s->got_video = true;
    image_width.store(width);
    image_height.store(height);
    const auto previous = s->presenter.presentedFrames();
    if (!s->presenter.present(width, height, &s->error))
        stop_requested.store(true);
    else
        frame_count.fetch_add(s->presenter.presentedFrames() - previous);
}
size_t audioBatch(const int16_t* data, size_t frames) {
    if (!active || stop_requested.load() || frontend_paused.load()) return 0;
    std::string error;
    auto consumed = active->audio.write(data, frames, &error);
    submitted_audio.fetch_add(consumed);
    if (!error.empty()) {
        __android_log_print(ANDROID_LOG_ERROR, "CARTDECK_WIIU_AUDIO", "%s", error.c_str());
        active->audio_failed.store(true);
        stop_requested.store(true);
    }
    return consumed;
}
void audio(int16_t left, int16_t right) {
    const int16_t stereo[] = {left, right};
    audioBatch(stereo, 1);
}
void inputPoll() {}
int16_t input(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port) return 0;
    if (device == RETRO_DEVICE_KEYBOARD && !index && id == RETROK_TAB) return tab_down.load();
    if (device == RETRO_DEVICE_ANALOG && index < 2 && id < 2)
        return axes[index * 2 + id].load();
    if (device == RETRO_DEVICE_POINTER && !index) {
        if (id == RETRO_DEVICE_ID_POINTER_X) return pointer_x.load();
        if (id == RETRO_DEVICE_ID_POINTER_Y) return pointer_y.load();
        if (id == RETRO_DEVICE_ID_POINTER_PRESSED) return pointer_down.load();
    }
    if (index || device != RETRO_DEVICE_JOYPAD || id >= 32) return 0;
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
                const std::string& disc, unsigned frames,
                const std::string& coreLibrary, const std::string& mediaDirectory) {
    WiiuSession session;
    session.system = root + "/system";
    session.save = root + "/saves";
    session.disc = disc;
    if (stop_requested.load()) return "WIIU_CANCELLED_BEFORE_START";
    frame_count.store(0);
    pressed_buttons.store(0);
    for (auto& axis : axes) axis.store(0);
    pointer_down.store(false);
    if (!window) return "WIIU_BLOCKED_NO_SURFACE";
    // The user must independently supply a lawful test image, keys and any
    // missing resources. Strict app-private staging; never arbitrary SAF access.
    try {
        const auto base = std::filesystem::canonical(mediaDirectory);
        const auto source = std::filesystem::canonical(disc);
        if (!std::filesystem::is_regular_file(source) || source.string().find(base.string() + "/") != 0)
            return "WIIU_BLOCKED_PRIVATE_FIXTURE";
        std::filesystem::create_directories(session.system);
        std::filesystem::create_directories(session.save);
    } catch (const std::exception&) {
        return "WIIU_BLOCKED_PRIVATE_FIXTURE";
    }
    if (!session.core.open(coreLibrary)) return "WIIU_BLOCKED_NATIVE_CORE_OR_DEPENDENCIES";
    active = &session;
    session.core.set_environment(environment);
    session.core.set_video(video);
    session.core.set_audio(audio);
    session.core.set_audio_batch(audioBatch);
    session.core.set_input_poll(inputPoll);
    session.core.set_input_state(input);
    session.core.init();
    session.initialized = true;
    host_stage.store(1);
    const auto initial_controller = requested_controller.load();
    session.core.set_controller(0, initial_controller);
    applied_controller.store(initial_controller);
    retro_game_info game{};
    game.path = session.disc.c_str();
    session.loaded = !stop_requested.load() && session.core.load_game(&game);
    if (session.loaded) host_stage.store(2);
    if (!session.loaded) session.error = "WIIU_CORE_LOAD_FAILED";
    if (session.loaded && (!session.got_hw || !session.negotiation))
        session.error = "WIIU_VULKAN_RENDER_REQUEST_MISSING";
    if (session.error.empty() && stop_requested.load()) session.error = "WIIU_CANCELLED";
    unsigned handled_surface_generation = 0;
    if (session.error.empty() &&
        !session.presenter.initialize(window, session.negotiation, &session.error))
        session.error = "WIIU_VULKAN_INIT_" + session.error;
    if (session.error.empty() && session.hw.context_reset)
        session.hw.context_reset();
    if (session.error.empty()) {
        host_stage.store(3);
        std::lock_guard<std::mutex> lock(diagnostics_mutex);
        gpu_diagnostics = session.presenter.deviceDescription();
    }
    if (session.error.empty()) {
        retro_system_av_info av{};
        session.core.get_av_info(&av);
        if (!std::isfinite(av.timing.sample_rate) || av.timing.sample_rate < 8000 ||
            av.timing.sample_rate > 192000)
            session.error = "WIIU_AUDIO_INVALID_SAMPLE_RATE";
        else
            session.audio_started = session.audio.start(
                static_cast<int32_t>(av.timing.sample_rate), &session.error);
    }
    if (session.error.empty()) {
        for (unsigned i = 0; (!frames || i < frames) && !stop_requested.load(); ++i) {
            {
                std::lock_guard<std::mutex> lock(surface_mutex);
                if (surface_generation != handled_surface_generation && pending_window) {
                    if (!session.presenter.replaceSurface(pending_window, &session.error)) break;
                    handled_surface_generation = surface_generation;
                }
            }
            if (frontend_paused.load()) {
                std::this_thread::sleep_for(std::chrono::milliseconds(10));
                continue;
            }
            const auto device = requested_controller.load();
            if (device != applied_controller.load()) {
                session.core.set_controller(0, device);
                applied_controller.store(device);
            }
            if (tab_down.load()) tab_down.store(false);
            else if (screen_requests.load()) {
                screen_requests.fetch_sub(1);
                tab_down.store(true);
            }
            if (!session.presenter.acquire(&session.error)) {
                if (session.error == "VULKAN_ACQUIRE_OUT_OF_DATE_RETRY") {
                    session.error.clear();
                    std::this_thread::sleep_for(std::chrono::milliseconds(10));
                    continue;
                }
                break;
            }
            session.core.run();
            // Cemu may still be booting without any video callback. Consume the
            // acquired swapchain image safely; don't deadlock on the next frame.
            if (session.presenter.hasAcquiredFrame() &&
                !session.presenter.presentBlank(&session.error)) break;
            if (!session.error.empty()) break;
        }
    }
    if (session.loaded) session.core.unload_game();
    session.audio.stop();
    if (session.got_hw && session.hw.context_destroy) session.hw.context_destroy();
    session.presenter.stop();
    if (session.initialized) session.core.deinit();
    // Never treat a completed run loop as gameplay proof.
    session.core.close();
    active = nullptr;
    host_stage.store(4);
    clearPendingSurface();
    if (session.error.empty() && session.audio_failed.load())
        session.error = "WIIU_AUDIO_WRITE_FAILED_SEE_NATIVE_LOG";
    const unsigned presented = frame_count.load();
    if (!session.error.empty()) return session.error +
        " PRESENTED_GAME_IMAGE_CALLBACKS=" + std::to_string(presented);
    if (!presented) return "WIIU_NO_GAME_FRAMES";
    return "WIIU_VULKAN_FRAME_CALLBACKS=" + std::to_string(presented) +
        " AUDIO_SUBMITTED_FRAMES=" + std::to_string(session.audio.submittedFrames()) +
        " (unverified game content/audio/input/save)";
}
}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeRun(
    JNIEnv* env, jobject, jobject surface, jstring root, jstring disc, jint maxFrames,
    jstring coreLibrary, jstring mediaDirectory) {
    if (maxFrames < 0 || maxFrames > 36000)
        return env->NewStringUTF("WIIU_BLOCKED_UNBOUNDED_FRAME_REQUEST");
    ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
    std::string outcome = run(window, jstringValue(env, root),
                              jstringValue(env, disc),
                              static_cast<unsigned>(maxFrames),
                              jstringValue(env, coreLibrary), jstringValue(env, mediaDirectory));
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

extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeAxis(
    JNIEnv*, jobject, jint axis, jint value) {
    if (axis >= 0 && axis < 4 && value >= -32767 && value <= 32767)
        axes[axis].store(static_cast<int16_t>(value));
}
extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativePointer(
    JNIEnv*, jobject, jint x, jint y, jboolean down) {
    if (x < -32767 || x > 32767 || y < -32767 || y > 32767) return;
    pointer_x.store(static_cast<int16_t>(x));
    pointer_y.store(static_cast<int16_t>(y));
    pointer_down.store(down);
}

extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativePrepare(
    JNIEnv*, jobject) {
    // UI prepares before starting the VM thread. A subsequent stop must never
    // be erased by a late-starting nativeRun after Surface destruction/pause.
    stop_requested.store(false);
    frontend_paused.store(false);
    clearPendingSurface();
    submitted_audio.store(0);
    host_stage.store(0);
    tab_down.store(false);
    screen_requests.store(0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativePause(
    JNIEnv*, jobject, jboolean paused) { frontend_paused.store(paused); }

extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeController(
    JNIEnv*, jobject, jboolean pro) {
    // Pinned Cemu RETRO_DEVICE_PRO = RETRO_DEVICE_SUBCLASS(JOYPAD, 1).
    requested_controller.store(pro ? 513u : RETRO_DEVICE_JOYPAD);
}

extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeToggleScreen(
    JNIEnv*, jobject) { screen_requests.fetch_add(1); }

extern "C" JNIEXPORT jstring JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeReport(
    JNIEnv* env, jobject) {
    std::lock_guard<std::mutex> lock(diagnostics_mutex);
    const auto out = "host_stage=" + std::to_string(host_stage.load()) +
        "\nimage_width=" + std::to_string(image_width.load()) +
        "\nimage_height=" + std::to_string(image_height.load()) +
        "\npresented_image_callbacks=" + std::to_string(frame_count.load()) +
        "\naudio_submitted_frames=" + std::to_string(submitted_audio.load()) +
        "\ncontroller_device_requested=" + std::to_string(applied_controller.load()) +
        "\n" + gpu_diagnostics + "\ngameplay=UNVERIFIED";
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_swordfish_lemuroid_app_debug_WiiuVulkanRuntimeProbeActivity_nativeSurface(
    JNIEnv* env, jobject, jobject surface) {
    ANativeWindow* next = surface ? ANativeWindow_fromSurface(env, surface) : nullptr;
    std::lock_guard<std::mutex> lock(surface_mutex);
    if (pending_window) ANativeWindow_release(pending_window);
    pending_window = next;
    ++surface_generation;
    if (!next) frontend_paused.store(true);
}
