"""Validate actual resource packaging and reject modified/placeholder source archives."""
import hashlib
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

from package_ps2_runtime_support import append_android_patches


class AndroidPatchesTest(unittest.TestCase):
    def test_pinned_archive_is_packaged_byte_for_byte_and_mutations_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            relative = 'platforms/android/app/src/main/assets/resources/patches.zip'
            asset = root / relative
            asset.parent.mkdir(parents=True)
            with zipfile.ZipFile(asset, 'w') as archive:
                archive.writestr('HOME-TEST.pnach', '// lawful packaging fixture\n')
            for args in (['init', '-q'], ['add', relative],
                         ['-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'fixture']):
                subprocess.run(['git', '-C', str(root)] + args, check=True)
            revision = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
            destination = root / 'resources.zip'
            with zipfile.ZipFile(destination, 'w') as archive:
                archive.writestr('GameIndex.yaml', '# source fixture\n')
            receipt = append_android_patches(root, destination, revision)
            with zipfile.ZipFile(destination) as archive:
                self.assertEqual(asset.read_bytes(), archive.read('patches.zip'))
            self.assertEqual(hashlib.sha256(asset.read_bytes()).hexdigest(), receipt['sha256'])
            with self.assertRaisesRegex(ValueError, 'Conflicting'):
                append_android_patches(root, destination, revision)
            asset.write_bytes(b'not the committed archive')
            with self.assertRaisesRegex(ValueError, 'pinned source'):
                append_android_patches(root, destination, revision)


if __name__ == '__main__':
    unittest.main()
