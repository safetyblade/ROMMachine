import pathlib
import shutil
import subprocess
import tempfile
import unittest

from native_symbols import build_id, retain, symbolize


@unittest.skipUnless(shutil.which('cc') and shutil.which('readelf'), 'ELF tools required')
class NativeSymbolsTest(unittest.TestCase):
    def setUp(self):
        self.work = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.work.name)
        source = self.root / 'fixture.c'
        source.write_text('int probe_init(void) { return 42; }\n')
        self.binary = self.root / 'libfixture.so'
        subprocess.run(['cc', '-g', '-shared', '-Wl,--build-id', str(source), '-o', str(self.binary)], check=True)
        self.packaged = self.root / 'packaged' / self.binary.name
        self.packaged.parent.mkdir()
        subprocess.run(['strip', '--strip-unneeded', '-o', str(self.packaged), str(self.binary)], check=True)

    def tearDown(self):
        self.work.cleanup()

    def test_retains_only_exact_unstripped_build_and_fingerprints_package(self):
        record = retain(self.binary, self.packaged, self.root / 'symbols', 'readelf')
        self.assertEqual(build_id(self.packaged, 'readelf'), record['build_id'])
        self.assertEqual(self.binary.read_bytes(), (self.root / 'symbols/libfixture.so.debug').read_bytes())
        self.assertEqual(64, len(record['packaged_sha256']))
        with self.assertRaisesRegex(ValueError, 'DWARF'):
            retain(self.packaged, self.packaged, self.root / 'bad', 'readelf')

    def test_wrong_build_is_rejected_before_symbols_are_published(self):
        other = self.root / 'other.so'
        subprocess.run(['cc', '-shared', '-Wl,--build-id', '-x', 'c', '-', '-o', str(other)],
                       input=b'int other_function(void) { return 5; }', check=True)
        with self.assertRaisesRegex(ValueError, 'different ELF build ID'):
            retain(other, self.packaged, self.root / 'symbols', 'readelf')
        self.assertFalse((self.root / 'symbols').exists())

    def test_symbolication_refuses_cross_build_guessing_and_absolute_paths(self):
        directory = self.root / 'symbols'
        retain(self.binary, self.packaged, directory, 'readelf')
        report = '#00 pc 1234 libfixture.so (unknown+0) (BuildId: 1234abcdef)'
        with self.assertRaisesRegex(ValueError, 'mismatched'):
            symbolize(report, directory, 'readelf', '/not/executed')
        with self.assertRaisesRegex(ValueError, 'No module-relative frames'):
            symbolize('#00 pc 1234 /private/libfixture.so (BuildId: 1234abcdef)', directory, 'readelf', '/not/executed')
