import struct
import unittest
from verify_gamecube_core import verify


def elf(alignment=16384, machine=183):
    data = bytearray(120)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<H', data, 16, 3)
    struct.pack_into('<H', data, 18, machine)
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 1)
    struct.pack_into('<I', data, 64, 1)
    struct.pack_into('<Q', data, 112, alignment)
    return data


SYMBOLS = '\n'.join(f'1: 0001 2 FUNC GLOBAL DEFAULT 7 {name}' for name in (
    'retro_init', 'retro_deinit', 'retro_load_game', 'retro_run',
    'retro_set_environment', 'retro_get_system_av_info',
    'retro_serialize', 'retro_unserialize'))


class VerifyGameCubeCore(unittest.TestCase):
    def test_android_aligned_core(self):
        verify(elf(), 'NEEDED libc.so', SYMBOLS)

    def test_reject_executable(self):
        data = elf()
        struct.pack_into('<H', data, 16, 2)
        with self.assertRaisesRegex(ValueError, 'ET_DYN'):
            verify(data, '', SYMBOLS)

    def test_reject_linux_dependencies(self):
        with self.assertRaisesRegex(ValueError, 'Linux'):
            verify(elf(), 'NEEDED libc.so.6', SYMBOLS)

    def test_reject_page_alignment(self):
        with self.assertRaisesRegex(ValueError, '16 KiB'):
            verify(elf(4096), '', SYMBOLS)

    def test_reject_wrong_architecture(self):
        with self.assertRaisesRegex(ValueError, 'AArch64'):
            verify(elf(machine=62), '', SYMBOLS)

    def test_reject_missing_or_undefined_exports(self):
        with self.assertRaisesRegex(ValueError, 'retro_run'):
            verify(elf(), '', SYMBOLS.replace('DEFAULT 7 retro_run', 'DEFAULT UND retro_run'))

    def test_reject_truncated_program_headers(self):
        with self.assertRaisesRegex(ValueError, 'program headers'):
            verify(elf()[:80], '', SYMBOLS)


if __name__ == '__main__':
    unittest.main()
