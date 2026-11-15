#!/usr/bin/env python3
"""Collect source-bound native inputs for the internal engineering APK build."""
import argparse
import hashlib
import json
import pathlib
import shutil
import subprocess
import zipfile

p = argparse.ArgumentParser()
p.add_argument('--engine', required=True, choices=['armsx3', 'rpcs3-libretro'])
p.add_argument('--source', required=True, type=pathlib.Path)
p.add_argument('--build', required=True, type=pathlib.Path)
p.add_argument('--output', required=True, type=pathlib.Path)
a = p.parse_args()
receipt = json.loads((a.build / 'cartdeck-engine-receipt.json').read_text())
name = 'libarmsx3-core.so' if a.engine == 'armsx3' else 'rpcs3_libretro.so'
files = list(a.build.rglob(name))
assert len(files) == 1
core = files[0]
a.output.mkdir(parents=True, exist_ok=True)
shutil.copyfile(core, a.output / name)
shutil.copyfile(a.build / 'cartdeck-engine-receipt.json', a.output / 'receipt.json')
patch = subprocess.check_output(['git', '-C', str(a.source), 'diff', '--binary'])
assert hashlib.sha256(patch).hexdigest() == receipt['source_patch_sha256']
(a.output / 'source.patch').write_bytes(patch)
with zipfile.ZipFile(a.output / 'licenses.zip', 'w', zipfile.ZIP_DEFLATED) as z:
    z.write(a.source / 'LICENSE', 'LICENSE')
    z.writestr('SOURCE.txt', f'{a.source.name}\n{receipt["revision"]}\nUpstream source and complete submodule revisions are recorded in receipt.json.\n')
    z.writestr('source.patch', patch)
    z.writestr('receipt.json', json.dumps(receipt, indent=2))
    for directory in [a.source / '3rdparty', a.build / '_deps']:
        for f in directory.rglob('*'):
            if f.is_file() and f.stat().st_size < 1048576 and (f.name.upper().startswith(('LICENSE', 'COPYING'))):
                z.write(f, directory.name + '/' + str(f.relative_to(directory)))
print(a.engine, 'SOURCE_BOUND_INTERNAL_BUILD_INPUT_COLLECTED')
