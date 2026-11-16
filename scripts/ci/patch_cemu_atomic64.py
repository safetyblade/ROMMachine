#!/usr/bin/env python3
"""Pinned Cemu coreinit guest atomic64 repair; host instructions must not require guest alignment."""
import hashlib
import pathlib
import subprocess

PIN = '444c585202302a7f4b319bafb4cf72156a8cac8b'
PATH = 'src/Cafe/OS/libs/coreinit/coreinit_Atomic.cpp'

def patch(root):
    root = pathlib.Path(root)
    assert subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip() == PIN
    original = subprocess.check_output(['git', '-C', str(root), 'show', f'{PIN}:{PATH}'])
    target = root / PATH
    assert target.read_bytes() == original, 'Atomic source drift: refuse stale or duplicate patch'
    text = original.decode()
    start = text.index('\t/* 64bit atomic operations */')
    end = text.index('\n\tvoid InitializeAtomic()', start)
    operations = '''\t/* Guest 64-bit operations use an alignment-safe, shared serialization domain. */
    uint64 OSSwapAtomic64(std::atomic<uint64be>* mem, uint64 value) {
        return CartdeckGuestAtomic64::update(mem, [=](uint64) { return value; });
    }
    uint64 OSSetAtomic64(std::atomic<uint64be>* mem, uint64 value) { return OSSwapAtomic64(mem, value); }
    uint64 OSGetAtomic64(std::atomic<uint64be>* mem) { return CartdeckGuestAtomic64::get(mem); }
    uint64 OSAddAtomic64(std::atomic<uint64be>* mem, uint64 value) {
        return CartdeckGuestAtomic64::update(mem, [=](uint64 old) { return old + value; });
    }
    uint64 OSAndAtomic64(std::atomic<uint64be>* mem, uint64 value) {
        return CartdeckGuestAtomic64::update(mem, [=](uint64 old) { return old & value; });
    }
    uint64 OSOrAtomic64(std::atomic<uint64be>* mem, uint64 value) {
        return CartdeckGuestAtomic64::update(mem, [=](uint64 old) { return old | value; });
    }
    bool OSCompareAndSwapAtomic64(std::atomic<uint64be>* mem, uint64 expected, uint64 desired) {
        return CartdeckGuestAtomic64::compareExchange(mem, expected, desired);
    }
    bool OSCompareAndSwapAtomicEx64(std::atomic<uint64be>* mem, uint64 expected, uint64 desired, uint64be* previous) {
        return CartdeckGuestAtomic64::compareExchange(mem, expected, desired, previous);
    }
'''
    text = text[:start] + operations + text[end:]
    text = text.replace('#include "coreinit_Atomic.h"', '#include "coreinit_Atomic.h"\n#include "CartdeckGuestAtomic64.h"')
    target.write_text(text)
    header = pathlib.Path(__file__).resolve().parents[2] / 'native/wiiu/GuestAtomic64.h'
    (target.parent / 'CartdeckGuestAtomic64.h').write_bytes(header.read_bytes())
    changelog = root / 'CHANGELOG.md'
    changelog.write_text(changelog.read_text().replace('\n', '\n\n## Unreleased CARTDECK adapter\n\n- Avoid ARM64 alignment faults in guest coreinit 64-bit atomics, including Bayonetta 2 compare-and-swap. All 64-bit operations share serialization and preserve big-endian guest values.\n', 1))
    return {'path': PATH, 'before_sha256': hashlib.sha256(original).hexdigest(),
            'after_sha256': hashlib.sha256(target.read_bytes()).hexdigest(),
            'helper_sha256': hashlib.sha256(header.read_bytes()).hexdigest(),
            'purpose': 'Guest alignment-safe serialized coreinit atomic64; exact Bayonetta 2 SIGBUS trace'}
