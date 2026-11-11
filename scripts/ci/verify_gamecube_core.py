#!/usr/bin/env python3
"""Reject non-Android/unaligned/incomplete Dolphin libraries before staging."""
import pathlib
import struct
import subprocess
import sys


def verify(data, dynamic, symbols):
    if data[:6] != b'\x7fELF\x02\x01' or len(data) < 64:
        raise ValueError('Expected little-endian ELF64')
    if struct.unpack_from('<H', data, 16)[0] != 3:
        raise ValueError('Expected shared ELF library (ET_DYN)')
    if struct.unpack_from('<H', data, 18)[0] != 183:
        raise ValueError('Expected AArch64')
    phoff = struct.unpack_from('<Q', data, 32)[0]
    size, count = struct.unpack_from('<HH', data, 54)
    if size < 56 or phoff + size * count > len(data):
        raise ValueError('Invalid ELF program headers')
    loads = [struct.unpack_from('<Q', data, phoff + i * size + 48)[0]
             for i in range(count)
             if struct.unpack_from('<I', data, phoff + i * size)[0] == 1]
    if not loads or any(align < 16384 for align in loads):
        raise ValueError('16 KiB PT_LOAD alignment required')
    if 'libc.so.6' in dynamic or 'libstdc++.so.6' in dynamic or 'ld-linux' in dynamic:
        raise ValueError('Linux host dependencies cannot ship on Android')
    required = {'retro_init', 'retro_deinit', 'retro_load_game', 'retro_run',
                'retro_set_environment', 'retro_get_system_av_info',
                'retro_serialize', 'retro_unserialize'}
    exported = {line.split()[-1] for line in symbols.splitlines()
                if ' GLOBAL ' in line and ' UND ' not in line and line.split()}
    if required - exported:
        raise ValueError(f'Missing exported Libretro ABI: {sorted(required - exported)}')


if __name__ == '__main__':
    core, readelf = sys.argv[1:]
    dynamic = subprocess.check_output([readelf, '-dW', core], text=True)
    symbols = subprocess.check_output([readelf, '--dyn-syms', '-W', core], text=True)
    verify(pathlib.Path(core).read_bytes(), dynamic, symbols)
    print('Android ARM64, exported Libretro ABI and 16 KiB PT_LOAD: PASS')
