#include "ProcessCoreLibrary.h"
#include <cassert>
int main(int argc, char** argv) {
    assert(argc == 2);
    static int destroyed = 0;
    auto* core = openProcessCore(argv[1]);
    assert(core);
    auto marker = reinterpret_cast<void (*)(int*)>(dlsym(core, "setMarker"));
    auto lock = reinterpret_cast<int (*)()>(dlsym(core, "lockAfterRelease"));
    assert(marker && lock);
    marker(&destroyed);
    assert(dlclose(core) == 0);
    assert(destroyed == 0); // no premature C++ static destructor
    assert(lock() == 42);  // mutex and its callback code still mapped
}
