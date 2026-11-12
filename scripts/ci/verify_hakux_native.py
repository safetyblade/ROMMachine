#!/usr/bin/env python3
"""Check actual SDL/Vulkan Android backend ABI, ELF alignment and dependencies."""
import argparse
import json
import pathlib
import re
import struct
import subprocess

SYSTEM = {'libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libandroid.so',
          'libvulkan.so', 'libEGL.so', 'libGLESv1_CM.so', 'libGLESv2.so', 'libGLESv3.so',
          'libOpenSLES.so', 'libz.so', 'libjnigraphics.so', 'libaaudio.so'}


def verify(directory, readelf='readelf'):
    files = {p.name: p for p in directory.glob('*.so')}
    if not {'libhakuxcore.so', 'libSDL3.so'} <= files.keys():
        raise ValueError('Missing actual hakuX/SDL3 runtime')
    result = {}
    for name, path in files.items():
        data = path.read_bytes()
        if len(data) < 64 or data[:6] != b'\x7fELF\x02\x01' or struct.unpack_from('<HH', data, 16) != (3, 183):
            raise ValueError(name + ': expected Android ARM64 shared ELF')
        offset = struct.unpack_from('<Q', data, 32)[0]
        size, count = struct.unpack_from('<HH', data, 54)
        if size < 56 or offset + size * count > len(data):
            raise ValueError(name + ': invalid ELF headers')
        loads = [struct.unpack_from('<Q', data, offset + i * size + 48)[0]
                 for i in range(count) if struct.unpack_from('<I', data, offset + i * size)[0] == 1]
        if not loads or min(loads) < 16384:
            raise ValueError(name + ': ELF lacks 16KB page alignment')
        dynamic = subprocess.check_output([readelf, '-dW', str(path)], text=True)
        needed = set(re.findall(r'\(NEEDED\).*\[(.*?)\]', dynamic))
        if needed - SYSTEM - files.keys():
            raise ValueError(name + ': unbundled dependencies ' + str(sorted(needed - SYSTEM - files.keys())))
        symbols = subprocess.check_output([readelf, '--dyn-syms', '-W', str(path)], text=True)
        exported = {line.split()[-1].split('@', 1)[0] for line in symbols.splitlines() if ' GLOBAL ' in line and ' UND ' not in line}
        required = {'SDL_main', 'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPad',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetOverlayPad',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPaused',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetFatal',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetFatalKind',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetFrameShown',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetBootState',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetGpuReport',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetRelaunch',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeGetQuit',
                    'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPads'} if name == 'libhakuxcore.so' else set()
        if name == 'libSDL3.so':
            required = {'JNI_OnLoad'}
        if required - exported:
            raise ValueError(name + ': missing native host ABI ' + str(sorted(required - exported)))
        result[name] = {'bytes': len(data), 'needed': sorted(needed), 'arm64_16kb': True}
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('directory', type=pathlib.Path)
    parser.add_argument('--readelf', default='readelf')
    args = parser.parse_args()
    print(json.dumps(verify(args.directory, args.readelf), indent=2))
