"""Run bounded native percentile/concurrency and report semantics, not device FPS."""
import pathlib
import subprocess
import tempfile
import unittest


class X360Performance(unittest.TestCase):
    def test_native_measurements_are_bounded_thread_safe_and_not_image_claims(self):
        source = pathlib.Path(__file__).resolve().parent / 'xbox360'
        with tempfile.TemporaryDirectory() as directory:
            exe = pathlib.Path(directory) / 'test'
            subprocess.run(['c++', '-std=c++17', '-O2', '-pthread', '-Wall', '-Wextra', '-Werror',
                            '-I' + str(source), str(source.parent / 'tests/X360PerformanceTest.cpp'), '-o', str(exe)], check=True)
            subprocess.run([str(exe)], check=True, timeout=30)
