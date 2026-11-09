import unittest

from media_probe import probe_disc, GAMECUBE_MAGIC, WII_MAGIC


def image_with_magic(magic: bytes, offset: int) -> bytes:
    raw = bytearray(64)
    raw[offset: offset + len(magic)] = magic
    return bytes(raw)


class NextGenerationMediaProbeTests(unittest.TestCase):
    def test_gamecube_iso_detected_by_real_disc_magic(self):
        image = image_with_magic(GAMECUBE_MAGIC, 0x1C)
        for filename in ("game.iso", "game.gcm", "Game.GCM"):
            with self.subTest(filename=filename):
                result = probe_disc(filename, image)
                self.assertEqual(result.system, "GAMECUBE")
                self.assertEqual(result.confidence, "signature")

    def test_wii_magic_does_not_get_mislabeled_gamecube(self):
        image = image_with_magic(WII_MAGIC, 0x18)
        result = probe_disc("game.iso", image)
        self.assertEqual(result.system, "WII")

    def test_ps2_iso_not_guessed_from_iso_extension(self):
        result = probe_disc("a_ps2_game.iso", bytes(64))
        self.assertIsNone(result.system)

    def test_arbitrary_gcm_name_does_not_prove_gamecube(self):
        result = probe_disc("fake.gcm", b"not actually a game")
        self.assertIsNone(result.system)

    def test_shared_compressed_formats_not_claimed_by_any_system(self):
        for ext in ("rvz", "chd", "cso", "zso", "m3u", "cue", "bin", "img", "nrg", "mdf"):
            with self.subTest(ext=ext):
                self.assertIsNone(probe_disc(f"item.{ext}", GAMECUBE_MAGIC + bytes(64)).system)

    def test_gba_and_generic_elf_untouched(self):
        self.assertIsNone(probe_disc("EmeraldRogue.gba", bytes(64)).system)
        self.assertIsNone(probe_disc("homebrew.elf", bytes(64)).system)

    def test_gamecube_magic_wrong_offset_is_not_enough(self):
        image = image_with_magic(GAMECUBE_MAGIC, 0x18)
        self.assertIsNone(probe_disc("file.iso", image).system)

    def test_short_header_is_not_confident(self):
        self.assertIsNone(probe_disc("file.iso", GAMECUBE_MAGIC).system)

    def test_path_and_case_does_not_force_system(self):
        self.assertIsNone(probe_disc(r"PS2\\My Game.ISO", bytes(64)).system)


if __name__ == "__main__":
    unittest.main()
