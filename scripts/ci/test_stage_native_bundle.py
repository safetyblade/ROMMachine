import hashlib
import pathlib
import tempfile
import unittest
import zipfile
from unittest.mock import patch

from stage_native_bundle import stage, stage_xbox, stage_xbox360
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

    def xbox360_fixture(self):
        folder = self.root / 'xbox360'
        folder.mkdir()
        core = folder / 'libcartdeck_xenia.so'
        core.write_bytes(elf())
        with zipfile.ZipFile(folder / 'xenia-licenses.zip', 'w') as z:
            z.writestr('LICENSE', 'BSD 3-Clause fixture')
        (folder / 'source-manifest.json').write_text('{"revision":"fixture"}')
        (folder / 'xenia.properties').write_text(
            'revision=779680ade075d2d2a4c589c6a8d57a8e26c116c0\n'
            'core.library=libcartdeck_xenia.so\n'
            'core.sha256=' + hashlib.sha256(elf()).hexdigest() + '\n'
            'library.names=libcartdeck_xenia.so\n'
            'library.libcartdeck_xenia.so.sha256=' + hashlib.sha256(elf()).hexdigest() + '\n'
            'native.build_id=abc123\n'
            'arch=arm64-v8a\nbackend=Xenia-ARM64-Vulkan\n')
        entries = ['libcartdeck_xenia.so', 'xenia-licenses.zip', 'xenia.properties', 'source-manifest.json']
        (folder / 'sha256.txt').write_text(''.join(
            hashlib.sha256((folder / n).read_bytes()).hexdigest() + '  ' + n + '\n'
            for n in entries))
        return folder

    @staticmethod
    def xenia_readelf(args, **kwargs):
        if '-dW' in args:
            return '(NEEDED) Shared library: [libc.so]'
        if '-nW' in args:
            return 'Build ID: abc123'
        return '1: 0001 2 FUNC GLOBAL DEFAULT 7 JNI_OnLoad'

    def test_xbox360_staging_is_isolated_and_hashed(self):
        folder = self.xbox360_fixture()
        with patch('stage_native_bundle.subprocess.check_output', side_effect=self.xenia_readelf):
            entries = stage_xbox360(folder, 'readelf')
            stage({}, self.root / 'x360-out', 'readelf', xbox360=folder)
        self.assertIn('jniLibs/arm64-v8a/libcartdeck_xenia.so', [entry[1] for entry in entries])
        self.assertEqual((self.root / 'x360-out/native-systems.txt').read_text(), 'xbox360\n')
        self.assertTrue((self.root / 'x360-out/assets/cartdeck/xenia.properties').is_file())

    def test_xbox360_tampered_native_dependency_is_rejected(self):
        folder = self.xbox360_fixture()
        (folder / 'libcartdeck_xenia.so').write_bytes(b'tampered')
        with patch('stage_native_bundle.subprocess.check_output', side_effect=self.xenia_readelf):
            with self.assertRaisesRegex(ValueError, 'package checksum'):
                stage_xbox360(folder, 'readelf')

    def test_xbox360_unknown_dependency_is_rejected(self):
        folder = self.xbox360_fixture()

        def missing(args, **kwargs):
            if '-dW' in args:
                return '(NEEDED) Shared library: [libmissing.so]'
            return self.xenia_readelf(args)

        with patch('stage_native_bundle.subprocess.check_output', side_effect=missing):
            with self.assertRaisesRegex(ValueError, 'unbundled dependency'):
                stage_xbox360(folder, 'readelf')

    def test_xbox360_wrong_revision_is_rejected(self):
        folder = self.xbox360_fixture()
        props = folder / 'xenia.properties'
        props.write_text(props.read_text().replace('779680ade075d2d2a4c589c6a8d57a8e26c116c0', 'a' * 40))
        with patch('stage_native_bundle.subprocess.check_output', side_effect=self.xenia_readelf):
            with self.assertRaisesRegex(ValueError, 'unexpected native revision'):
                stage_xbox360(folder, 'readelf')

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
