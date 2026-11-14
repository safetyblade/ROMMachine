import tempfile
import unittest
from pathlib import Path
from merge_verified_native_payloads import merge


class NativePayloadMergeTests(unittest.TestCase):
    def test_retains_new_engine_and_preserves_identical_existing_file(self):
        with tempfile.TemporaryDirectory() as root:
            source, target = Path(root) / 'source', Path(root) / 'target'
            for directory in ('assets/cartdeck', 'jniLibs/arm64-v8a'):
                (source / directory).mkdir(parents=True)
                (target / directory).mkdir(parents=True)
            (source / 'jniLibs/arm64-v8a/libplay.so').write_bytes(b'old engine')
            (target / 'jniLibs/arm64-v8a/libarmsx2.so').write_bytes(b'new engine')
            (source / 'assets/cartdeck/provenance').write_bytes(b'pin')
            self.assertEqual(2, merge(source, target))
            self.assertEqual(2, merge(source, target))
            self.assertEqual(b'new engine', (target / 'jniLibs/arm64-v8a/libarmsx2.so').read_bytes())
            self.assertEqual(b'old engine', (target / 'jniLibs/arm64-v8a/libplay.so').read_bytes())

    def test_collision_fails_before_copying_any_file(self):
        with tempfile.TemporaryDirectory() as root:
            source, target = Path(root) / 'source', Path(root) / 'target'
            for directory in ('assets', 'jniLibs/arm64-v8a'):
                (source / directory).mkdir(parents=True)
                (target / directory).mkdir(parents=True)
            (source / 'assets/new').write_bytes(b'must not copy')
            (source / 'jniLibs/arm64-v8a/libsame.so').write_bytes(b'one')
            (target / 'jniLibs/arm64-v8a/libsame.so').write_bytes(b'two')
            with self.assertRaisesRegex(ValueError, 'collide'):
                merge(source, target)
            self.assertFalse((target / 'assets/new').exists())
            self.assertEqual(b'two', (target / 'jniLibs/arm64-v8a/libsame.so').read_bytes())
