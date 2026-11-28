"""PC-R02 shipped-core audit regression tests (synthetic, no real core)."""
import hashlib
import importlib.util
import pathlib
import struct
import unittest

PROBE = pathlib.Path(__file__).resolve().parents[1] / "verify_pcr02.py"
spec = importlib.util.spec_from_file_location("pcr02_verify", PROBE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


def fake_elf(symbols=(), needed=(), alignment=16384, machine=183, vaddr_base=0):
    """Minimal ELF64 with one LOAD, one PT_DYNAMIC, dynsym/dynstr/hash."""
    names = list(symbols)
    strtab = bytearray(b"\x00")
    name_offsets = {}
    for name in names + list(needed):
        if name not in name_offsets:
            name_offsets[name] = len(strtab)
            strtab += name.encode() + b"\x00"
    nsyms = 1 + len(names)
    dynsym = bytearray(24)  # null symbol
    for name in names:
        dynsym += struct.pack(
            "<IBBHQQ", name_offsets[name], 0x12, 0, 1, 0x1000, 0
        )
    hashtab = struct.pack("<II", 1, nsyms) + bytes(4 * (1 + nsyms))
    dyn_entries = []

    header_size = 64 + 2 * 56
    strtab_off = header_size
    dynsym_off = strtab_off + len(strtab)
    hash_off = dynsym_off + len(dynsym)
    dynamic_off = hash_off + len(hashtab)
    n_dyn = 5 + len(needed) + 1  # HASH, STRTAB, SYMTAB, STRSZ, SYMENT, NEEDED*, NULL
    total = dynamic_off + 16 * n_dyn

    dyn_entries.append((4, vaddr_base + hash_off))       # DT_HASH
    dyn_entries.append((5, vaddr_base + strtab_off))     # DT_STRTAB
    dyn_entries.append((6, vaddr_base + dynsym_off))     # DT_SYMTAB
    dyn_entries.append((10, len(strtab)))                # DT_STRSZ
    dyn_entries.append((11, 24))                         # DT_SYMENT
    for lib in needed:
        dyn_entries.append((1, name_offsets[lib]))       # DT_NEEDED
    dyn_entries.append((0, 0))                           # DT_NULL
    dynamic = b"".join(struct.pack("<qQ", t, v) for t, v in dyn_entries)

    data = bytearray(total)
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", data, 16, 3)                  # ET_DYN
    struct.pack_into("<H", data, 18, machine)
    struct.pack_into("<Q", data, 32, 64)                 # e_phoff
    struct.pack_into("<H", data, 54, 56)                 # e_phentsize
    struct.pack_into("<H", data, 56, 2)                  # e_phnum
    struct.pack_into(
        "<IIQQQQQQ", data, 64,
        1, 5, 0, vaddr_base, vaddr_base, total, total, alignment,
    )
    struct.pack_into(
        "<IIQQQQQQ", data, 64 + 56,
        2, 4, dynamic_off, vaddr_base + dynamic_off,
        vaddr_base + dynamic_off, len(dynamic), len(dynamic), 8,
    )
    data[strtab_off:strtab_off + len(strtab)] = strtab
    data[dynsym_off:dynsym_off + len(dynsym)] = dynsym
    data[hash_off:hash_off + len(hashtab)] = hashtab
    data[dynamic_off:dynamic_off + len(dynamic)] = dynamic
    return bytes(data)


class Pcr02ElfTests(unittest.TestCase):
    def test_unrecognised_header(self):
        self.assertFalse(mod.elf_report(b"not an elf")["valid"])

    def test_full_host_symbol_set_passes(self):
        report = mod.elf_report(
            fake_elf(symbols=mod.HOST_REQUIRED_SYMBOLS, needed=["libc.so"])
        )
        self.assertTrue(report["valid"])
        self.assertTrue(report["all_load_segments_aligned_16kb"])
        self.assertEqual(report["missing_host_symbols"], [])
        self.assertEqual(report["unexpected_needed"], [])

    def test_missing_host_symbol_detected(self):
        symbols = [s for s in mod.HOST_REQUIRED_SYMBOLS if s != "retro_run"]
        report = mod.elf_report(fake_elf(symbols=symbols))
        self.assertEqual(report["missing_host_symbols"], ["retro_run"])

    def test_wrong_machine_rejected(self):
        report = mod.elf_report(fake_elf(machine=62))
        self.assertFalse(report["valid"])

    def test_old_4k_load_alignment_not_accepted(self):
        report = mod.elf_report(fake_elf(alignment=4096))
        self.assertFalse(report["all_load_segments_aligned_16kb"])

    def test_incongruent_load_rejected(self):
        report = mod.elf_report(fake_elf(vaddr_base=0x1000))
        self.assertFalse(report["all_load_segments_aligned_16kb"])

    def test_unexpected_needed_library_detected(self):
        report = mod.elf_report(
            fake_elf(
                symbols=mod.HOST_REQUIRED_SYMBOLS,
                needed=["libc.so", "libssl.so"],
            )
        )
        self.assertEqual(report["unexpected_needed"], ["libssl.so"])

    def test_git_blob_sha_format(self):
        payload = b"hello"
        expected = hashlib.sha1(b"blob 5\x00hello").hexdigest()
        self.assertEqual(mod.git_blob_sha(payload), expected)


if __name__ == "__main__":
    unittest.main()
