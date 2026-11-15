#!/usr/bin/env python3
"""Put both genuine ARMSX2 page models in one APK, retaining original symbol receipts."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

from ps2_next_armsx2 import SOURCE_SHA, ANDROID_SYSTEM_LIBRARIES, validate_native


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def stage(work, bundle, llvm):
    work, bundle, llvm = Path(work), Path(bundle), Path(llvm)
    proof = json.loads((work / 'native-evidence.json').read_text())
    if proof.get('source_sha') != SOURCE_SHA or proof.get('result') != 'NATIVE_SOURCE_BUILD_VERIFIED_ONLY':
        raise ValueError('Pinned source-built ARMSX2 artifact required')
    libraries = {}
    source_receipts = []
    main_hashes = {}
    for page in (4096, 16384):
        entries = [p for p in proof['artifacts'] if p['host_page_size'] == page]
        if len(entries) != 1:
            raise ValueError('Both actual source-built page models are required')
        info = entries[0]
        source = work / 'verified' / ('page-' + str(page))
        core = 'libemucore_' + ('4k' if page == 4096 else '16k') + '.so'
        if sha(source / core) != info['sha256']:
            raise ValueError('Native engine bytes differ from the passed source build')
        exports = subprocess.check_output([str(llvm / 'llvm-readelf'), '--dyn-syms', '-W', str(source / core)], text=True)
        if 'Java_kr_co_iefriends_pcsx2_NativeApp_setFreeSoftwareNotice' not in exports:
            raise ValueError('Pinned engine lacks the upstream optional startup-notice setting')
        # These are the runtime DT_NEEDED dependencies. Upstream's unrelated
        # Flurry/Savers home-screen effects are not dependencies of CARTDECK gameplay.
        pending, needed = list(info['needed']), {}
        while pending:
            name = pending.pop()
            if name in ANDROID_SYSTEM_LIBRARIES or name in needed:
                continue
            meta = info['dependency_closure'].get(name)
            if not meta or sha(source / name) != meta['sha256']:
                raise ValueError('Unresolved or changed PS2 runtime dependency: ' + name)
            needed[name] = meta
            pending.extend(meta['needed'])
        target = bundle / 'jniLibs/arm64-v8a' / core
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            raise ValueError('Engine core destination already exists')
        shutil.copyfile(source / core, target)
        subprocess.run([str(llvm / 'llvm-strip'), '--strip-unneeded', str(target)], check=True)
        packaged = validate_native(target, llvm / 'llvm-readelf')
        if packaged['build_id'] != info['build_id']:
            raise ValueError('Stripping changed the native crash BuildID')
        main_hashes[page] = packaged['sha256']
        libraries[core] = packaged['sha256']
        for name, meta in needed.items():
            if name in libraries and libraries[name] != meta['sha256']:
                raise ValueError('Page models require different same-name dependencies: ' + name)
            destination = target.parent / name
            if destination.exists() and sha(destination) != meta['sha256']:
                raise ValueError('Another engine has incompatible native dependency bytes: ' + name)
            if not destination.exists():
                shutil.copyfile(source / name, destination)
            libraries[name] = meta['sha256']
        source_receipts.append({'page_size': page, 'source_elf_sha256': info['sha256'],
                                'packaged_elf_sha256': packaged['sha256'], 'build_id': info['build_id']})
    assets = bundle / 'assets/cartdeck/armsx2'
    assets.mkdir(parents=True, exist_ok=True)
    for filename, key in (('armsx2-resources.zip', 'resources_sha256'), ('armsx2-licenses.zip', 'notices_sha256')):
        if sha(work / filename) != proof[key]:
            raise ValueError('Source resources/notices do not match their receipt')
        shutil.copyfile(work / filename, assets / filename)
    props = {'revision': SOURCE_SHA, 'upstream.revision': SOURCE_SHA,
             'host.page_size': 'universal', 'core.sha256': main_hashes[4096],
             'library.names': ','.join(sorted(libraries)),
             'resources.sha256': proof['resources_sha256'], 'notices.sha256': proof['notices_sha256']}
    props.update({'library.' + key + '.sha256': value for key, value in libraries.items()})
    (assets / 'armsx2.properties').write_text(''.join(key + '=' + value + '\n' for key, value in sorted(props.items())))
    receipt = {'source_revision': SOURCE_SHA, 'page_models': source_receipts,
               'native_libraries': libraries, 'native_rebuilt': False, 'gameplay_verified': False}
    (bundle / 'ps2-connected-evidence.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--work', required=True)
    p.add_argument('--bundle', required=True)
    p.add_argument('--llvm', required=True)
    a = p.parse_args()
    stage(a.work, a.bundle, a.llvm)
