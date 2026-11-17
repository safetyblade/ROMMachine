#!/usr/bin/env python3
"""Assert real PS3 payloads, native ABI/dependency closure and source notices.

Packaging evidence only; neither licensing clearance nor native gameplay proof.
"""
import hashlib
import io
import json
import pathlib
import re
import subprocess
import sys
import tempfile
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / 'ci'))
from verify_gamecube_core import verify_android_elf

PINS = {'armsx3': 'd396f0b6b8372789e6423a4f4d242cab4c5135eb',
        'rpcs3-libretro': '993a9914a05361e6b3afe0487c463d9cb5f5b12b'}
SYSTEM = {'libc.so', 'libm.so', 'libdl.so', 'liblog.so', 'libandroid.so', 'libvulkan.so',
          'libaaudio.so', 'libz.so', 'libEGL.so', 'libGLESv2.so', 'libGLESv3.so',
          'libOpenSLES.so', 'libamidi.so'}


def native(data, names):
    with tempfile.NamedTemporaryFile(suffix='.so') as f:
        f.write(data)
        f.flush()
        dynamic = subprocess.check_output(['readelf', '-dW', f.name], text=True)
        notes = subprocess.check_output(['readelf', '-nW', f.name], text=True)
    verify_android_elf(data, dynamic)
    build_id = re.search(r'Build ID:\s*([0-9a-f]+)', notes)
    assert build_id, 'Native build ID missing'
    dependencies = re.findall(r'\(NEEDED\).*?\[(.*?)\]', dynamic)
    for dependency in dependencies:
        assert dependency in SYSTEM or 'lib/arm64-v8a/' + dependency in names, dependency
    return {'build_id': build_id[1], 'needed': dependencies,
            'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data)}


def verify(apk):
    with zipfile.ZipFile(apk) as z:
        result = {}
        for engine, binary in [('armsx3', 'libarmsx3-core.so'), ('rpcs3-libretro', 'librpcs3_libretro.so')]:
            props = dict(line.split('=', 1) for line in z.read(f'assets/cartdeck/ps3/{engine}.properties')
                         .decode().splitlines() if '=' in line and not line.startswith('#'))
            assert props['revision'] == PINS[engine], 'Wrong PS3 source pin'
            data = z.read('lib/arm64-v8a/' + binary)
            assert len(data) > 1048576, 'Wrapper-only PS3 engine rejected'
            info = native(data, z.namelist())
            assert info['sha256'] == props['core.sha256']
            notices = z.read(f'assets/cartdeck/ps3/{engine}-licenses.zip')
            assert hashlib.sha256(notices).hexdigest() == props['notices.sha256']
            with zipfile.ZipFile(io.BytesIO(notices)) as source:
                assert source.read('LICENSE'), 'Missing upstream licence'
                assert props['revision'] in source.read('SOURCE.txt').decode(), 'Source notice pin mismatch'
            result[engine] = dict(info, revision=props['revision'])
        for name in ['libcartdeck_ps3.so', 'libcartdeckps3vulkanhost.so']:
            result[name] = native(z.read('lib/arm64-v8a/' + name), z.namelist())
    return {'apk': str(apk), 'engines': result, 'result': 'PS3_REAL_ENGINE_APK_PACKAGING_PASS',
            'gameplay': 'UNVERIFIED', 'licensing_clearance': 'SEPARATE_REQUIRED_CONTROL'}


if __name__ == '__main__':
    print(json.dumps(verify(pathlib.Path(sys.argv[1])), indent=2))
