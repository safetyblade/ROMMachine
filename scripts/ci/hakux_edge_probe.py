"""XB-01/02: bounded exact-source transform for hakuX guest/swap border samples.

Existing native probe copies 64 RGBA pixels per image at three asynchronous
frames. Replace interior grid positions only; do not modify normal rendering,
sampling filters, command submission, or the original engine's pixel data.
"""
from pathlib import Path

REL = Path("hw/xbox/hle/vk/swapchain.c")
OLD = """    VkBufferImageCopy regions[64] = {0};
    for (unsigned i = 0; i < 64; i++) {
        regions[i].bufferOffset = offset + i * 4;
        regions[i].imageSubresource = (VkImageSubresourceLayers){ VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
        regions[i].imageOffset = (VkOffset3D){ (int32_t)(((i % 8) * 2 + 1) * (uint64_t)w / 16),
                                             (int32_t)(((i / 8) * 2 + 1) * (uint64_t)h / 16), 0 };
        regions[i].imageExtent = (VkExtent3D){1, 1, 1};
    }"""
NEW = """    /* 4 edges x 2 rings x 8 locations = 64 sparse RGBA pixels.
     * Outer ring (0) plus adjacent inner ring (1); clamp degenerate
     * dimensions and preserve identical normalized positions per stage.
     * Probe remains on-demand, <=3 frames and asynchronous.
     * It DOES NOT cover every edge pixel or establish the fringe's cause. */
    VkBufferImageCopy regions[64] = {0};
    for (unsigned i = 0; i < 64; i++) {
        const unsigned side = i / 16, ring = (i % 16) / 8, step = i % 8;
        const uint32_t x = (uint32_t)((uint64_t)step * (w - 1) / 7);
        const uint32_t y = (uint32_t)((uint64_t)step * (h - 1) / 7);
        const uint32_t ix = ring && w > 1 ? 1 : 0;
        const uint32_t iy = ring && h > 1 ? 1 : 0;
        const uint32_t px = side == 2 ? ix : side == 3 ? w - 1 - ix : x;
        const uint32_t py = side == 0 ? iy : side == 1 ? h - 1 - iy : y;
        regions[i].bufferOffset = offset + i * 4;
        regions[i].imageSubresource = (VkImageSubresourceLayers){ VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1 };
        regions[i].imageOffset = (VkOffset3D){ (int32_t)px, (int32_t)py, 0 };
        regions[i].imageExtent = (VkExtent3D){1, 1, 1};
    }"""


def patch(text: str) -> str:
    if text.count(OLD) != 1:
        raise ValueError("Pinned native edge probe source anchor absent or duplicated")
    if text.count("static void probe_copy(") != 1:
        raise ValueError("Pinned native probe function anchor mismatch")
    return text.replace(OLD, NEW, 1)


def apply(root: Path) -> None:
    path = Path(root) / REL
    original = path.read_text()
    path.write_text(patch(original))
