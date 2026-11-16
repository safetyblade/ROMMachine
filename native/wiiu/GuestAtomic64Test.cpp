#include "GuestAtomic64.h"
#include <array>
#include <cassert>
#include <thread>
#include <vector>
using namespace CartdeckGuestAtomic64;
int main() {
    for (unsigned offset = 0; offset < 8; ++offset) {
        alignas(8) std::array<unsigned char, 32> data{};
        auto* guest = data.data() + offset;
        auto* previous = data.data() + 17; // Ex output may also be unaligned.
        store(guest, 0x0102030405060708ULL);
        assert(data[offset] == 1 && data[offset + 7] == 8);
        assert(compareExchange(guest, 0x0102030405060708ULL, 9, previous));
        assert(load(previous) == 0x0102030405060708ULL && get(guest) == 9);
        assert(!compareExchange(guest, 8, 99, previous));
        assert(load(previous) == 9 && get(guest) == 9);
        assert(update(guest, [](uint64_t) { return UINT64_MAX; }) == 9);
        assert(update(guest, [](uint64_t old) { return old + 1; }) == UINT64_MAX);
        assert(get(guest) == 0);
        std::vector<std::thread> workers;
        for (unsigned n = 0; n < 8; ++n) workers.emplace_back([guest] {
            for (unsigned i = 0; i < 10000; ++i) update(guest, [](uint64_t old) { return old + 1; });
        });
        for (auto& worker : workers) worker.join();
        assert(get(guest) == 80000);
    }
}
