"""Regression: GEM source parser must survive Kotlin formatter line wrapping."""
import unittest

from validate_taxonomy import _kotlin_string_set


class KotlinSourceSetParserTest(unittest.TestCase):
    def test_multiline_ktlint_layout(self):
        src = '''
            private val nonGame =
                setOf(
                    "application",
                    "firmware",
                )
        '''
        self.assertEqual(_kotlin_string_set(src, "nonGame"), {"application", "firmware"})

    def test_single_line_layout(self):
        self.assertEqual(_kotlin_string_set('private val ambiguous = setOf("misc", "unknown")', "ambiguous"), {"misc", "unknown"})

    def test_missing_set_fails_closed(self):
        with self.assertRaisesRegex(AssertionError, "Missing"):
            _kotlin_string_set("private val somethingElse = setOf(\"misc\")", "nonGame")

    def test_expression_not_silently_accepted(self):
        with self.assertRaisesRegex(AssertionError, "Unsupported"):
            _kotlin_string_set('private val nonGame = setOf("firmware", extraTag)', "nonGame")


if __name__ == "__main__":
    unittest.main()
