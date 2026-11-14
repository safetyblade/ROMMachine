#!/usr/bin/env python3
import hashlib
import io
import pathlib
import struct
import tempfile
import unittest
import zipfile

from verify_native_bundle_apk import ENGINE_LIBRARIES, LEGACY_LIBRARIES, verify


def elf(machine=183):
    data = bytearray(120)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<H', data, 18, machine)
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 1)
    struct.pack_into('<I', data, 64, 1)
    struct.pack_into('<Q', data, 112, 16384)
    return bytes(data)


class FullApkContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        self.bundle = self.root / 'bundle'
        self.bundle.mkdir()
        (self.bundle / 'native-systems.txt').write_text('dc,gc,ps2\n')
        self.host = self.root / 'host.aar'
        with zipfile.ZipFile(self.host, 'w') as z:
            z.writestr('jni/arm64-v8a/liblibretrodroid.so', elf() + b'patched host')
        self.entries = {'lib/arm64-v8a/' + n: elf() for n in ENGINE_LIBRARIES | LEGACY_LIBRARIES}
        self.entries['lib/arm64-v8a/liblibretrodroid.so'] = elf() + b'patched host'
        self.entries['classes.dex'] = b'dex fixture\0dc,gc,ps2\0'
        (self.bundle / 'bundle-sha256.txt').write_text(''.join(
            hashlib.sha256(elf()).hexdigest() + '  jniLibs/arm64-v8a/' + n + '\n'
            for n in sorted(ENGINE_LIBRARIES)))

    def write_apk(self):
        apk = self.root / 'app.apk'
        with zipfile.ZipFile(apk, 'w', zipfile.ZIP_DEFLATED) as z:
            for name, data in self.entries.items():
                z.writestr(name, data)
        return apk

    def check(self):
        return verify(self.write_apk(), self.bundle, True, self.host)

    def test_full_bundle_and_exact_patched_host_pass(self):
        report = self.check()
        self.assertTrue(report['all_five_engines_bundled'])
        self.assertTrue(report['patched_host_matches'])
        self.assertTrue(report['all_native_elf_16k'])

    def test_baseline_or_missing_candidate_cannot_publish(self):
        del self.entries['lib/arm64-v8a/libplay_modern_libretro_android.so']
        with self.assertRaisesRegex(ValueError, 'engines/fallbacks missing'):
            self.check()

    def test_missing_original_fallback_cannot_publish(self):
        del self.entries['lib/arm64-v8a/libppsspp_libretro_android.so']
        with self.assertRaisesRegex(ValueError, 'engines/fallbacks missing'):
            self.check()

    def test_binaries_without_enabled_library_registry_cannot_publish(self):
        self.entries['classes.dex'] = b'baseline APK without new consoles'
        with self.assertRaisesRegex(ValueError, 'registry enabled'):
            self.check()

    def test_stock_or_modified_host_cannot_publish(self):
        self.entries['lib/arm64-v8a/liblibretrodroid.so'] = elf() + b'stock host'
        with self.assertRaisesRegex(ValueError, 'patched graphics/input host'):
            self.check()

    def test_modified_native_candidate_cannot_publish(self):
        self.entries['lib/arm64-v8a/libflycast_libretro_android.so'] += b'changed'
        with self.assertRaisesRegex(ValueError, 'changed verified bundle bytes'):
            self.check()

    def test_wrong_architecture_hidden_under_arm64_path_cannot_publish(self):
        self.entries['lib/arm64-v8a/libppsspp_libretro_android.so'] = elf(62)
        with self.assertRaisesRegex(ValueError, 'non-ARM64 or non-16KB'):
            self.check()

    def expansion(self, xbox=False):
        systems = 'dc,gc,ps2,wii' + (',xbox' if xbox else '')
        (self.bundle / 'native-systems.txt').write_text(systems + '\n')
        self.entries['classes.dex'] = b'dex fixture\0' + systems.encode() + b'\0'
        if xbox:
            self.entries['lib/arm64-v8a/libhakuxcore.so'] = elf()
            self.entries['lib/arm64-v8a/libSDL3.so'] = elf()
            self.entries['assets/cartdeck/hakux.properties'] = b'revision=' + b'a' * 40
            self.entries['classes.dex'] += (
                b'Lcom/rfandango/haku_x/NativeCoreActivity;\0'
                b'Lcom/swordfish/lemuroid/app/shared/game/xbox/XboxGameLauncher;\0'
                b'Lcom/swordfish/lemuroid/app/shared/game/xbox/XboxEngineStore;\0'
            )
            self.entries['AndroidManifest.xml'] = b'com.rfandango.haku_x.NativeCoreActivity'

    def test_wii_reuses_verified_dolphin_and_requires_registry(self):
        self.expansion()
        report = verify(self.write_apk(), self.bundle, True, self.host, require_wii=True)
        self.assertTrue(report['wii_dolphin_bundled'])
        self.entries['classes.dex'] = b'dc,gc,ps2\0'
        with self.assertRaisesRegex(ValueError, 'registry enabled'):
            verify(self.write_apk(), self.bundle, True, self.host, require_wii=True)

    def test_xbox_requires_native_backend_and_provenance(self):
        self.expansion(xbox=True)
        report = verify(self.write_apk(), self.bundle, True, self.host, True, True)
        self.assertTrue(report['xbox_native_backend_bundled'])
        del self.entries['lib/arm64-v8a/libhakuxcore.so']
        with self.assertRaisesRegex(ValueError, 'native backend is missing'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True)

    def test_xbox_registration_without_provenance_cannot_publish(self):
        self.expansion(xbox=True)
        del self.entries['assets/cartdeck/hakux.properties']
        with self.assertRaisesRegex(ValueError, 'provenance is missing'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True)

    def test_xbox_binaries_without_internal_host_cannot_publish(self):
        self.expansion(xbox=True)
        self.entries['classes.dex'] = b'dc,gc,ps2,wii,xbox\0'
        with self.assertRaisesRegex(ValueError, 'host implementation is missing'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True)

    def test_xbox_unregistered_activity_cannot_publish(self):
        self.expansion(xbox=True)
        self.entries['AndroidManifest.xml'] = b'baseline manifest'
        with self.assertRaisesRegex(ValueError, 'not registered'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True)

    def xbox360_expansion(self):
        self.expansion(xbox=True)
        systems = 'dc,gc,ps2,wii,xbox,xbox360'
        (self.bundle / 'native-systems.txt').write_text(systems + '\n')
        self.entries['classes.dex'] = self.entries['classes.dex'].replace(
            b'dc,gc,ps2,wii,xbox\0', systems.encode('utf-8') + b'\0')
        self.entries['classes.dex'] += (
            b'Lcom/swordfish/lemuroid/app/shared/game/xbox360/Xbox360Activity;\0'
            b'Lcom/swordfish/lemuroid/app/shared/game/xbox360/Xbox360GameLauncher;\0'
        )
        self.entries['AndroidManifest.xml'] += b'com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360Activity'
        self.entries['lib/arm64-v8a/libcartdeck_xenia.so'] = elf()
        self.entries['assets/cartdeck/xenia.properties'] = b'revision=779680ade075d2d2a4c589c6a8d57a8e26c116c0'
        self.entries['assets/cartdeck/xenia/source-manifest.json'] = b'{"revision":"fixture"}'
        memory = io.BytesIO()
        with zipfile.ZipFile(memory, 'w') as archive:
            archive.writestr('LICENSE', 'BSD 3-Clause fixture')
        self.entries['assets/cartdeck/xenia/xenia-licenses.zip'] = memory.getvalue()

    def test_xbox360_requires_verified_engine_and_isolated_host(self):
        self.xbox360_expansion()
        result = verify(self.write_apk(), self.bundle, True, self.host, True, True, True)
        self.assertTrue(result['xbox360_native_backend_bundled'])
        del self.entries['lib/arm64-v8a/libcartdeck_xenia.so']
        with self.assertRaisesRegex(ValueError, 'native engine is missing'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True, True)

    def test_xbox360_rejects_missing_provenance_and_host(self):
        self.xbox360_expansion()
        del self.entries['assets/cartdeck/xenia/xenia-licenses.zip']
        with self.assertRaisesRegex(ValueError, 'provenance/licenses missing'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True, True)
        self.xbox360_expansion()
        self.entries['AndroidManifest.xml'] = b'com.rfandango.haku_x.NativeCoreActivity'
        with self.assertRaisesRegex(ValueError, 'isolated Activity not registered'):
            verify(self.write_apk(), self.bundle, True, self.host, True, True, True)

    def test_existing_five_engine_apk_cannot_pass_as_wii_expansion(self):
        with self.assertRaisesRegex(ValueError, 'Required library systems'):
            verify(self.write_apk(), self.bundle, True, self.host, require_wii=True)


if __name__ == '__main__':
    unittest.main()
