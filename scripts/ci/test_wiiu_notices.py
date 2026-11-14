import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from wiiu_notices import collect


class WiiuNoticesTests(unittest.TestCase):
    def test_built_dependency_notices_and_actual_patch_accompany_binary(self):
        with tempfile.TemporaryDirectory() as folder:
            work = Path(folder)
            (work / 'source/dependencies').mkdir(parents=True)
            (work / 'source/LICENSE.txt').write_text('upstream licence')
            (work / 'source/CMakeLists.txt').write_text('actual patched source')
            dep = work / 'build/vcpkg_installed/arm64-android/share/zstd'
            dep.mkdir(parents=True)
            (dep / 'copyright').write_text('actual dependency licence')
            (work / 'source-patches.json').write_text('[{"patch":"usb-option"}]')
            (work / 'native-evidence.json').write_text('{}')
            digest = collect(work)
            archive = work / 'cemu-licenses.zip'
            self.assertEqual(digest, hashlib.sha256(archive.read_bytes()).hexdigest())
            self.assertEqual(digest, json.loads((work / 'native-evidence.json').read_text())['notices_sha256'])
            with zipfile.ZipFile(archive) as z:
                self.assertEqual(b'actual dependency licence', z.read('vcpkg/zstd/copyright'))
                self.assertEqual(b'actual patched source', z.read('source/CMakeLists.txt'))

    def test_missing_built_dependency_licences_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            work = Path(folder)
            (work / 'source/dependencies').mkdir(parents=True)
            (work / 'source/LICENSE.txt').write_text('upstream licence')
            with self.assertRaisesRegex(ValueError, 'dependency licences missing'):
                collect(work)
