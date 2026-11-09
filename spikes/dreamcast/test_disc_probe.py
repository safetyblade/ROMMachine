import unittest

from disc_probe import classify_disc_filename, parse_gdi


GOOD_GDI = """3
1 0 4 2352 track01.bin 0
2 450 0 2352 track02.raw 0
3 45150 4 2048 "track 03.bin" 0
"""


class DreamcastDiscProbeTest(unittest.TestCase):
    def test_gdi_is_uniquely_discoverable(self):
        result = classify_disc_filename("My Games/Dreamcast/Example.GDI")
        self.assertEqual(result.kind, "dreamcast-gdi")
        self.assertTrue(result.safe_for_auto_detection)

    def test_shared_disc_extensions_do_not_auto_claim_dreamcast(self):
        for extension in ("chd", "iso", "cue", "bin", "m3u"):
            with self.subTest(extension=extension):
                self.assertEqual(
                    classify_disc_filename(f"Library/Dreamcast/example.{extension}").kind,
                    "ambiguous",
                )
                self.assertFalse(
                    classify_disc_filename(f"Library/Dreamcast/example.{extension}").safe_for_auto_detection
                )

    def test_cdi_is_only_a_candidate(self):
        result = classify_disc_filename("games/example.cdi")
        self.assertEqual(result.kind, "candidate-cdi")
        self.assertFalse(result.safe_for_auto_detection)

    def test_unrelated_files_not_discovered(self):
        self.assertEqual(classify_disc_filename("test.gba").kind, "unrecognised")

    def test_gdi_track_parser_validates_count_and_companions(self):
        tracks = parse_gdi(GOOD_GDI)
        self.assertEqual(len(tracks), 3)
        self.assertEqual(tracks[2].filename, "track 03.bin")
        self.assertEqual(tracks[2].lba, 45150)

    def test_gdi_rejects_missing_track(self):
        with self.assertRaises(ValueError):
            parse_gdi(GOOD_GDI.replace("3\n", "4\n", 1))

    def test_gdi_rejects_directory_escape_and_absolute_paths(self):
        for path in ("../secret.bin", "/tmp/secret.bin", "C:/secret.bin"):
            with self.subTest(path=path), self.assertRaises(ValueError):
                parse_gdi(f"1\n1 0 4 2352 {path} 0")

    def test_gdi_rejects_track_gaps_and_invalid_sector_size(self):
        for descriptor in (
            "1\n2 0 4 2352 track02.bin 0",
            "1\n1 0 4 512 track01.bin 0",
        ):
            with self.subTest(descriptor=descriptor), self.assertRaises(ValueError):
                parse_gdi(descriptor)

    def test_gdi_rejects_invalid_quoting(self):
        with self.assertRaises(ValueError):
            parse_gdi('1\n1 0 4 2352 "unclosed.bin 0')


if __name__ == "__main__":
    unittest.main()
