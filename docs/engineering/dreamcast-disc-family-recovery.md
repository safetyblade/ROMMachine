# Dreamcast descriptor discovery and staging

Refs #82, #143. Source implementation; phone gameplay acceptance remains open.

Root causes in the merged source: Dreamcast did not register CUE; descriptor and
playlist grouping required byte-exact case matches against SAF display names;
CUE data tracks could win metadata selection before their descriptor; standard
SAF staging used display names rather than the names the native descriptor opens.
Those are independent of the working CDI renderer.

The scanner now registers CUE without making it a unique Dreamcast extension.
Explicit Dreamcast folders, GD-ROM density declarations and bounded Dreamcast
boot-sector evidence identify CUE families. Positive legacy CD evidence takes
priority over a misplaced Dreamcast folder. Ambiguous CUE remains eligible for
existing PS1/Sega CD identification. CHD's existing GD-ROM metadata probe and CDI
selection are retained. A Dreamcast playlist requires every referenced disc to
identify as Dreamcast; a mixed-console playlist is not promoted by one GDI.

GDI/CUE/M3U references resolve exact names first, then a unique case-insensitive
match. Ambiguous case collisions are rejected. Present tracks stay grouped under
an incomplete descriptor, and new imports validate completeness before insertion.
Repeated refresh uses the descriptor's original URI and existing database ID.
Skipped descriptor families produce recovery guidance in the refresh result;
existing incomplete cartridges fail Open with a recoverable disc-family message.

For Dreamcast descriptor launches, both local and SAF providers traverse the
reachable family and stage dependencies before the primary. Native files receive
the descriptor's exact requested spelling. Descriptor bytes, indices, LBAs and
track offsets are never regenerated. The original ROMs, shelf identity, VMU and
save directories are untouched. The normal single-file CDI/CHD paths are unchanged.

## Supported bounds

Flat same-directory GDI/CUE families and M3U disc sets, with up to 99 playlist
entries and a bounded two-level descriptor graph. Traversal, remote paths, nested
M3U, oversized descriptors, malformed GDI and missing/ambiguous companions fail
closed. Nested disc folders and multi-file archives are not silently flattened or
claimed supported. No disc geometry or missing descriptor is fabricated.

## Verification and phone acceptance

Pure tests cover GDI/RAW and CUE staging, case aliases, unchanged descriptor bytes,
M3U reopen, missing members, unsafe references, legacy boot discrimination and
CHD metadata. Android grouping tests cover actual source names and incomplete
families; full Android test execution is still delegated to CI because the local
Robolectric runtime download is blocked by workspace network policy.

On the owner phone, refresh lawful fixtures for CDI, complete GDI/BIN/RAW, GD-ROM
CUE/BIN, self-boot CUE, GD-ROM CHD and flat M3U. Check one shelf cartridge per disc
family, then Open, first visible frame, audio, controls, VMU save, exit and reopen.
Temporarily remove one track and refresh: keep the existing cartridge/player data,
show recovery information on Open, restore the track and confirm the same ID.
Check mixed PS1/Sega CD/Dreamcast folders and case-colliding files remain distinct.
Check an install-over retains working CDI and prior VMU data. These hardware
results are not implied by successful Kotlin compilation or staging tests.
