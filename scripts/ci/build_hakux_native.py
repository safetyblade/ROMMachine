#!/usr/bin/env python3
"""Pinned Android hakuX build, source handoff and verifiable native package."""
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tarfile
import zipfile

from verify_hakux_native import verify

ROOT = pathlib.Path(__file__).resolve().parents[2]
REVISION = 'a7cdb5a975e31730d488c47f6f10440b16c1e6f4'
PATCHES = ('hakux-libslirp-4.7-ssize.patch', 'hakux-vulkan-loader-version.patch', 'hakux-confirmed-presentation.patch', 'hakux-gpu-report.patch', 'hakux-presentation-diagnostics.patch', 'hakux-pixel-stage-probe.patch')
DEPS = {
    'SDL3': ('libsdl-org/SDL', '829a65d769d935c4852f8159e964312c0957260a'),
    'glslang': ('KhronosGroup/glslang', 'b5782e52ee2f7b3e40bb9c80d15b47016e008bc9'),
    'spirv_reflect': ('KhronosGroup/SPIRV-Reflect', 'ef913b3ab3da1becca3cf46b15a10667c67bebe5'),
    'vma': ('GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator', '1d8f600fd424278486eade7ed3e877c99f0846b1'),
    'volk': ('zeux/volk', '0b17a763ba5643e32da1b2152f8140461b3b7345'),
    'adrenotools': ('bylaws/libadrenotools', '8fae8ce254dfc1344527e05301e43f37dea2df80'),
    'libslirp': ('qemu/libslirp', '3ad1710a96678fe79066b1469cead4058713a1d9'),
    'root/subprojects/nv2a_vsh_cpu': ('xemu-project/nv2a_vsh_cpu', '1115255708c10c4841b65dcd2223262e7a316598'),
}


def run(*args, **kwargs):
    subprocess.run([str(a) for a in args], check=True, **kwargs)


def capture(*args):
    return subprocess.check_output([str(a) for a in args], text=True).strip()


def checkout(path, repo, revision):
    path.mkdir(parents=True, exist_ok=True)
    if not (path / '.git').exists():
        run('git', 'init', path)
        run('git', '-C', path, 'remote', 'add', 'origin', 'https://github.com/' + repo + '.git')
    if capture('git', '-C', path, 'status', '--porcelain'):
        raise ValueError('Refusing to overwrite changed source: ' + str(path))
    run('git', '-C', path, 'fetch', '--depth', '1', 'https://github.com/' + repo + '.git', revision)
    run('git', '-C', path, 'checkout', '--detach', revision)


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def package(source, deps, build, output, ndk):
    output.mkdir(parents=True, exist_ok=True)
    llvm = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
    libraries = [build / 'libhakuxcore.so', build / 'sdl3/libSDL3.so']
    libraries += sorted((build / 'adrenotools/src/hook').glob('lib*.so'))
    for library in libraries:
        run(llvm / 'llvm-strip', '--strip-unneeded', '-o', output / library.name, library)
    (output / 'native-verification.json').write_text(json.dumps(verify(output, str(llvm / 'llvm-readelf')), indent=2) + '\n')
    manifest = {'upstream': 'https://github.com/rfandango/hakuX', 'revision': REVISION,
                'license': 'GPL-3.0-or-later; see bundled per-component notices',
                'core.sha256': digest(output / 'libhakuxcore.so'),
                'android.ndk': '29.0.14206865', 'android.api': '28', 'arch': 'arm64-v8a',
                'backend': 'SDL3-Vulkan', 'cartdeck.presentation': 'vk-present-guest-v1', 'cartdeck.pixelProbe': 'async-grid-v1', 'fex.revision': capture('git', '-C', source / 'external/fex', 'rev-parse', 'HEAD')}
    names = sorted(library.name for library in libraries)
    manifest['library.names'] = ','.join(names)
    manifest.update({'library.' + name + '.sha256': digest(output / name) for name in names})
    (output / 'hakux.properties').write_text(''.join(k + '=' + v + '\n' for k, v in manifest.items()))
    inventory = {'hakuX': REVISION, **{k: {'repository': repo, 'revision': rev} for k, (repo, rev) in DEPS.items()},
                 'cartdeck.patches': {name: digest(ROOT / 'scripts/ci/patches' / name) for name in PATCHES},
                 'submodules': capture('git', '-C', source, 'submodule', 'status', '--recursive'),
                 'deviations': ['nv2a_vsh_cpu documented short 561fe80 unavailable in published repository; pinned published 1115255, built against its ABI',
                                'libslirp selected official mirrored 4.7.0 tag 3ad1710 instead of unavailable documented 26be815b'],
                 'distribution': 'Engineering only; public distribution decision and per-file licence review remain outstanding.'}
    (output / 'source-manifest.json').write_text(json.dumps(inventory, indent=2) + '\n')
    with zipfile.ZipFile(output / 'hakux-licenses.zip', 'w', zipfile.ZIP_DEFLATED) as notices:
        notices.write(output / 'source-manifest.json', 'source-manifest.json')
        for name in ('NOTICE', 'NOTICE.toolchain', 'source.properties'):
            notices.write(ndk / name, 'android-ndk/' + name)
        for prefix, tree in [('hakuX', source), ('dependencies', deps)]:
            for item in sorted(tree.rglob('*')):
                if item.is_file() and '.git' not in item.parts and any(token in item.name.lower() for token in ('license', 'copying', 'notice', 'copyright')):
                    notices.write(item, prefix + '/' + item.relative_to(tree).as_posix())
    (output / 'sha256.txt').write_text(''.join(digest(p) + '  ' + p.name + '\n' for p in sorted(output.iterdir())
                                            if p.is_file() and p.name != 'sha256.txt'))
    compliance = output / 'source-compliance'
    compliance.mkdir(exist_ok=True)
    archive = compliance / 'hakux-corresponding-source.tar.xz'
    def filtered(info):
        return None if '.git' in pathlib.PurePosixPath(info.name).parts else info
    with tarfile.open(archive, 'w:xz', preset=3) as tar:
        tar.add(source, arcname='hakuX', filter=filtered)
        tar.add(deps, arcname='dependencies', filter=filtered)
        tar.add(output / 'source-manifest.json', arcname='source-manifest.json')
        for name in ('build_hakux_native.py', 'build_hakux_native.sh', 'verify_hakux_native.py'):
            tar.add(ROOT / 'scripts/ci' / name, arcname='cartdeck-build/' + name)
        for name in PATCHES:
            tar.add(ROOT / 'scripts/ci/patches' / name, arcname='cartdeck-build/patches/' + name)
    (compliance / 'sha256.txt').write_text(digest(archive) + '  ' + archive.name + '\n')
    print('Built actual Android hakuX native package:', output)


def main():
    output = pathlib.Path(sys.argv[1]).resolve()
    ndk = pathlib.Path(os.environ['ANDROID_NDK_HOME'])
    work = pathlib.Path(os.environ.get('HAKUX_BUILD_ROOT', ROOT / 'work/hakux-native')).resolve()
    source, deps, build = work / 'source', work / 'deps', work / 'build'
    checkout(source, 'rfandango/hakuX', REVISION)
    for name in PATCHES:
        run('git', '-C', source, 'apply', ROOT / 'scripts/ci/patches' / name)
    run('git', '-C', source, 'submodule', 'update', '--init', '--depth', '1', 'external/fex')
    run('git', '-C', source / 'external/fex', 'submodule', 'update', '--init', '--depth', '1',
        'External/fmt', 'External/range-v3', 'External/rpmalloc', 'External/unordered_dense',
        'External/xxhash', 'Source/Common/cpp-optparse')
    for name, (repo, revision) in DEPS.items():
        checkout(deps / name, repo, revision)
    run('git', '-C', deps / 'adrenotools', 'submodule', 'update', '--init', '--recursive', '--depth', '1')
    run('git', '-C', deps / 'adrenotools', 'apply', source / 'tools/native/adrenotools-kgsl-context-priority.patch')
    run('cmake', '-S', source / 'native', '-B', build, '-G', 'Ninja',
        '-DCMAKE_TOOLCHAIN_FILE=' + str(ndk / 'build/cmake/android.toolchain.cmake'),
        '-DANDROID_ABI=arm64-v8a', '-DANDROID_PLATFORM=android-28', '-DANDROID_STL=c++_static',
        '-DCMAKE_BUILD_TYPE=Release', '-DNC_DEPS=' + str(deps),
        '-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384')
    run('cmake', '--build', build, '--target', 'hakuxcore', '--parallel', os.environ.get('NATIVE_BUILD_JOBS', '2'))
    package(source, deps, build, output, ndk)


if __name__ == '__main__':
    main()
