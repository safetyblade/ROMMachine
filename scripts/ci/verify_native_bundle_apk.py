#!/usr/bin/env python3
"""Check a built engineering APK against its verified private bundle."""
import argparse
import hashlib
import json
import pathlib
import struct
import zipfile


def elf_16k(data):
    if len(data) < 64 or data[:6] != b'\x7fELF\x02\x01':
        return False
    offset = struct.unpack_from('<Q', data, 32)[0]
    stride, count = struct.unpack_from('<HH', data, 54)
    if stride < 56 or offset + stride * count > len(data):
        return False
    loads = [struct.unpack_from('<Q', data, offset + i * stride + 48)[0]
             for i in range(count)
             if struct.unpack_from('<I', data, offset + i * stride)[0] == 1]
    return bool(loads) and min(loads) >= 16384


def verify(apk, bundle):
    expected = (bundle / 'bundle-sha256.txt').read_text().splitlines()
    result = {'bundle_files_verified': 0, 'native_elf_16k_failures': [],
              'native_library_count': 0, 'stored_native_count': 0}
    with zipfile.ZipFile(apk) as archive, apk.open('rb') as raw:
        for line in expected:
            digest, relative = line.split('  ', 1)
            member = relative.replace('jniLibs/', 'lib/', 1) if relative.startswith('jniLibs/') else relative
            data = archive.read(member)
            if hashlib.sha256(data).hexdigest() != digest:
                raise ValueError(f'APK changed verified bundle bytes: {member}')
            result['bundle_files_verified'] += 1
        for item in archive.infolist():
            if not item.filename.startswith('lib/') or not item.filename.endswith('.so'):
                continue
            if not item.filename.startswith('lib/arm64-v8a/'):
                raise ValueError('Non-ARM64 library: ' + item.filename)
            result['native_library_count'] += 1
            if not elf_16k(archive.read(item)):
                result['native_elf_16k_failures'].append(item.filename)
            if item.compress_type == zipfile.ZIP_STORED:
                raw.seek(item.header_offset)
                header = raw.read(30)
                name_size, extra_size = struct.unpack_from('<HH', header, 26)
                data_offset = item.header_offset + 30 + name_size + extra_size
                if data_offset % 16384:
                    raise ValueError('Uncompressed native entry lacks 16KB ZIP alignment: ' + item.filename)
                result['stored_native_count'] += 1
    # Report inherited alignment separately; no compile/device/runtime claims are inferred.
    result['all_native_elf_16k'] = not result['native_elf_16k_failures']
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=pathlib.Path)
    parser.add_argument('bundle', type=pathlib.Path)
    args = parser.parse_args()
    print(json.dumps(verify(args.apk, args.bundle), indent=2))
