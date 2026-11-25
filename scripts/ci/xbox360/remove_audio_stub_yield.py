"""Port the small upstream XenDroid+ #123 audio-stub CPU fix to pinned Xenia.

Source: phforner0/Xendroid-Plus commit 654777bb8e6bb6712c8b9f922d7e2bc6eabb79df
(BSD-3-Clause Xenia core; ART/host shell not imported).
This entry point is a guest kernel volume-change flag query, not a scheduler
work queue. Yielding on every query wastes host scheduling without changing the
guest-visible result. Apply to the EXACT pinned source, fail closed on drift.
"""
from pathlib import Path

REL = Path("xenia/src/xenia/kernel/xboxkrnl/xboxkrnl_audio.cc")
OLD = """dword_result_t XAudioGetVoiceCategoryVolumeChangeMask_entry(
    lpunknown_t driver_ptr, lpdword_t out_ptr) {
  assert_true((driver_ptr.guest_address() & 0xFFFF0000) == 0x41550000);

  xe::threading::MaybeYield();

  // Checking these bits to see if any voice volume changed."""
NEW = """dword_result_t XAudioGetVoiceCategoryVolumeChangeMask_entry(
    lpunknown_t driver_ptr, lpdword_t out_ptr) {
  assert_true((driver_ptr.guest_address() & 0xFFFF0000) == 0x41550000);

  // Checking these bits to see if any voice volume changed."""


def patch(text: str) -> str:
    if text.count(OLD) != 1:
        raise ValueError("Pinned XenDroid audio stub source anchor mismatch")
    return text.replace(OLD, NEW, 1)


def apply(cpp_root: Path) -> None:
    path = Path(cpp_root) / REL
    path.write_text(patch(path.read_text()))
