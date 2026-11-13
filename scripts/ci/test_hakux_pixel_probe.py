"""Exercise the exact C summary helper shipped by the pinned engine patch."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest


@unittest.skipUnless(shutil.which('cc') and shutil.which('git'), 'C compiler and git required')
class PixelProbeTest(unittest.TestCase):
    def test_native_summary_distinguishes_blank_color_and_alpha_without_images(self):
        patch = pathlib.Path(__file__).parent / 'patches/hakux-pixel-stage-probe.patch'
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            subprocess.run(['git', 'apply', '--include=hw/xbox/hle/vk/cartdeck_pixel_probe.h', str(patch.resolve())],
                           cwd=root, check=True)
            source = root / 'fixture.c'
            source.write_text('''#include <string.h>
#include "hw/xbox/hle/vk/cartdeck_pixel_probe.h"
int main(void) {
    uint8_t pixels[64*4]; char out[256];
    memset(pixels, 255, sizeof(pixels));
    cartdeck_pixel_summary(pixels, 64, out, sizeof(out)); puts(out);
    for (unsigned i=0; i<64; i++) { pixels[i*4]=pixels[i*4+1]=pixels[i*4+2]=0; }
    cartdeck_pixel_summary(pixels, 64, out, sizeof(out)); puts(out);
    for (unsigned i=0; i<64; i++) { pixels[i*4+3]=0; }
    cartdeck_pixel_summary(pixels, 64, out, sizeof(out)); puts(out);
    return 0;
}''')
            exe = root / 'probe'
            subprocess.run(['cc', '-std=c11', '-Wall', '-Wextra', '-Werror', '-I', str(root), str(source), '-o', str(exe)],
                           check=True)
            white, black, transparent = subprocess.check_output([str(exe)], text=True).splitlines()
            self.assertIn('rgb=255..255 white=64 black=0 alpha=255..255', white)
            self.assertIn('rgb=0..0 white=0 black=64 alpha=255..255', black)
            self.assertIn('alpha=0..0', transparent)
            self.assertNotEqual(white.split('fnv=')[1], black.split('fnv=')[1])
