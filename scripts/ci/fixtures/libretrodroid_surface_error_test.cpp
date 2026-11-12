#include <cassert>
#include <stdexcept>
#include "utils/libretrodroidexception.h"
#include "errorcodes.h"

#define JNIEXPORT
#define JNICALL
#define LOGE(...) ((void) 0)
using jclass = void*;
struct JNIEnv { int exceptionCode = -1; };

// Replace the graphics driver and Java VM, but compile the actual patched JNI
// function unchanged. Native exception construction and error codes are real.
namespace libretrodroid {
class LibretroDroid {
public:
    int failure = 0;
    static LibretroDroid& getInstance() { static LibretroDroid instance; return instance; }
    void onSurfaceCreated() {
        if (failure == 1) throw LibretroDroidError("Missing GLES context", ERROR_GL_NOT_COMPATIBLE);
        if (failure == 2) throw std::runtime_error("Driver initialization failed");
        if (failure == 3) throw 17;
    }
};
namespace JavaUtils {
void throwRetroException(JNIEnv* env, int code) { env->exceptionCode = code; }
}

#include "jni_surface_boundary.inc"
}

int main() {
    using namespace libretrodroid;
    JNIEnv env;
    LibretroDroid::getInstance().failure = 0;
    Java_com_swordfish_libretrodroid_LibretroDroid_onSurfaceCreated(&env, nullptr);
    assert(env.exceptionCode == -1);
    LibretroDroid::getInstance().failure = 1;
    Java_com_swordfish_libretrodroid_LibretroDroid_onSurfaceCreated(&env, nullptr);
    assert(env.exceptionCode == ERROR_GL_NOT_COMPATIBLE);
    LibretroDroid::getInstance().failure = 2;
    Java_com_swordfish_libretrodroid_LibretroDroid_onSurfaceCreated(&env, nullptr);
    assert(env.exceptionCode == ERROR_GENERIC);
    LibretroDroid::getInstance().failure = 3;
    Java_com_swordfish_libretrodroid_LibretroDroid_onSurfaceCreated(&env, nullptr);
    assert(env.exceptionCode == ERROR_GENERIC);
}
