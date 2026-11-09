#!/usr/bin/env python3
"""Read-only Dreamcast disc metadata probe.

This does not register Dreamcast in CARTDECK, mount a disc, download a ROM,
or assert that a libretro core is compatible with the Android frontend.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import PurePosixPath
import shlex


@dataclass(frozen=True)
class DiscCandidate:
    kind: str
    safe_for_auto_detection: bool
    reason: str


@dataclass(frozen=True)
class GdiTrack:
    number: int
    lba: int
    track_type: int
    sector_size: int
    filename: str
    file_offset: int


def classify_disc_filename(filename: str) -> DiscCandidate:
    """Classify filenames conservatively: a shared extension is not a system ID."""
    suffix = PurePosixPath(filename.replace("\\", "/")).suffix.lower()
    if suffix == ".gdi":
        return DiscCandidate(
            "dreamcast-gdi",
            True,
            "Dreamcast-specific track descriptor; still validate all companion tracks.",
        )
    if suffix in {".chd", ".cue", ".m3u", ".iso", ".bin"}:
        return DiscCandidate(
            "ambiguous",
            False,
            "Shared disc format: require verified container metadata or explicit user system.",
        )
    if suffix == ".cdi":
        return DiscCandidate(
            "candidate-cdi",
            False,
            "A CDI extension alone does not prove an original, complete Dreamcast image.",
        )
    return DiscCandidate("unrecognised", False, "Not a supported probe format.")


def parse_gdi(text: str) -> tuple[GdiTrack, ...]:
    """Validate a GDI descriptor without opening or extracting its track files.

    A valid descriptor gives an exact track count followed by:
    number LBA track-type sector-size filename offset.
    Filenames must remain within the descriptor directory.
    """
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if not lines:
        raise ValueError("GDI descriptor is empty")
    try:
        count = int(lines[0])
    except ValueError as exc:
        raise ValueError("Invalid GDI track count") from exc
    if count < 1 or count > 99 or len(lines) != count + 1:
        raise ValueError("GDI track count does not match the descriptor")
    tracks: list[GdiTrack] = []
    for track_index, line in enumerate(lines[1:], start=1):
        try:
            fields = shlex.split(line, posix=True)
        except ValueError as exc:
            raise ValueError("Invalid quoted GDI track filename") from exc
        if len(fields) != 6:
            raise ValueError("Each GDI track must have six fields")
        try:
            number, lba, track_type, sector_size = map(int, fields[:4])
            offset = int(fields[5])
        except ValueError as exc:
            raise ValueError("Invalid numeric GDI field") from exc
        filename = fields[4].replace("\\", "/")
        path = PurePosixPath(filename)
        if (
            path.is_absolute()
            or ".." in path.parts
            or len(path.parts) != 1
            or not filename
            or ":" in filename
        ):
            raise ValueError("GDI track filename must be a local file")
        if number != track_index or lba < 0 or track_type not in (0, 4):
            raise ValueError("Invalid GDI track number, LBA or type")
        if sector_size not in (2048, 2336, 2352) or offset < 0:
            raise ValueError("Invalid GDI sector size or offset")
        tracks.append(GdiTrack(number, lba, track_type, sector_size, filename, offset))
    return tuple(tracks)
