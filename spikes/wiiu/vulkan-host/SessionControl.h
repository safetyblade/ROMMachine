#pragma once
#include <condition_variable>
#include <chrono>
#include <mutex>
#include <string>

// Only the VM observes/acknowledges commands at completed frame boundaries.
// UI requests never destroy graphics, mutate the core or wait on the VM.
class SessionControl {
public:
    void prepare() {
        std::lock_guard<std::mutex> lock(mutex_);
        paused_ = stopped_ = terminal_ = acknowledged_pause_ = false;
        surface_ = true;
        request_ = acknowledged_ = surface_generation_ = 0;
        state_ = "starting";
    }
    void pause(bool value) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (stopped_ || terminal_ || paused_ == value) return;
        paused_ = value;
        ++request_;
        changed_.notify_all();
    }
    void surface(bool available) {
        std::lock_guard<std::mutex> lock(mutex_);
        if (stopped_ || terminal_) return;
        surface_ = available;
        ++surface_generation_;
        changed_.notify_all();
    }
    void stop() {
        std::lock_guard<std::mutex> lock(mutex_);
        if (stopped_ || terminal_) return;
        stopped_ = true;
        ++request_;
        changed_.notify_all();
    }
    bool boundary() {
        std::unique_lock<std::mutex> lock(mutex_);
        if (terminal_ || stopped_) { state_ = "stopping"; return false; }
        acknowledged_ = request_;
        acknowledged_pause_ = paused_ || !surface_;
        state_ = paused_ ? "paused" : (!surface_ ? "surface_wait" : "running");
        if (!acknowledged_pause_) return true;
        const auto generation = surface_generation_;
        const auto request = request_;
        // Timer services audio reconnection while parked. Commands wake immediately.
        changed_.wait_for(lock, std::chrono::milliseconds(100), [&] {
            return stopped_ || request_ != request || surface_generation_ != generation;
        });
        return false; // re-observe commands and service audio before guest execution
    }
    void finish() {
        std::lock_guard<std::mutex> lock(mutex_);
        terminal_ = true;
        state_ = "stopped";
        changed_.notify_all();
    }
    std::string report() const {
        std::lock_guard<std::mutex> lock(mutex_);
        return "\nsession_state=" + std::string(state_) +
            "\npause_requested=" + std::to_string(paused_) +
            "\npause_acknowledged=" + std::to_string(acknowledged_pause_) +
            "\nlifecycle_request=" + std::to_string(request_) +
            "\nlifecycle_ack=" + std::to_string(acknowledged_) +
            "\nsurface_generation=" + std::to_string(surface_generation_) +
            "\nsurface_available=" + std::to_string(surface_) +
            "\nstop_requested=" + std::to_string(stopped_);
    }
private:
    mutable std::mutex mutex_;
    std::condition_variable changed_;
    bool paused_ = false, stopped_ = false, terminal_ = false, surface_ = true;
    bool acknowledged_pause_ = false;
    unsigned request_ = 0, acknowledged_ = 0, surface_generation_ = 0;
    const char* state_ = "unprepared";
};
