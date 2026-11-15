#pragma once
#include <memory>
#include <mutex>

// UI transfers one retained window reference. The VM takes an independent
// snapshot, so rebuilding a surface never holds the UI's update lock.
template<class Window> class SurfaceMailbox {
public:
    using Ref = void (*)(Window*);
    using Handle = std::unique_ptr<Window, Ref>;
    struct Snapshot { Handle window; unsigned generation; };
    SurfaceMailbox(Ref retain, Ref release) : retain_(retain), release_(release), pending_(nullptr, release) {}
    void update(Window* window) {
        std::lock_guard<std::mutex> lock(mutex_);
        pending_.reset(window);
        ++generation_;
    }
    Snapshot snapshot(unsigned handled) {
        std::lock_guard<std::mutex> lock(mutex_);
        auto* window = generation_ != handled ? pending_.get() : nullptr;
        if (window) retain_(window);
        return {Handle(window, release_), generation_};
    }
    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        pending_.reset();
        generation_ = 0;
    }
private:
    Ref retain_, release_;
    std::mutex mutex_;
    Handle pending_;
    unsigned generation_ = 0;
};
