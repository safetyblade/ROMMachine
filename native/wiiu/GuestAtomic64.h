#pragma once
#include <cstdint>
#include <cstring>
#include <mutex>

// Wii U pointers can be four-byte aligned. AArch64 CAS/exclusive instructions
// require eight-byte alignment even where ordinary loads accept unaligned data.
// Serialize ALL coreinit 64-bit operations, not only CAS or unaligned accesses.
namespace CartdeckGuestAtomic64 {
inline std::mutex mutex;
inline uint64_t load(const void* address) {
    unsigned char bytes[8];
    std::memcpy(bytes, address, sizeof(bytes));
    uint64_t value = 0;
    for (auto byte : bytes) value = (value << 8) | byte;
    return value;
}
inline void store(void* address, uint64_t value) {
    unsigned char bytes[8];
    for (unsigned i = 0; i < 8; ++i) bytes[7 - i] = static_cast<unsigned char>(value >> (i * 8));
    std::memcpy(address, bytes, sizeof(bytes));
}
template<class Operation> uint64_t update(void* address, Operation operation) {
    std::lock_guard<std::mutex> lock(mutex);
    const uint64_t previous = load(address);
    store(address, operation(previous));
    return previous;
}
inline uint64_t get(void* address) {
    std::lock_guard<std::mutex> lock(mutex);
    return load(address);
}
inline bool compareExchange(void* address, uint64_t expected, uint64_t desired, void* previous = nullptr) {
    std::lock_guard<std::mutex> lock(mutex);
    const uint64_t actual = load(address);
    const bool matched = actual == expected;
    if (matched) store(address, desired);
    if (previous) store(previous, actual);
    return matched;
}
}
