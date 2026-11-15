#!/usr/bin/env python3
"""The engineering APK must contain both complete native engines and matching provenance."""
import hashlib
import json
import pathlib
import sys
import zipfile

apk = pathlib.Path(sys.argv[1])
with zipfile.ZipFile(apk) as z:
    result = {}
    for engine, binary in [('armsx3', 'libarmsx3-core.so'), ('rpcs3-libretro', 'librpcs3_libretro.so')]:
        props = dict(line.split('=', 1) for line in z.read(f'assets/cartdeck/ps3/{engine}.properties').decode().splitlines() if '=' in line and not line.startswith('#'))
        data = z.read('lib/arm64-v8a/' + binary)
        assert len(data) > 1048576 and data[:4] == b'\x7fELF' and data[4] == 2
        assert int.from_bytes(data[18:20], 'little') == 183
        assert hashlib.sha256(data).hexdigest() == props['core.sha256']
        notices = z.read(f'assets/cartdeck/ps3/{engine}-licenses.zip')
        assert hashlib.sha256(notices).hexdigest() == props['notices.sha256']
        result[engine] = {'revision': props['revision'], 'sha256': props['core.sha256'], 'bytes': len(data)}
    for name in ['libcartdeck_ps3.so', 'libcartdeckps3vulkanhost.so']:
        assert z.read('lib/arm64-v8a/' + name)[:4] == b'\x7fELF'
print(json.dumps({'apk': str(apk), 'engines': result, 'result': 'PS3_REAL_ENGINE_APK_PACKAGING_PASS', 'gameplay': 'UNVERIFIED'}, indent=2))
