"""S5: durable, fail-closed persistence fix for the PINNED hakuX pipeline cache.

The upstream pinned build already has per-title disk caches, async writes,
SPIR-V reuse and background prebuild. This bounded source correction only
changes cache write accounting: do not report a snapshot as saved before
g_file_set_contents succeeds. A failed write remains eligible for retry.
"""
from pathlib import Path

REL = Path("hw/xbox/hle/vk/pcache.c")

WRITE_OLD = """    if (buf) {
        const int64_t t2 = g_get_monotonic_time();
        g_file_set_contents(g_pc.blob, buf, n, NULL);   /* temp file + rename */
        hle_log("hakuX-hle-pipe", "pipeline cache saved: %zu bytes (merged in %.1f ms, "
                "serialized in %.1f ms, written in %.1f ms), spirv hits=%llu misses=%llu", n,
                (t1 - t0) / 1000.0, (t2 - t1) / 1000.0, (g_get_monotonic_time() - t2) / 1000.0,
                (unsigned long long)g_pc.spv_hits, (unsigned long long)g_pc.spv_misses);
    }"""

WRITE_NEW = """    if (buf) {
        const int64_t t2 = g_get_monotonic_time();
        GError *error = NULL;
        /* GLib writes via a temporary file + rename. Only mark the snapshot
         * persisted after the replacement has actually succeeded. If it
         * fails (storage full, permissions, interruption), preserve dirty
         * state for the next timed save or the final shutdown flush. */
        if (g_file_set_contents(g_pc.blob, (const gchar *)buf, n, &error)) {
            hle_atomic_set(&g_pc.saved, snapshot);
            hle_log("hakuX-hle-pipe", "pipeline cache saved: %zu bytes (merged in %.1f ms, "
                    "serialized in %.1f ms, written in %.1f ms), spirv hits=%llu misses=%llu", n,
                    (t1 - t0) / 1000.0, (t2 - t1) / 1000.0,
                    (g_get_monotonic_time() - t2) / 1000.0,
                    (unsigned long long)g_pc.spv_hits, (unsigned long long)g_pc.spv_misses);
        } else {
            hle_log("hakuX-hle-pipe", "pipeline cache write failed: %s",
                    error ? error->message : "unknown error");
        }
        g_clear_error(&error);
    }"""

# Strict exact-pinned-source anchors, not best-effort edits on arbitrary forks.
REPLACEMENTS = (
    (
        "cache write snapshot",
        "static void write_blob(void)\n{",
        "static void write_blob(void)\n{\n"
        "    /* Other threads may compile more shaders while we serialize. */\n"
        "    const uint64_t snapshot = hle_atomic_read(&g_pc.added);",
    ),
    ("write completion before marking persisted", WRITE_OLD, WRITE_NEW),
    (
        "atomic added-vs-saved check",
        "    if (!g_pc.cache || g_pc.added == g_pc.saved ||",
        "    if (!g_pc.cache || hle_atomic_read(&g_pc.added) == "
        "hle_atomic_read(&g_pc.saved) ||",
    ),
    (
        "premature save acknowledgement",
        "    g_pc.saved = g_pc.added;\n",
        "",
    ),
)


def patch(source: str) -> str:
    for name, old, new in REPLACEMENTS:
        if source.count(old) != 1:
            raise ValueError("Pinned hakuX cache anchor mismatch: " + name)
        source = source.replace(old, new, 1)
    return source


def apply(source_root: Path) -> None:
    path = Path(source_root) / REL
    original = path.read_text()
    revised = patch(original)
    # No partially written native source when the source pin drifts.
    path.write_text(revised)
