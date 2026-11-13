#!/usr/bin/env python3
"""Check a built engineering APK against its verified private bundle."""
import argparse
import hashlib
import io
import json
import pathlib
import struct
import zipfile


def elf_16k(data):
    if len(data) < 64 or data[:6] != b'\x7fELF\x02\x01':
        return False
    if struct.unpack_from('<H', data, 18)[0] != 183:
        return False
    offset = struct.unpack_from('<Q', data, 32)[0]
    stride, count = struct.unpack_from('<HH', data, 54)
    if stride < 56 or offset + stride * count > len(data):
        return False
    loads = [struct.unpack_from('<Q', data, offset + i * stride + 48)[0]
             for i in range(count)
             if struct.unpack_from('<I', data, offset + i * stride)[0] == 1]
    return bool(loads) and min(loads) >= 16384


ENGINE_LIBRARIES = {
    'libpcsx_rearmed_modern_libretro_android.so',
    'libppsspp_modern_libretro_android.so',
    'libflycast_libretro_android.so',
    'libdolphin_modern_libretro_android.so',
    'libplay_modern_libretro_android.so',
}
LEGACY_LIBRARIES = {'libpcsx_rearmed_libretro_android.so', 'libppsspp_libretro_android.so'}


def verify(apk, bundle, require_five_engines=False, host_aar=None, require_wii=False, require_xbox=False, require_xbox360=False):
    require_five_engines = require_five_engines or require_wii or require_xbox or require_xbox360
    expected = (bundle / 'bundle-sha256.txt').read_text().splitlines()
    result = {'bundle_files_verified': 0, 'native_elf_16k_failures': [],
              'native_library_count': 0, 'stored_native_count': 0}
    with zipfile.ZipFile(apk) as archive, apk.open('rb') as raw:
        libraries = {pathlib.PurePosixPath(n).name for n in archive.namelist() if n.startswith('lib/')}
        result['all_five_engines_bundled'] = ENGINE_LIBRARIES <= libraries
        if require_five_engines:
            missing = (ENGINE_LIBRARIES | LEGACY_LIBRARIES) - libraries
            if missing:
                raise ValueError('Required engines/fallbacks missing: ' + ', '.join(sorted(missing)))
            systems = (bundle / 'native-systems.txt').read_text().strip()
            required_systems = {'dc', 'gc', 'ps2'}
            if require_wii:
                required_systems.add('wii')
            if require_xbox:
                required_systems.add('xbox')
            if require_xbox360:
                required_systems.add('xbox360')
            if set(systems.split(',')) != required_systems:
                raise ValueError('Required library systems must be enabled: ' + ','.join(sorted(required_systems)))
            # The non-minified engineering APK must carry the actual compiled registry flag.
            enabled = systems.encode('utf-8') + b'\0'
            if not any(enabled in archive.read(n) for n in archive.namelist() if n.endswith('.dex')):
                raise ValueError('APK was built without the new-console registry enabled')
            if host_aar is None:
                raise ValueError('Patched host AAR is required for the five-engine APK')
        if require_wii:
            if 'libdolphin_modern_libretro_android.so' not in libraries:
                raise ValueError('Wii Dolphin binary is missing')
            result['wii_dolphin_bundled'] = True
        if require_xbox:
            if not {'libhakuxcore.so', 'libSDL3.so'} <= libraries:
                raise ValueError('Xbox hakuX/SDL native backend is missing')
            if 'assets/cartdeck/hakux.properties' not in archive.namelist():
                raise ValueError('Xbox provenance is missing')
            dex = [archive.read(n) for n in archive.namelist() if n.endswith('.dex')]
            for descriptor in (
                b'Lcom/rfandango/haku_x/NativeCoreActivity;',
                b'Lcom/swordfish/lemuroid/app/shared/game/xbox/XboxGameLauncher;',
                b'Lcom/swordfish/lemuroid/app/shared/game/xbox/XboxEngineStore;',
            ):
                if not any(descriptor in data for data in dex):
                    raise ValueError('Xbox internal host implementation is missing: ' + descriptor.decode())
            activity = 'com.rfandango.haku_x.NativeCoreActivity'
            manifest = archive.read('AndroidManifest.xml')
            if activity.encode() not in manifest and activity.encode('utf-16le') not in manifest:
                raise ValueError('Xbox internal activity is not registered')
            result['xbox_native_backend_bundled'] = True
        if require_xbox360:
            if 'libcartdeck_xenia.so' not in libraries:
                raise ValueError('Xbox 360 native engine is missing')
            for item in ('assets/cartdeck/xenia.properties',
                         'assets/cartdeck/xenia/xenia-licenses.zip',
                         'assets/cartdeck/xenia/source-manifest.json'):
                if item not in archive.namelist():
                    raise ValueError('Xbox 360 provenance/licenses missing: ' + item)
            with zipfile.ZipFile(io.BytesIO(archive.read('assets/cartdeck/xenia/xenia-licenses.zip'))) as notices:
                if not notices.namelist():
                    raise ValueError('Xbox 360 source notices empty')
            dex = [archive.read(n) for n in archive.namelist() if n.endswith('.dex')]
            for descriptor in (b'Lcom/swordfish/lemuroid/app/shared/game/xbox360/Xbox360Activity;',
                               b'Lcom/swordfish/lemuroid/app/shared/game/xbox360/Xbox360GameLauncher;'):
                if not any(descriptor in data for data in dex):
                    raise ValueError('Xbox 360 internal host missing: ' + descriptor.decode())
            activity = 'com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360Activity'
            manifest = archive.read('AndroidManifest.xml')
            if activity.encode() not in manifest and activity.encode('utf-16le') not in manifest:
                raise ValueError('Xbox 360 isolated Activity not registered')
            result['xbox360_native_backend_bundled'] = True
        if host_aar is not None:
            with zipfile.ZipFile(host_aar) as host:
                expected_host = host.read('jni/arm64-v8a/liblibretrodroid.so')
            if archive.read('lib/arm64-v8a/liblibretrodroid.so') != expected_host:
                raise ValueError('APK does not contain the verified patched graphics/input host')
            result['patched_host_matches'] = True
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
    if require_five_engines and not result['all_native_elf_16k']:
        raise ValueError('Five-engine APK contains non-ARM64 or non-16KB ELF libraries')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=pathlib.Path)
    parser.add_argument('bundle', type=pathlib.Path)
    parser.add_argument('--require-five-engines', action='store_true')
    parser.add_argument('--host-aar', type=pathlib.Path)
    parser.add_argument('--require-wii', action='store_true')
    parser.add_argument('--require-xbox', action='store_true')
    parser.add_argument('--require-xbox360', action='store_true')
    args = parser.parse_args()
    print(json.dumps(verify(args.apk, args.bundle, args.require_five_engines, args.host_aar,
                            args.require_wii, args.require_xbox, args.require_xbox360), indent=2))
