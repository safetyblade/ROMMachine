#!/usr/bin/env python3
import hashlib
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


if __name__ == '__main__':
    unittest.main()
