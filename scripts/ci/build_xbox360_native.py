#!/usr/bin/env python3
"""Pinned XenDroid Android runtime build and versioned CARTDECK native package."""
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tarfile
import zipfile

from verify_hakux_native import SYSTEM
from verify_gamecube_core import verify_android_elf
from xbox360_native_abi import require_jni_entry

ROOT = pathlib.Path(__file__).resolve().parents[2]
REVISION = '779680ade075d2d2a4c589c6a8d57a8e26c116c0'
CORE = 'libcartdeck_xenia.so'


def run(*args, **kwargs):
    subprocess.run(list(map(str, args)), check=True, **kwargs)


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def verify(directory, readelf):
    files = {p.name: p for p in directory.glob('*.so')}
    if CORE not in files:
        raise ValueError('Xenia runtime missing')
    result = {}
    for name, path in files.items():
        dynamic = subprocess.check_output([readelf, '-dW', str(path)], text=True)
        verify_android_elf(path.read_bytes(), dynamic)
        import re
        needed = set(re.findall(r'\(NEEDED\).*\[(.*?)\]', dynamic))
        if needed - SYSTEM - files.keys():
            raise ValueError('Unbundled dependency: ' + str(needed - SYSTEM - files.keys()))
        notes = subprocess.check_output([readelf, '-nW', str(path)], text=True)
        build = re.search(r'Build ID: ([0-9a-f]+)', notes)
        if not build:
            raise ValueError('Native build ID missing: ' + name)
        if name == CORE:
            symbols = subprocess.check_output([readelf, '--dyn-syms', '-W', str(path)], text=True)
            require_jni_entry(symbols)
        result[name] = {'sha256': sha(path), 'build_id': build[1], 'needed': sorted(needed)}
    return result


def source_archive_filter(member):
    # Keep source directories named build, notably xenia/tools/build: those
    # scripts are required to regenerate the guest SPIR-V shader headers.
    parts = pathlib.PurePosixPath(member.name).parts[1:]
    if any(part in {'.git', '.gradle', '.cxx'} for part in parts):
        return None
    if parts[:1] == ('build',) or parts[:2] in {('app', 'build'), ('emulator-core', 'build')}:
        return None
    return member


def main():
    output = pathlib.Path(sys.argv[1]).resolve()
    work = pathlib.Path(os.environ.get('X360_BUILD_ROOT', ROOT / 'work/xbox360-native')).resolve()
    sdk = pathlib.Path(os.environ['ANDROID_HOME'])
    ndk = sdk / 'ndk/29.0.14206865'
    if not (ndk / 'build/cmake/android.toolchain.cmake').is_file():
        raise RuntimeError('Missing pinned Android NDK 29.0.14206865; install with sdkmanager before native compilation')
    for tool in ('glslangValidator', 'spirv-opt', 'spirv-dis'):
        if not shutil.which(tool):
            raise RuntimeError('Missing host shader compiler: ' + tool)
    work.mkdir(parents=True, exist_ok=True)
    source = work / 'source'
    run('git', 'clone', '--no-checkout', 'https://github.com/rfandango/XenDroid.git', source)
    run('git', '-C', source, 'checkout', '--detach', REVISION)
    run('git', '-C', source, 'submodule', 'update', '--init', '--recursive', '--depth', '1')
    submodules = subprocess.check_output(['git', '-C', str(source), 'submodule', 'status', '--recursive'], text=True)
    if any(line and line[0] in '-+U' for line in submodules.splitlines()):
        raise ValueError('Unverified submodule revisions')
    sys.path.insert(0, str(ROOT / 'scripts/ci/xbox360'))
    from prepare_source import prepare
    prepare(source, ROOT / 'scripts/ci/xbox360')
    sdk = pathlib.Path(os.environ['ANDROID_HOME'])
    (source / 'local.properties').write_text(f'sdk.dir={sdk}\ncmake.dir={sdk}/cmake/3.30.3\n')
    run('bash', source / 'gradlew', ':emulator-core:externalNativeBuildRelease',
        '--no-daemon', '--max-workers=2', cwd=source)
    candidates = list((source / 'emulator-core/build').rglob(CORE))
    if not candidates:
        candidates = list((source / 'emulator-core/.cxx').rglob(CORE))
    if not candidates:
        raise ValueError('Native build did not produce the embedded engine')
    output.mkdir(parents=True, exist_ok=False)
    native = candidates[0].parent
    llvm = sdk / 'ndk/29.0.14206865/toolchains/llvm/prebuilt/linux-x86_64/bin'
    # Only runtime dependencies, never conflicting optional GPU-driver hooks.
    for path in native.glob('*.so'):
        if 'hook' not in path.name and 'ProcessorInfo' not in path.name:
            shutil.copyfile(path, output / path.name)
    inventory = verify(output, str(llvm / 'llvm-readelf'))
    with zipfile.ZipFile(output / 'xenia-licenses.zip', 'w', zipfile.ZIP_DEFLATED) as notices:
        for p in source.rglob('*'):
            if p.is_file() and '.git' not in p.parts and any(s in p.name.lower() for s in ('license', 'copying', 'notice')):
                notices.write(p, str(p.relative_to(source)))
    manifest = {'revision': REVISION, 'core.sha256': sha(output / CORE), 'core.library': CORE,
                'library.names': ','.join(sorted(inventory)), 'upstream': 'https://github.com/rfandango/XenDroid',
                'native.build_id': inventory[CORE]['build_id'], 'android.api': '29', 'arch': 'arm64-v8a',
                'backend': 'Xenia-ARM64-Vulkan', 'diagnostics': 'cartdeck-stages-v1'}
    manifest.update({'library.' + n + '.sha256': v['sha256'] for n, v in inventory.items()})
    (output / 'xenia.properties').write_text(''.join(k+'='+v+'\n' for k,v in manifest.items()))
    provenance = {'revision': REVISION, 'submodules': submodules, 'libraries': inventory,
                  'adapter': {p.name: sha(p) for p in (ROOT / 'scripts/ci/xbox360').glob('*') if p.is_file()}}
    (output / 'source-manifest.json').write_text(json.dumps(provenance, indent=2)+'\n')
    # Retain unstripped libraries for exact-build crash symbolication.
    symbols = output / 'symbols'; symbols.mkdir()
    for name in inventory:
        shutil.copyfile(output / name, symbols / name)
        run(llvm / 'llvm-strip', '--strip-unneeded', output / name)
    # Stripping changes hashes. Manifest must describe packaged bytes.
    for name in inventory:
        manifest['library.'+name+'.sha256'] = sha(output / name)
    manifest['core.sha256'] = sha(output / CORE)
    for name in inventory:
        inventory[name]['sha256'] = sha(output / name)
    (output / 'source-manifest.json').write_text(json.dumps(provenance, indent=2)+'\n')
    (output / 'xenia.properties').write_text(''.join(k+'='+v+'\n' for k,v in manifest.items()))
    (output / 'sha256.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in output.iterdir() if p.is_file()))
    with tarfile.open(output / 'corresponding-source.tar.xz', 'w:xz', preset=1) as archive:
        archive.add(source, arcname='XenDroid', filter=source_archive_filter)
    provenance['source_archive_sha256'] = sha(output / 'corresponding-source.tar.xz')
    provenance['build_recipe_sha256'] = sha(pathlib.Path(__file__))
    (output / 'source-manifest.json').write_text(json.dumps(provenance, indent=2) + '\n')
    (output / 'sha256.txt').write_text(''.join(
        sha(p) + '  ' + p.name + '\n' for p in sorted(output.iterdir())
        if p.is_file() and p.name != 'sha256.txt'))
    print(json.dumps({'result': 'XENIA_NATIVE_SOURCE_AND_ABI_VERIFIED',
                      'revision': REVISION, 'libraries': inventory,
                      'source_archive_sha256': provenance['source_archive_sha256'],
                      'build_recipe_sha256': provenance['build_recipe_sha256'],
                      'gameplay_verified': False}, indent=2), flush=True)


if __name__ == '__main__':
    main()
