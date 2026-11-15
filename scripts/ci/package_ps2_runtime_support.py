#!/usr/bin/env python3
"""Package licensed resources for an existing pinned ARMSX2 build; never compile an engine."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import urllib.request
import zipfile

from ps2_next_armsx2 import SOURCE_SHA, UPSTREAM, collect_notices
from ps2_rust_notices import PIN as RUST_PIN, retain
from ps2_sdl_contract import verify as verify_sdl_contract


def run(*args):
    subprocess.run(list(map(str, args)), check=True)


def checkout(url, revision, destination):
    run('git', 'clone', '--depth', '1', '--no-checkout', url, destination)
    run('git', '-C', destination, 'fetch', '--depth', '1', 'origin', revision)
    run('git', '-C', destination, 'checkout', '--detach', revision)
    actual = subprocess.check_output(['git', '-C', str(destination), 'rev-parse', 'HEAD'], text=True).strip()
    if actual != revision:
        raise ValueError('Resource source pin mismatch')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main(work, ndk):
    work = Path(work)
    receipt_path = work / 'native-evidence.json'
    proof = json.loads(receipt_path.read_text())
    if proof.get('source_sha') != SOURCE_SHA or proof.get('result') != 'NATIVE_SOURCE_BUILD_VERIFIED_ONLY':
        raise ValueError('The existing native artifact is not the pinned genuine engine')
    source = work / 'source'
    if not source.exists():
        source = work / 'support-source'
        checkout(UPSTREAM, SOURCE_SHA, source)
        run('git', '-C', source, 'submodule', 'update', '--init', '--recursive', '--depth', '1')
    elif subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip() != SOURCE_SHA:
        raise ValueError('Existing source checkout differs from the runtime source pin')
    proof['sdl_jni_contract'] = verify_sdl_contract(source)
    with zipfile.ZipFile(work / 'armsx2-resources.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((source / 'bin/resources').rglob('*')):
            if path.is_file():
                archive.writestr(path.relative_to(source / 'bin/resources').as_posix(), path.read_bytes())
    rust = work / 'shader-source'
    checkout('https://github.com/SnowflakePowered/librashader.git', RUST_PIN, rust)
    with zipfile.ZipFile(work / 'armsx2-licenses.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for name in collect_notices(source):
            archive.writestr(name, (source / name).read_bytes())
        shader_licences = (
            ('KhronosGroup/glslang', '09c541ee5b22bbac307987b50d86ec2b4f683d75', 'LICENSE.txt'),
            ('KhronosGroup/SPIRV-Tools', 'e4bceacf59fdfe742047e94e41ef65a48999a0dd', 'LICENSE'),
            ('KhronosGroup/SPIRV-Headers', '465055f6c9128772e20082e893d974146acf7a02', 'LICENSE'),
        )
        for repo, revision, name in shader_licences:
            url = 'https://raw.githubusercontent.com/' + repo + '/' + revision + '/' + name
            with urllib.request.urlopen(url, timeout=60) as response:
                archive.writestr('shaderc/' + repo.split('/')[1] + '/' + name, response.read())
        proof['rust_dependencies'] = retain(source, work, archive, ndk, librashader_source=rust)
        archive.writestr('native-source-receipt.json', json.dumps(proof, indent=2))
    proof['resources_sha256'] = sha(work / 'armsx2-resources.zip')
    proof['notices_sha256'] = sha(work / 'armsx2-licenses.zip')
    proof['runtime_support_source'] = SOURCE_SHA
    receipt_path.write_text(json.dumps(proof, indent=2) + '\n')
    print('Pinned ARMSX2 resources and licence support packaged; native binaries reused unchanged.')


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--work', required=True)
    p.add_argument('--ndk', required=True)
    a = p.parse_args()
    main(a.work, a.ndk)
