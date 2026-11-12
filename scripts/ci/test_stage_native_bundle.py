import hashlib
import pathlib
import tempfile
import unittest
import zipfile
from unittest.mock import patch

from stage_native_bundle import stage, stage_xbox
from test_verify_gamecube_core import elf


class NativeStageContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        for name in ('libhakuxcore.so', 'libSDL3.so'):
            (self.root / name).write_bytes(elf())
        with zipfile.ZipFile(self.root / 'hakux-licenses.zip', 'w') as z:
            z.writestr('LICENSE', 'GPL-3.0-or-later fixture')
        (self.root / 'hakux.properties').write_text(
            'revision=' + 'a' * 40 + '\ncore.sha256=' +
            hashlib.sha256(elf()).hexdigest() + '\n')
        self.manifest()

    def manifest(self):
        names = ['libhakuxcore.so', 'libSDL3.so', 'hakux-licenses.zip']
        (self.root / 'sha256.txt').write_text(''.join(
            hashlib.sha256((self.root / n).read_bytes()).hexdigest() + '  ' + n + '\n'
            for n in names))

    @staticmethod
    def readelf(args, **kwargs):
        if '-dW' in args:
            return '(NEEDED) Shared library: [libc.so]'
        names = ('SDL_main', 'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPads',
                 'Java_com_rfandango_haku_1x_NativeCoreActivity_nativeSetPaused',
                 'SDL_Init', 'SDL_Quit', 'SDL_CreateWindow')
        return '\n'.join('1: 0001 2 FUNC GLOBAL DEFAULT 7 ' + n for n in names)

    def test_wii_cannot_enable_without_matching_dolphin(self):
        with self.assertRaisesRegex(ValueError, 'matching Dolphin'):
            stage({}, self.root / 'out', 'readelf', wii=True)

    def test_native_backend_passes_without_libretro_exports(self):
        with patch('stage_native_bundle.subprocess.check_output', side_effect=self.readelf):
            entries = stage_xbox(self.root, 'readelf')
        self.assertIn('jniLibs/arm64-v8a/libhakuxcore.so', [entry[1] for entry in entries])

    def test_changed_dependency_rejected(self):
        (self.root / 'libSDL3.so').write_bytes(b'changed')
        with patch('stage_native_bundle.subprocess.check_output', side_effect=self.readelf):
            with self.assertRaisesRegex(ValueError, 'dependency checksum'):
                stage_xbox(self.root, 'readelf')

    def test_missing_android_dependency_rejected(self):
        def output(args, **kwargs):
            if '-dW' in args:
                return '(NEEDED) Shared library: [libmissing.so]'
            return self.readelf(args)
        with patch('stage_native_bundle.subprocess.check_output', side_effect=output):
            with self.assertRaisesRegex(ValueError, 'unbundled native dependencies'):
                stage_xbox(self.root, 'readelf')

    def test_missing_sdl_entrypoint_rejected(self):
        def output(args, **kwargs):
            return self.readelf(args).replace('SDL_main', 'unused_main')
        with patch('stage_native_bundle.subprocess.check_output', side_effect=output):
            with self.assertRaisesRegex(ValueError, 'native host ABI'):
                stage_xbox(self.root, 'readelf')

    def test_path_escape_in_dependency_manifest_rejected(self):
        with (self.root / 'sha256.txt').open('a') as f:
            f.write('a' * 64 + '  ../escape.so\n')
        with patch('stage_native_bundle.subprocess.check_output', side_effect=self.readelf):
            with self.assertRaisesRegex(ValueError, 'unsafe or duplicate'):
                stage_xbox(self.root, 'readelf')


if __name__ == '__main__':
    unittest.main()
