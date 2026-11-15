#include <mutex>
static int* destroyed = nullptr;
static std::mutex mutex;
struct Lifetime { ~Lifetime() { if (destroyed) *destroyed = 1; } } lifetime;
extern "C" void setMarker(int* value) { destroyed = value; }
extern "C" int lockAfterRelease() { std::lock_guard<std::mutex> lock(mutex); return 42; }
