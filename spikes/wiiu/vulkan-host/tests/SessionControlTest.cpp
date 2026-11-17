#include "SessionControl.h"
#include <cassert>
#include <future>
int main() {
    SessionControl state;
    state.prepare();
    assert(state.boundary());
    for (int i = 0; i < 100; ++i) {
        state.pause(true);
        auto parked = std::async(std::launch::async, [&] { return state.boundary(); });
        // No polling/delays needed: final acknowledgement is checked after join.
        state.pause(false);
        parked.get();
        assert(state.boundary());
    }
    state.pause(true);
    assert(!state.boundary());
    assert(state.report().find("pause_acknowledged=1") != std::string::npos);
    state.surface(false);
    state.pause(false);
    assert(!state.boundary()); // resume cannot override genuine surface loss
    state.surface(true);
    assert(state.boundary()); // surface restoration cannot leave a sticky menu pause
    state.stop();
    state.stop();
    state.pause(false);
    state.surface(true);
    assert(!state.boundary());
    state.finish();
    assert(state.report().find("session_state=stopped") != std::string::npos);
    state.pause(true);
    assert(!state.boundary()); // terminal session cannot restart
}
