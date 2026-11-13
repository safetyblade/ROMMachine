"""Compile the changed native guard and bounded stage probe from the shipped patch."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

PATCH = pathlib.Path(__file__).parent / 'patches/hakux-guest-publication-recovery.patch'


def patched_lines(path):
    section = PATCH.read_text().split('diff --git a/' + path + ' b/' + path + '\n')[1].split('\ndiff --git ')[0]
    return '\n'.join(line[1:] for line in section.splitlines()
                     if line.startswith(('+', ' ')) and not line.startswith('+++'))


@unittest.skipUnless(shutil.which('cc'), 'C compiler required')
class GuestPublicationTest(unittest.TestCase):
    def test_invalid_sources_never_reserve_or_publish_a_display_slot(self):
        back = patched_lines('hw/xbox/hle/vk/back.c')
        # Execute the actual changed guard, not a Python copy of its conditions.
        guard = back[back.index('    HleVkSurf *src ='):back.index('        VkCommandBuffer cb =')]
        header = patched_lines('hw/xbox/hle/vk/cartdeck_stage_probe.h')
        source = r'''
#include <assert.h>
#include <stdbool.h>
#include <string.h>
#include "cartdeck_stage_probe.h"
CartdeckStages g_cartdeck_stages;
#define VK_IMAGE_ASPECT_COLOR_BIT 1
struct { struct { unsigned swaps; } st; } g_back;
typedef struct { unsigned aspect, w, h; } HleVkSurf;
typedef struct { unsigned surf, swap_id; uint64_t value; } HleCmdSwap;
static HleVkSurf surface;
static unsigned slots, submits, ensured;
static bool available, ensure_ok;
static HleVkSurf *hle_surf_get(unsigned id) { (void)id; return available ? &surface : NULL; }
static bool hle_surf_ensure(HleVkSurf *s) { (void)s; ensured++; return ensure_ok; }
static int hle_disp_begin_write(unsigned id, uint64_t v) { (void)id; (void)v; slots++; return 0; }
static void submit(uint64_t value) { assert(value == 19); submits++; }
static void run(const HleCmdSwap *c) {
''' + guard + r'''
    }
}
int main(void) {
    HleCmdSwap c = {1, 8, 19};
    g_cartdeck_stages.budget = 100;
    surface = (HleVkSurf){1, 640, 480};
    ensure_ok = true;
    run(&c); assert(slots == 0 && submits == 1 && ensured == 0);
    available = true; ensure_ok = false;
    run(&c); assert(slots == 0 && submits == 2);
    ensure_ok = true; surface.aspect = 2;
    run(&c); assert(slots == 0 && submits == 3);
    surface.aspect = 1; surface.w = 0;
    run(&c); assert(slots == 0 && submits == 4);
    surface.w = 640; surface.h = 0;
    run(&c); assert(slots == 0 && submits == 5);
    surface.h = 480;
    run(&c); assert(slots == 1 && submits == 5);
    assert(g_back.st.swaps == 5);
    assert(g_cartdeck_stages.count[CD_SWAP_REJECT] == 5);
    assert(g_cartdeck_stages.count[CD_DISPLAY_WRITE] == 1);
    memset(&g_cartdeck_stages, 0, sizeof(g_cartdeck_stages));
    cartdeck_stage_hit(CD_KERNEL_CALL);
    assert(g_cartdeck_stages.count[CD_KERNEL_CALL] == 0); /* default disabled */
    g_cartdeck_stages.budget = 3;
    for (unsigned i = 0; i < 100; i++) cartdeck_stage_hit(CD_VK_DRAW);
    assert(g_cartdeck_stages.count[CD_VK_DRAW] == 3);
    assert(g_cartdeck_stages.budget == 0);
    char out[512]; cartdeck_stage_report(out, sizeof(out));
    assert(strstr(out, "kernel_calls=0") && strstr(out, "vk_draw=3"));
    return 0;
}
'''
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / 'cartdeck_stage_probe.h').write_text(header)
            (root / 'test.c').write_text(source)
            exe = root / 'test'
            subprocess.run(['cc', '-std=c11', '-Wall', '-Wextra', '-Werror', '-I', str(root),
                            str(root / 'test.c'), '-o', str(exe)], check=True)
            subprocess.run([str(exe)], check=True)

    def test_stage_hooks_and_guest_identity_are_shipped(self):
        patch = PATCH.read_text()
        for stage in ('CD_KERNEL_CALL', 'CD_FRONT_COMMAND', 'CD_FRONT_DRAW', 'CD_BACK_COMMAND',
                      'CD_BACK_DRAW', 'CD_VK_DRAW', 'CD_PB_METHOD', 'CD_SWAP_REJECT', 'CD_DISPLAY_WRITE'):
            self.assertIn('cartdeck_stage_hit(' + stage + ')', patch)
        self.assertIn('f->probe_swap_id = g_ps.f.swap_id;', patch)
        self.assertIn('stages_at_collect=', patch)
        self.assertIn('g_probe_done == 3', patch)
        build = (PATCH.parent.parent / 'build_hakux_native.py').read_text()
        self.assertIn(PATCH.name, build)
        self.assertIn('validated-source-v1', build)
