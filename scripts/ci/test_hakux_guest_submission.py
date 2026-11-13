"""Run the shipped FIFO wakeup and bounded guest-progress code as native C."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

PATCHES = pathlib.Path(__file__).parent / 'patches'
PATCH = PATCHES / 'hakux-guest-submission-progress.patch'


def after_lines(path):
    section = PATCH.read_text().split('diff --git a/' + path + ' b/' + path + '\n')[1].split('\ndiff --git ')[0]
    return '\n'.join(line[1:] for line in section.splitlines()
                     if line.startswith(('+', ' ')) and not line.startswith('+++'))


def function(source, name):
    start = source.index(name + '(')
    start = source.rfind('\n', 0, start) + 1
    brace = source.index('{', start)
    depth = 1
    end = brace + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]


@unittest.skipUnless(shutil.which('cc') and shutil.which('git'), 'C compiler/git required')
class GuestSubmissionTest(unittest.TestCase):
    def compile_run(self, body):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            subprocess.run(['git', 'init', '-q', str(root)], check=True)
            # Exercise the final header, including its earlier merged version.
            for name in ('hakux-guest-publication-recovery.patch', PATCH.name):
                subprocess.run(['git', 'apply', '--include=hw/xbox/hle/vk/cartdeck_stage_probe.h',
                                str((PATCHES / name).resolve())], cwd=root, check=True)
            (root / 'test.c').write_text(body)
            exe = root / 'test'
            subprocess.run(['cc', '-std=c11', '-Wall', '-Wextra', '-Werror', '-I', str(root),
                            str(root / 'test.c'), '-o', str(exe)], check=True)
            subprocess.run([str(exe)], check=True)

    def test_fifo_publication_precedes_decoder_wake_for_both_aliases(self):
        nv2a = after_lines('native/dev/nv2a.c')
        helper = function(nv2a, 'fifo_dma_write')
        self.assertIn('fifo_dma_write(off, (uint32_t)value);\n        return;', nv2a)
        self.assertIn('fifo_dma_write(off - NV_USER_DMA_PUT + NV_PFIFO_CACHE1_DMA_PUT, (uint32_t)value);', nv2a)
        self.compile_run(r'''
#include <assert.h>
#include <stdbool.h>
#include <string.h>
#include "hw/xbox/hle/vk/cartdeck_stage_probe.h"
CartdeckStages g_cartdeck_stages;
#define NV_PFIFO_CACHE1_DMA_PUT 0x3240u
#define NV_PFIFO_CACHE1_DMA_GET 0x3244u
#define NV_USER_DMA_PUT 0x800040u
#define NV_USER_DMA_GET 0x800044u
#define NV_USER_REF 0x800048u
static uint32_t regs[3];
static unsigned scouts, wakes, resets, sequence, expected;
static uint32_t *reg(uint32_t off) { return &regs[(off - NV_PFIFO_CACHE1_DMA_PUT) / 4]; }
static void hle_pb_pushed(uint32_t value) {
    assert(value == expected); assert(regs[0] != value);
    assert(sequence == 0); sequence = 1; scouts++;
}
static void hle_pb_put(void) {
    assert(sequence == 1); assert(regs[0] == expected); sequence = 2; wakes++;
    /* A sleeping consumer wakes and can see all the newly published work. */
    regs[1] = regs[0];
}
static void hle_pb_get(void) { assert(regs[1] == expected); resets++; }
''' + helper + r'''
int main(void) {
    g_cartdeck_stages.budget = 100;
    expected = 8;
    fifo_dma_write(NV_PFIFO_CACHE1_DMA_PUT, expected);
    assert(scouts == 1 && wakes == 1 && regs[1] == 8);
    sequence = 0; expected = 16;
    fifo_dma_write(NV_USER_DMA_PUT - NV_USER_DMA_PUT + NV_PFIFO_CACHE1_DMA_PUT, expected);
    assert(scouts == 2 && wakes == 2 && regs[1] == 16);
    expected = 4;
    fifo_dma_write(NV_PFIFO_CACHE1_DMA_GET, expected);
    assert(resets == 1 && regs[1] == 4);
    expected = 12;
    fifo_dma_write(NV_USER_DMA_GET - NV_USER_DMA_PUT + NV_PFIFO_CACHE1_DMA_PUT, expected);
    assert(resets == 2 && regs[1] == 12);
    fifo_dma_write(NV_USER_REF - NV_USER_DMA_PUT + NV_PFIFO_CACHE1_DMA_PUT, 7);
    assert(regs[2] == 7 && wakes == 2 && resets == 2 && scouts == 2);
    assert(g_cartdeck_stages.count[CD_FIFO_PUT] == 2);
    assert(g_cartdeck_stages.count[CD_FIFO_GET] == 2);
    return 0;
}
''')

    def test_callback_only_progress_and_guest_waits_are_distinguishable(self):
        kernel = after_lines('native/kernel/kernel.c')
        implementation = '\n'.join(function(kernel, name) for name in (
            'cartdeck_kernel_sample', 'cartdeck_ordinal_name', 'nc_cartdeck_kernel_report'))
        self.compile_run(r'''
#include <assert.h>
#include <stdbool.h>
#include <string.h>
#include "hw/xbox/hle/vk/cartdeck_stage_probe.h"
CartdeckStages g_cartdeck_stages;
#define NC_KORDINAL_DPC_RETURN 0x3fe
#define NC_KORDINAL_ISR_RETURN 0x3fc
#define NC_KORDINAL_APC_RETURN 0x3fb
#define NC_KORDINAL_CALLOUT_RETURN 0x3fd
#define NC_KORDINAL_THREAD_RETURN 0x3ff
#define NC_KORDINAL_APU_SPIN 0x3f5
#define NC_KORDINAL_TRAP 0x3fa
#define NC_KORDINAL_SEH_RETURN 0x3f9
#define NC_KORDINAL_SEH_NESTED 0x3f8
#define NC_KORDINAL_SEH_COLLIDED 0x3f7
#define NC_MAX_THREADS 128
#define RECENT 16
enum { NC_T_FREE, NC_T_READY, NC_T_RUNNING, NC_T_WAITING, NC_T_SUSPENDED, NC_T_TERMINATED };
typedef struct { unsigned count, objects[64]; uint64_t deadline_ns; } Wait;
typedef struct { unsigned id, state; Wait wait; } NcThread;
typedef struct { unsigned threads_hi; NcThread threads[128]; NcThread *current; } NcKernel;
typedef struct { const char *name; } NcKExport;
static const NcKExport export_wait = {"KeWaitForSingleObject"};
static const NcKExport *nc_kernel_export(unsigned ordinal) { return ordinal == 159 ? &export_wait : NULL; }
static struct { unsigned ordinal, thread, ret, result, running; } g_recent[RECENT];
static unsigned g_nrecent;
''' + implementation + r'''
int main(void) {
    NcKernel k = {0}; k.threads_hi = 3;
    k.threads[0].id = 1; k.threads[0].state = NC_T_WAITING;
    k.threads[0].wait.count = 1; k.threads[0].wait.objects[0] = 0x1234;
    k.threads[1].id = 2; k.threads[1].state = NC_T_RUNNING;
    k.threads[2].id = 3; k.threads[2].state = NC_T_SUSPENDED;
    k.current = &k.threads[1];
    g_nrecent = 1; g_recent[0].ordinal = 159; g_recent[0].thread = 1;
    g_recent[0].ret = 0xabc; g_recent[0].result = 0x103; g_recent[0].running = 1;
    cartdeck_kernel_sample(&k, 159);
    assert(g_cartdeck_stages.ordinal[159] == 0); /* disabled */
    g_cartdeck_stages.budget = 1000;
    for (unsigned n = 0; n < 248; n++) {
        cartdeck_stage_hit(CD_KERNEL_CALL);
        cartdeck_kernel_sample(&k, NC_KORDINAL_DPC_RETURN);
    }
    char out[1024]; nc_cartdeck_kernel_report(out, sizeof(out));
    assert(strstr(out, "DPC_RETURN(1022):248"));
    assert(strstr(out, "snapshot_consistent=1"));
    assert(!strstr(out, "KeWaitForSingleObject(159):")); /* not a new export */
    assert(strstr(out, "waiting=1 suspended=1"));
    assert(strstr(out, "wait0=t1 objects=1 first=00001234 timed=0"));
    assert(strstr(out, "t1:KeWaitForSingleObject@00000abc=running")); /* existing ring reused */
    cartdeck_stage_hit(CD_KERNEL_CALL); cartdeck_kernel_sample(&k, 159);
    nc_cartdeck_kernel_report(out, sizeof(out));
    assert(strstr(out, "KeWaitForSingleObject(159):1"));
    g_cartdeck_stages.budget = 0; cartdeck_kernel_sample(&k, 159);
    assert(g_cartdeck_stages.ordinal[159] == 1); /* remains bounded */
    g_cartdeck_stages.snapshot_seq |= 1;
    nc_cartdeck_kernel_report(out, sizeof(out));
    assert(strstr(out, "snapshot_consistent=0"));
    char tiny[9]; memset(tiny, 'X', sizeof(tiny));
    nc_cartdeck_kernel_report(tiny, 8);
    assert(tiny[7] == 0 && tiny[8] == 'X');
    return 0;
}
''')

    def test_existing_probe_includes_fifo_and_stall_evidence(self):
        patch = PATCH.read_text()
        for marker in ('CD_DECODE_PASS', 'CD_DECODE_TRAP', 'CD_DECODE_CLOSED',
                       'CD_DECODE_FLIP', 'CD_DECODE_NO_DMA', 'nc_cartdeck_kernel_report(kernel,',
                       'nc_cartdeck_fifo_report(fifo,', 'pixels[8192], report[9216]'):
            self.assertIn(marker, patch)
        build = (PATCHES.parent / 'build_hakux_native.py').read_text()
        self.assertIn(PATCH.name, build)
        self.assertIn('fifo-put-wakeup-v1', build)
