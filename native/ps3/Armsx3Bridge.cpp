#include <jni.h>
#include <dlfcn.h>
#include <string>
#include <string_view>
#include <stdexcept>
#include <mutex>

// The pinned engine owns Vulkan, Oboe and Cell execution. This bridge never synthesizes frames or boot success.
namespace {
void* library = nullptr;
std::mutex loadMutex;
std::string text(JNIEnv* env, jstring value) {
    if (!value) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) throw std::runtime_error("Invalid native string");
    std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}
template<class T> T symbol(const char* name) {
    if (!library) throw std::runtime_error("ARMSX3 core is not loaded");
    auto result = reinterpret_cast<T>(dlsym(library, name));
    if (!result) throw std::runtime_error(std::string("Missing pinned ARMSX3 ABI: ") + name);
    return result;
}
void callback(JNIEnv* env, const char* name, const char* method, const char* signature, bool isStatic) {
    jclass cls = env->FindClass(name);
    if (!cls || env->ExceptionCheck()) {
        env->ExceptionClear();
        throw std::runtime_error(std::string("PS3 callback class missing: ") + name);
    }
    jmethodID id = isStatic ? env->GetStaticMethodID(cls, method, signature) : env->GetMethodID(cls, method, signature);
    env->DeleteLocalRef(cls);
    if (!id || env->ExceptionCheck()) {
        env->ExceptionClear();
        throw std::runtime_error(std::string("PS3 callback ABI mismatch: ") + name + "." + method);
    }
}
void failure(JNIEnv* env, const std::exception& error) {
    if (!env->ExceptionCheck()) env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
}
}
#define JNI_NAME(name) Java_com_swordfish_lemuroid_app_debug_ps3_Ps3Native_##name
extern "C" JNIEXPORT jboolean JNICALL JNI_NAME(load)(JNIEnv* e, jobject, jstring path) {
    try {
        std::lock_guard lock(loadMutex);
        if (library) return true;
        callback(e, "com/armsx3/core/ProgressRepository", "onProgressEvent", "(JJJLjava/lang/String;)Z", true);
        callback(e, "com/armsx3/core/FirmwareRepository", "onFirmwareInstalled", "(Ljava/lang/String;)V", true);
        callback(e, "com/armsx3/core/FirmwareRepository", "onFirmwareCompiled", "(Ljava/lang/String;)V", true);
        callback(e, "com/armsx3/core/GameRepository", "add", "([Lcom/armsx3/core/GameInfo;J)V", true);
        callback(e, "com/armsx3/core/GameInfo", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V", false);
        library = dlopen(text(e, path).c_str(), RTLD_NOW | RTLD_LOCAL | RTLD_NODELETE);
        if (!library) throw std::runtime_error("ARMSX3 core or one of its native dependencies could not load");
        for (const char* name : {"_armsx3_initialize", "_armsx3_boot", "_armsx3_getState", "_armsx3_surfaceEvent", "_armsx3_overlayPadData", "_armsx3_installFw", "_armsx3_shutdown", "_armsx3_startMainThreadProcessor", "_armsx3_processCompilationQueue"}) symbol<void*>(name);
        return true;
    } catch (const std::exception& x) { failure(e,x); return false; }
}
extern "C" JNIEXPORT jboolean JNICALL JNI_NAME(initialize)(JNIEnv* e, jobject, jstring root, jstring user) {
    try { return symbol<bool(*)(std::string_view,std::string_view)>("_armsx3_initialize")(text(e,root),text(e,user)); } catch (const std::exception& x) { failure(e,x); return false; }
}
extern "C" JNIEXPORT jint JNICALL JNI_NAME(boot)(JNIEnv* e, jobject, jstring path) {
    try { return symbol<int(*)(std::string_view)>("_armsx3_boot")(text(e,path)); } catch (const std::exception& x) { failure(e,x); return -1; }
}
extern "C" JNIEXPORT jint JNICALL JNI_NAME(state)(JNIEnv* e,jobject) {
    try { return symbol<int(*)()>("_armsx3_getState")(); } catch(const std::exception& x) { failure(e,x); return -1; }
}
extern "C" JNIEXPORT void JNICALL JNI_NAME(process)(JNIEnv* e,jobject,jboolean compilation) {
    try { symbol<bool(*)(JNIEnv*)>(compilation ? "_armsx3_processCompilationQueue" : "_armsx3_startMainThreadProcessor")(e); } catch(const std::exception& x) { failure(e,x); }
}
extern "C" JNIEXPORT jboolean JNICALL JNI_NAME(surface)(JNIEnv* e,jobject,jobject surface,jint event,jint width,jint height) {
    try {
        if (width>0 && height>0) symbol<void(*)(int,int)>("_armsx3_surfaceSizeChanged")(width,height);
        return symbol<bool(*)(JNIEnv*,jobject,jint)>("_armsx3_surfaceEvent")(e,surface,event);
    } catch(const std::exception& x) { failure(e,x); return false; }
}
extern "C" JNIEXPORT void JNICALL JNI_NAME(pause)(JNIEnv* e,jobject,jboolean paused) {
    try { symbol<void(*)()>(paused ? "_armsx3_pause" : "_armsx3_resume")(); } catch(const std::exception& x) { failure(e,x); }
}
extern "C" JNIEXPORT void JNICALL JNI_NAME(stop)(JNIEnv* e,jobject) {
    try { symbol<void(*)()>("_armsx3_shutdown")(); } catch(const std::exception& x) { failure(e,x); }
}
extern "C" JNIEXPORT jboolean JNICALL JNI_NAME(install)(JNIEnv* e,jobject,jint fd,jboolean firmware) {
    try { return symbol<bool(*)(JNIEnv*,int,long)>(firmware ? "_armsx3_installFw" : "_armsx3_install")(e,fd,1); } catch(const std::exception& x) { failure(e,x); return false; }
}
extern "C" JNIEXPORT void JNICALL JNI_NAME(pad)(JNIEnv* e,jobject,jint port,jint d1,jint d2,jint lx,jint ly,jint rx,jint ry) {
    try { symbol<bool(*)(int,int,int,int,int,int,int)>("_armsx3_overlayPadData")(port,d1,d2,lx,ly,rx,ry); } catch(const std::exception& x) { failure(e,x); }
}
extern "C" JNIEXPORT jlong JNICALL JNI_NAME(framePeriod)(JNIEnv* e,jobject) {
    try { return static_cast<jlong>(symbol<unsigned long long(*)()>("_armsx3_getFramePeriodNs")()); }
    catch(const std::exception& x) { failure(e,x); return 0; }
}
