"""Read-only next-generation disc identity probe for CARTDECK's future scanner.

This helper does not register new emulators, import ROMs or open game files.
For ambiguous container extensions, a system label is allowed ONLY when the
raw disc signature is positively identified. Compressed RVZ/CHD needs an
explicit container parser later, not extension-based guessing.
"""
from dataclasses import dataclass
from pathlib import PurePosixPath


GAMECUBE_MAGIC = bytes.fromhex("C2339F3D")
WII_MAGIC = bytes.fromhex("5D1C9EA3")
GC_MAGIC_OFFSET = 0x1C
WII_MAGIC_OFFSET = 0x18


@dataclass(frozen=True)
class MediaCandidate:
    system: str | None
    confidence: str
    reason: str


def probe_disc(filename: str, header: bytes = b"") -> MediaCandidate:
    """Classify without searching for PS2 strings in unrelated file data.

    Header must contain bytes from the *raw uncompressed disc* at offset 0.
    This must never be run against a random offset, zipped file or RVZ/CHD.
    """
    suffix = PurePosixPath(filename.replace("\\", "/")).suffix.lower()
    if suffix in {".rvz", ".chd", ".cso", ".zso", ".m3u", ".cue", ".bin", ".img", ".nrg", ".mdf"}:
        return MediaCandidate(None, "unknown", "Shared or compressed format; needs container parsing")
    if suffix in {".iso", ".gcm"}:
        if len(header) >= 0x20 and header[GC_MAGIC_OFFSET:GC_MAGIC_OFFSET + 4] == GAMECUBE_MAGIC:
            return MediaCandidate("GAMECUBE", "signature", "Nintendo GameCube disc magic at 0x1C")
        if len(header) >= 0x1C and header[WII_MAGIC_OFFSET:WII_MAGIC_OFFSET + 4] == WII_MAGIC:
            return MediaCandidate("WII", "signature", "Nintendo Wii disc magic at 0x18; not GameCube")
        return MediaCandidate(None, "unknown", "ISO/GCM suffix not enough; PS2 cannot be inferred safely")
    if suffix == ".elf":
        return MediaCandidate(None, "unknown", "Generic ELF is not a PS2 ownership/system signal")
    return MediaCandidate(None, "unknown", "Unrecognised or unrelated format")
