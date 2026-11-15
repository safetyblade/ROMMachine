#!/usr/bin/env python3
"""Stage the actual source-built PS3 engines into an existing connected CARTDECK bundle.
No firmware, games, licences/keys for games, placeholder engines or downloaded APK binaries.
Run only for the separately cleared PS3 engineering distribution; ordinary master stays unchanged.
"""
import argparse
import hashlib
import json
import pathlib
import shutil
import subprocess
import zipfile

PINS = {'armsx3': 'd396f0b6b8372789e6423a4f4d242cab4c5135eb', 'rpcs3-libretro': '993a9914a05361e6b3afe0487c463d9cb5f5b12b'}

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        while chunk := f.read(1048576):
            h.update(chunk)
    return h.hexdigest()

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--bundle', type=pathlib.Path, required=True)
    p.add_argument('--armsx3-source', type=pathlib.Path, required=True)
    p.add_argument('--rpcs3-source', type=pathlib.Path, required=True)
    p.add_argument('--armsx3-core', type=pathlib.Path, required=True)
    p.add_argument('--rpcs3-core', type=pathlib.Path, required=True)
    p.add_argument('--armsx3-receipt', type=pathlib.Path, required=True)
    p.add_argument('--rpcs3-receipt', type=pathlib.Path, required=True)
    p.add_argument('--native-bridge', type=pathlib.Path, required=True)
    p.add_argument('--libretro-host', type=pathlib.Path, required=True)
    p.add_argument('--ndk', type=pathlib.Path, required=True)
    args = p.parse_args()
    native = args.bundle / 'jniLibs/arm64-v8a'
    assets = args.bundle / 'assets/cartdeck/ps3'
    native.mkdir(parents=True, exist_ok=True)
    assets.mkdir(parents=True, exist_ok=True)
    tools = args.ndk / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
    evidence = {}
    for id, source, core, name in [
        ('armsx3', args.armsx3_source, args.armsx3_core, 'libarmsx3-core.so'),
        ('rpcs3-libretro', args.rpcs3_source, args.rpcs3_core, 'librpcs3_libretro.so'),
    ]:
        sha = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip()
        if sha != PINS[id]:
            raise ValueError('Native source pin mismatch: ' + id)
        receipt_path = args.armsx3_receipt if id == 'armsx3' else args.rpcs3_receipt
        receipt = json.loads(receipt_path.read_text())
        if receipt['revision'] != sha or receipt['source_elf_sha256'] != digest(core):
            raise ValueError('Source build receipt does not match emulator ELF: ' + id)
        if receipt['arm64_16k_load_alignment'] != 'PASS' or receipt['exported_native_abi'] != 'PASS':
            raise ValueError('Native acceptance checks did not pass: ' + id)
        if core.stat().st_size < 1048576:
            raise ValueError('Wrapper-only or empty engine rejected: ' + id)
        target = native / name
        shutil.copyfile(core, target)
        subprocess.run([str(tools / 'llvm-strip'), '--strip-unneeded', str(target)], check=True)
        header = subprocess.check_output([str(tools / 'llvm-readelf'), '-h', str(target)], text=True)
        if 'AArch64' not in header:
            raise ValueError('Wrong emulator ABI: ' + id)
        notices = assets / f'{id}-licenses.zip'
        with zipfile.ZipFile(notices, 'w', zipfile.ZIP_DEFLATED) as z:
            z.write(source / 'LICENSE', 'LICENSE')
            z.writestr('SOURCE.txt', f'https://github.com/{"ARMSX2/ARMSX3" if id == "armsx3" else "WizzardSK/rpcs3-libretro"}\n{sha}\n')
            z.writestr('submodules.txt', subprocess.check_output(['git', '-C', str(source), 'submodule', 'status', '--recursive'], text=True))
            for file in source.glob('3rdparty/**/LICENSE*'):
                if file.is_file() and file.stat().st_size < 1048576:
                    z.write(file, str(file.relative_to(source)))
        h = digest(target)
        properties = {'revision': sha, 'core.sha256': h, 'library.names': name, f'library.{name}.sha256': h, 'notices.sha256': digest(notices), 'architecture': 'arm64-v8a', 'renderer': 'Vulkan', 'gameplay': 'UNVERIFIED'}
        (assets / f'{id}.properties').write_text(''.join(f'{k}={v}\n' for k, v in properties.items()))
        evidence[id] = dict(properties, source_elf_sha256=digest(core), source_build_receipt=receipt)
    for source, name in [(args.native_bridge, 'libcartdeck_ps3.so'), (args.libretro_host, 'libcartdeckps3vulkanhost.so')]:
        shutil.copyfile(source, native / name)
        evidence[name] = digest(native / name)
    # DT_NEEDED must resolve to Android system libraries or an explicitly packaged dependency.
    allowed = {'libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libandroid.so', 'libvulkan.so', 'libaaudio.so', 'libz.so', 'libEGL.so', 'libGLESv2.so', 'libOpenSLES.so'}
    import re
    for name in ['libarmsx3-core.so', 'librpcs3_libretro.so', 'libcartdeck_ps3.so', 'libcartdeckps3vulkanhost.so']:
        dynamic = subprocess.check_output([str(tools / 'llvm-readelf'), '-d', str(native / name)], text=True)
        for dependency in re.findall(r'\(NEEDED\).*?\[(.*?)\]', dynamic):
            if dependency not in allowed and not (native / dependency).is_file():
                raise ValueError(f'{name}: unresolved native dependency {dependency}')
    (args.bundle / 'ps3-engineering-evidence.json').write_text(json.dumps(evidence, indent=2))
    print(json.dumps(evidence, indent=2))

if __name__ == '__main__':
    main()
