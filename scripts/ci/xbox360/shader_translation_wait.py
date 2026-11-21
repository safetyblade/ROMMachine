"""PERF-X360-03: port upstream's C++20 shader wait/publication correction.

Source concept: Xendroid+ #123 (BSD-3-Clause Xenia core). The pinned XenDroid
has a racy bool publication and spin-yields while translation is pending.
Use acquire/release publishing and atomic.wait to avoid busy-wait CPU pressure.
Every exact-source anchor must match once: no best-effort partial rewrites.
"""
from pathlib import Path


GPU = Path("xenia/src/xenia/gpu")


def once(source, old, new, name):
    if source.count(old) != 1:
        raise ValueError(f"Xenia shader-wait anchor mismatch: {name}")
    return source.replace(old, new, 1)


def patch_file(path, changes):
    text = path.read_text()
    for name, old, new in changes:
        text = once(text, old, new, name)
    path.write_text(text)


def apply(cpp):
    gpu = Path(cpp) / GPU
    header = gpu / "shader.h"
    patch_file(header, [
        ("shader valid flag",
         "bool is_valid() const { return is_valid_; }",
         "bool is_valid() const { return is_valid_.load(std::memory_order_acquire); }"),
        ("shader translated flag",
         "bool is_translated() const { return is_translated_; }",
         "bool is_translated() const { return is_translated_.load(std::memory_order_acquire); }"),
        ("translation wait method",
         "    bool TryClaimTranslation() {\n"
         "      return !translation_claimed_.test_and_set(std::memory_order_acq_rel);\n"
         "    }",
         "    bool TryClaimTranslation() {\n"
         "      return !translation_claimed_.test_and_set(std::memory_order_acq_rel);\n"
         "    }\n"
         "    void WaitForTranslation() const {\n"
         "      while (!is_translated_.load(std::memory_order_acquire)) {\n"
         "        is_translated_.wait(false, std::memory_order_acquire);\n"
         "      }\n"
         "    }\n"
         "    void PublishTranslation() {\n"
         "      is_translated_.store(true, std::memory_order_release);\n"
         "      is_translated_.notify_all();\n"
         "    }"),
        ("valid field", "    bool is_valid_ = false;", "    std::atomic<bool> is_valid_{false};"),
        ("translated field", "    bool is_translated_ = false;", "    std::atomic<bool> is_translated_{false};"),
    ])
    patch_file(gpu / "shader_translator.cc", [
        ("early publication",
         "  translation.is_translated_ = true;\n\n  bool is_valid = true;",
         "  // Publish only after validity, bindings and PostTranslation are complete.\n\n"
         "  bool is_valid = true;"),
        ("final publication",
         "  PostTranslation();\n\n  // In case is_valid_ is modified by PostTranslation, reload.",
         "  PostTranslation();\n"
         "  translation.PublishTranslation();\n\n"
         "  // In case is_valid_ is modified by PostTranslation, reload."),
    ])
    patch_file(gpu / "guest_spirv_shader_cache.cc", [
        ("SPIR-V translation wait",
         "        while (!translation.is_translated()) {\n"
         "          std::this_thread::yield();\n"
         "        }",
         "        translation.WaitForTranslation();"),
    ])
    patch_file(gpu / "vulkan/vulkan_pipeline_cache.cc", [
        ("Vulkan translation wait",
         "        while (!translation.is_translated()) {\n"
         "          xe::threading::MaybeYield();\n"
         "        }",
         "        translation.WaitForTranslation();"),
    ])
    patch_file(gpu / "d3d12/pipeline_cache.cc", [
        ("D3D12 translation wait",
         "        while (!translation.is_translated()) {\n"
         "          std::this_thread::yield();\n"
         "        }",
         "        translation.WaitForTranslation();"),
    ])
