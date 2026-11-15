#pragma once
#include <dlfcn.h>

// Cemu owns process-global and detached workers. In the one-title Android
// process, releasing a loader reference must not run its static destructors.
// Normal retro_unload_game/retro_deinit still stop the title and flush saves.
inline void* openProcessCore(const char* path) {
    return dlopen(path, RTLD_NOW | RTLD_LOCAL | RTLD_NODELETE);
}
