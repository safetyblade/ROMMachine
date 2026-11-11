#!/usr/bin/env python3
"""Stage verified local build outputs for one private CARTDECK test APK.

No network, upstream downloads, game media, BIOS, Git writes or publication.
"""
import argparse
import hashlib
import pathlib
import shutil
import subprocess

from verify_gamecube_core import verify

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


def stage(sources, destination, readelf):
    if destination.exists():
        raise ValueError('Use a fresh staging directory; never merge engine versions')
    # Validate every selected engine before creating the new bundle.
    validated = []
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
    for source, relative in validated:
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'native-systems.txt').write_text(','.join(k for k in sources if k in {'dc', 'gc', 'ps2'}) + '\n')
    (destination / 'bundle-sha256.txt').write_text(''.join(
        f'{sha(destination / relative)}  {relative}\n' for _, relative in validated))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True, type=pathlib.Path)
    parser.add_argument('--readelf', default='readelf')
    for engine in ENGINES:
        parser.add_argument('--' + engine, type=pathlib.Path)
    args = parser.parse_args()
    sources = {name: getattr(args, name) for name in ENGINES if getattr(args, name)}
    if not sources:
        parser.error('Select at least one locally built engine')
    stage(sources, args.output, args.readelf)
    print('Verified private bundle:', args.output)
    print('Gradle properties: -PcartdeckNativeBundleDir=' + str(args.output.resolve()) +
          ' -PcartdeckNativeSystems=' + (args.output / 'native-systems.txt').read_text().strip())
