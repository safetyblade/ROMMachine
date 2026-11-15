#!/usr/bin/env python3
"""Check the delivered APK contains the connected runtimes and their resource manifests."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def verify(apk, bundle):
    with zipfile.ZipFile(apk) as archive:
        for prefix in ('jniLibs', 'assets'):
            for source in (Path(bundle) / prefix).rglob('*'):
                if not source.is_file():
                    continue
                member = source.relative_to(bundle).as_posix().replace('jniLibs/', 'lib/', 1)
                if hashlib.sha256(archive.read(member)).digest() != hashlib.sha256(source.read_bytes()).digest():
                    raise ValueError('APK differs from connected native payload: ' + member)
        for native in ('libemucore_4k.so', 'libemucore_16k.so', 'libcemu_libretro_android.so',
                       'libcartdeckwiiuvulkanhost.so', 'libcartdeck_xenia.so', 'libplay_modern_libretro_android.so'):
            if not archive.read('lib/arm64-v8a/' + native):
                raise ValueError('Required real engine missing: ' + native)
        account = archive.read('assets/cartdeck/xenia-profile/Account')
        profile = json.loads(archive.read('assets/cartdeck/xenia-profile/profile-source.json'))
        if (len(account) != 0x194 or hashlib.sha256(account).hexdigest() != profile['sha256'] or
                profile['revision'] != '779680ade075d2d2a4c589c6a8d57a8e26c116c0' or
                not profile['offline_only'] or not profile['native_encrypt_decrypt_verified'] or profile['xuid_bound']):
            raise ValueError('Source-verified local Xbox 360 profile missing')
        dex = [archive.read(name) for name in archive.namelist() if name.endswith('.dex')]
        for name in ('kr/co/iefriends/pcsx2/Armsx2JniContract',
                     'com/swordfish/lemuroid/app/debug/Armsx2RuntimeProbeActivity',
                     'com/swordfish/lemuroid/app/debug/WiiuVulkanRuntimeProbeActivity',
                     'com/swordfish/lemuroid/app/shared/game/xbox360/Xbox360Activity'):
            if not any(('L' + name + ';').encode() in data for data in dex):
                raise ValueError('Connected gameplay host missing: ' + name)
        with zipfile.ZipFile(Path(bundle) / 'assets/cartdeck/armsx2/armsx2-resources.zip') as resources:
            if 'GameIndex.yaml' not in resources.namelist():
                raise ValueError('Real ARMSX2 resources missing')
    return {'apk_sha256': hashlib.sha256(Path(apk).read_bytes()).hexdigest(),
            'apk_bytes': Path(apk).stat().st_size, 'ps2_page_models': [4096, 16384],
            'native_engines_packaged': ['ARMSX2', 'Cemu', 'Xenia'], 'play_fallback_packaged': True,
            'physical_gameplay_verified': False}


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--apk', required=True)
    p.add_argument('--bundle', required=True)
    a = p.parse_args()
    print(json.dumps(verify(a.apk, a.bundle), indent=2))
