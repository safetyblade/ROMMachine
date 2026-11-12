#!/usr/bin/env python3
"""Stage verified local build outputs for one private CARTDECK test APK.

No network, upstream downloads, game media, BIOS, Git writes or publication.
"""
import argparse
import hashlib
import pathlib
import shutil
import subprocess
import re
import zipfile

from verify_gamecube_core import verify, verify_android_elf

ENGINES = {
    'ps1': ('libpcsx_rearmed_modern_libretro_android.so', 'pcsx-rearmed-modern.properties', None),
    'psp': ('libppsspp_modern_libretro_android.so', 'ppsspp-modern.properties', 'ppsspp-modern-assets.zip'),
    'dc': ('libflycast_libretro_android.so', 'flycast.properties', None),
    'gc': ('libdolphin_modern_libretro_android.so', 'dolphin-modern.properties', 'dolphin-modern-assets.zip'),
    'ps2': ('libplay_modern_libretro_android.so', 'play-modern.properties', None),
}


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def stage_xbox(source, readelf):
    """Validate a native SDL backend without pretending it implements Libretro."""
    props = dict(line.split('=', 1) for line in (source / 'hakux.properties').read_text().splitlines()
                 if line.strip() and not line.startswith('#'))
    if not re.fullmatch('[0-9a-f]{40}', props.get('revision', '')):
        raise ValueError('Xbox: missing exact source revision')
    if props.get('core.sha256') != sha(source / 'libhakuxcore.so'):
        raise ValueError('Xbox: core/manifest checksum mismatch')
    entries = []
    listed = set()
    for line in (source / 'sha256.txt').read_text().splitlines():
        digest, relative = line.split('  ', 1)
        path = pathlib.PurePosixPath(relative)
        if path.is_absolute() or '..' in path.parts or relative in listed:
            raise ValueError('Xbox: unsafe or duplicate package path')
        listed.add(relative)
        item = source / relative
        if sha(item) != digest:
            raise ValueError('Xbox: dependency checksum mismatch: ' + relative)
        if relative.endswith('.so'):
            if len(path.parts) != 1:
                raise ValueError('Xbox: native libraries must be flat')
            dynamic = subprocess.check_output([readelf, '-dW', str(item)], text=True)
            verify_android_elf(item.read_bytes(), dynamic)
            symbols = subprocess.check_output([readelf, '--dyn-syms', '-W', str(item)], text=True)
            exported = {line.split()[-1].split('@', 1)[0] for line in symbols.splitlines()
                        if ' GLOBAL ' in line and ' UND ' not in line and line.split()}
            required = set()
            if relative == 'libhakuxcore.so':
                required = {'SDL_main', 'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPads',
                            'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPaused'}
            elif relative == 'libSDL3.so':
                required = {'SDL_Init', 'SDL_Quit', 'SDL_CreateWindow'}
            if required - exported:
                raise ValueError('Xbox: missing native host ABI: ' + ', '.join(sorted(required - exported)))
            entries.append((item, 'jniLibs/arm64-v8a/' + relative))
        else:
            entries.append((item, 'assets/cartdeck/hakux/' + relative))
    if not {'libhakuxcore.so', 'libSDL3.so', 'hakux-licenses.zip'} <= listed:
        raise ValueError('Xbox: incomplete native/source-notice package')
    system = {'libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libandroid.so',
              'libvulkan.so', 'libEGL.so', 'libGLESv2.so', 'libGLESv3.so',
              'libOpenSLES.so', 'libGLESv1_CM.so', 'libz.so', 'libjnigraphics.so', 'libaaudio.so'}
    for item, relative in entries:
        if not relative.endswith('.so'):
            continue
        dynamic = subprocess.check_output([readelf, '-dW', str(item)], text=True)
        needed = set(re.findall(r'\(NEEDED\).*\[(.*?)\]', dynamic))
        if needed - system - listed:
            raise ValueError('Xbox: unbundled native dependencies: ' + ', '.join(sorted(needed - system - listed)))
    with zipfile.ZipFile(source / 'hakux-licenses.zip') as notices:
        if not notices.namelist():
            raise ValueError('Xbox: empty source notices')
    entries.append((source / 'hakux.properties', 'assets/cartdeck/hakux.properties'))
    return entries


def stage(sources, destination, readelf, wii=False, xbox=None):
    if destination.exists():
        raise ValueError('Use a fresh staging directory; never merge engine versions')
    # Validate every selected engine before creating the new bundle.
    validated = []
    if wii and 'gc' not in sources:
        raise ValueError('Wii requires the matching Dolphin engine and Sys resources')
    for name, source in sources.items():
        core_name, manifest_name, archive_name = ENGINES[name]
        core, manifest = source / core_name, source / manifest_name
        props = dict(line.split('=', 1) for line in manifest.read_text().splitlines()
                     if line.strip() and not line.startswith('#'))
        if props.get('core.sha256') != sha(core):
            raise ValueError(f'{name}: core/manifest checksum mismatch')
        revision = props.get('revision', props.get('upstream.revision', ''))
        if len(revision) != 40 or any(c not in '0123456789abcdef' for c in revision):
            raise ValueError(f'{name}: missing exact upstream revision')
        dynamic = subprocess.check_output([readelf, '-dW', str(core)], text=True)
        symbols = subprocess.check_output([readelf, '--dyn-syms', '-W', str(core)], text=True)
        verify(core.read_bytes(), dynamic, symbols)
        entries = [(core, 'jniLibs/arm64-v8a/' + core_name), (manifest, 'assets/cartdeck/' + manifest_name)]
        if archive_name:
            archive = source / archive_name
            if props.get('assets.sha256') != sha(archive):
                raise ValueError(f'{name}: asset/manifest checksum mismatch')
            entries.append((archive, 'assets/cartdeck/' + archive_name))
        validated.extend(entries)
    if xbox:
        validated.extend(stage_xbox(xbox, readelf))
    for source, relative in validated:
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    destination.mkdir(parents=True, exist_ok=True)
    systems = [k for k in sources if k in {'dc', 'gc', 'ps2'}]
    systems += ['wii'] if wii else []
    systems += ['xbox'] if xbox else []
    (destination / 'native-systems.txt').write_text(','.join(systems) + '\n')
    (destination / 'bundle-sha256.txt').write_text(''.join(
        f'{sha(destination / relative)}  {relative}\n' for _, relative in validated))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True, type=pathlib.Path)
    parser.add_argument('--readelf', default='readelf')
    parser.add_argument('--wii', action='store_true', help='Enable Wii with the existing Dolphin package')
    parser.add_argument('--xbox', type=pathlib.Path, help='Verified hakuX native Android package')
    for engine in ENGINES:
        parser.add_argument('--' + engine, type=pathlib.Path)
    args = parser.parse_args()
    sources = {name: getattr(args, name) for name in ENGINES if getattr(args, name)}
    if not sources:
        parser.error('Select at least one locally built engine')
    stage(sources, args.output, args.readelf, args.wii, args.xbox)
    print('Verified private bundle:', args.output)
    print('Gradle properties: -PcartdeckNativeBundleDir=' + str(args.output.resolve()) +
          ' -PcartdeckNativeSystems=' + (args.output / 'native-systems.txt').read_text().strip())
