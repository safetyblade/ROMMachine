"""XGFX-01: bounded corrections to the pinned Xenia page tracking."""
from pathlib import Path

REL = Path("xenia/src/xenia/gpu/shared_memory.cc")


def replace_once(text, old, new):
    if text.count(old) != 1:
        raise ValueError("XGFX-01 pinned source anchor mismatch")
    return text.replace(old, new, 1)


def patch(text):
    # The dirty bitmap drives valid <- GPU-authoritative at the existing
    # optional frame-end refresh. CPU-only uploads also change that equation.
    # Mark a chunk only when the underlying flags actually change.
    text = replace_once(text, """      system_page_flags_valid_[i] |= valid_bits;
      if (written_by_gpu) {
        system_page_flags_valid_and_gpu_written_[i] |= valid_bits;
        // Mark the touched chunk dirty so the next copy picks it up.
        gpu_written_data_dirty_ = true;
        dirty_blocks_ |= 1u << (i >> 6);
      } else {
        system_page_flags_valid_and_gpu_written_[i] &= ~valid_bits;
      }""", """      const uint64_t old_valid = system_page_flags_valid_[i];
      const uint64_t old_gpu_written = system_page_flags_valid_and_gpu_written_[i];
      system_page_flags_valid_[i] |= valid_bits;
      if (written_by_gpu) {
        system_page_flags_valid_and_gpu_written_[i] |= valid_bits;
      } else {
        system_page_flags_valid_and_gpu_written_[i] &= ~valid_bits;
      }
      if (old_valid != system_page_flags_valid_[i] ||
          old_gpu_written != system_page_flags_valid_and_gpu_written_[i]) {
        cartdeck_changed_chunks |= 1u << (i >> 6);
      }""")
    # Batch dirty mask writes once per locked range rather than repeatedly
    # touching shared bookkeeping for every CPU-uploaded page.
    text = replace_once(text, """    for (uint32_t i = valid_block_first; i <= valid_block_last; ++i) {""",
        """    uint32_t cartdeck_changed_chunks = 0;
    for (uint32_t i = valid_block_first; i <= valid_block_last; ++i) {""")
    text = replace_once(text, """      }
    }
  }

  if (memory_invalidation_callback_handle_) {""",
        """      }
    }
    if (cartdeck_changed_chunks) {
      gpu_written_data_dirty_ = true;
      dirty_blocks_ |= cartdeck_changed_chunks;
    }
  }

  if (memory_invalidation_callback_handle_) {""")
    # Empty ranges have no invalidated pages. Malformed ranges must not
    # permit hoisting an upload ahead of previously recorded GPU consumers.
    return replace_once(text, """  for (uint32_t i = 0; i < count; ++i) {
    uint32_t page_first = page_ranges[i].first;
    uint32_t page_last = page_first + page_ranges[i].second - 1;""", """  if (count && (!page_ranges || !system_page_flags_invalidated_in_submission_)) {
    return true;
  }
  const uint32_t page_count = kBufferSize >> page_size_log2_;
  for (uint32_t i = 0; i < count; ++i) {
    const uint32_t page_first = page_ranges[i].first;
    const uint32_t length = page_ranges[i].second;
    if (!length) {
      continue;
    }
    if (page_first >= page_count || length > page_count - page_first) {
      return true;
    }
    const uint32_t page_last = page_first + length - 1;""")


def apply(cpp):
    path = Path(cpp) / REL
    path.write_text(patch(path.read_text()))
