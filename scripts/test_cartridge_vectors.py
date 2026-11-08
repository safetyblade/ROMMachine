"""Native resource contract checks; not device or visual acceptance."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / 'lemuroid-app/src/main/res/drawable'
A = '{http://schemas.android.com/apk/res/android}'


class CartridgeVectorTests(unittest.TestCase):
    def vector(self, edition, name):
        return ET.parse(DRAWABLE / f'cartdeck_{edition}_{name}.xml').getroot()

    def test_editions_share_all_geometry(self):
        for path in DRAWABLE.glob('cartdeck_original_*.xml'):
            pink = self.vector('pink', path.stem.removeprefix('cartdeck_original_'))
            original = ET.parse(path).getroot()
            for attr in ('viewportWidth', 'viewportHeight', 'width', 'height'):
                self.assertEqual(original.get(A + attr), pink.get(A + attr))
            self.assertEqual(
                [node.get(A + 'pathData') for node in original],
                [node.get(A + 'pathData') for node in pink],
            )

    def test_front_and_spine_keep_canonical_silhouettes(self):
        for name in ('front_base', 'spine_base'):
            source = ROOT / f'design/cartdeck-v05/cartridge/{name.replace("_", "-")}.svg'
            svg = ET.parse(source).getroot()
            # SVG may carry <title> and <desc> metadata ahead of its geometry.
            # Compare the first actual path, not a brittle child index.
            svg_ns = '{http://www.w3.org/2000/svg}'
            outer = next((node for node in svg if node.tag == svg_ns + 'path'), None)
            self.assertIsNotNone(outer, f'Missing outer silhouette in {source}')
            self.assertEqual(outer.get('d'), self.vector('original', name)[0].get(A + 'pathData'))

    def test_original_fallback_is_native_vector_and_edition_tinted(self):
        original = self.vector('original', 'fallback_atlas')
        pink = self.vector('pink', 'fallback_atlas')
        self.assertEqual('240', original.get(A + 'viewportWidth'))
        self.assertEqual('200', original.get(A + 'viewportHeight'))
        self.assertEqual(6, len(original))
        self.assertNotEqual(original[0].get(A + 'fillColor'), pink[0].get(A + 'fillColor'))

    def test_front_art_window_and_outline_share_bounds(self):
        overlay = self.vector('original', 'front_overlay')
        self.assertIn('M48,113H272', overlay[2].get(A + 'pathData'))
        self.assertEqual('320', overlay.get(A + 'viewportWidth'))
        self.assertEqual('390', overlay.get(A + 'viewportHeight'))


if __name__ == '__main__':
    unittest.main()
