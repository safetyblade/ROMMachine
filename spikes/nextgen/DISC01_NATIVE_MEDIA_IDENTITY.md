# DISC-01 — native multi-system media identity probe

This is source implementation infrastructure for Dreamcast #82, GameCube #83 and PS2 #84. It does **not** yet register untested systems, mutate LemuroidLibrary or promote titles to Supported Systems.

`NextGenDiscIdentity.kt` implements:

- **GameCube**: verified raw disc magic at offset 0x1C for GCM/ISO; rejects generic ISO. Identifies Wii separately at 0x18 solely to prevent false GameCube assignment — no Wii launch support.
- **Dreamcast**: parses bounded GDI track descriptor text (track count, numeric fields, local file names), verifies optional companion-track list. Does not treat generic BIN, CDI, CUE, CHD, RVZ or M3U as Dreamcast.
- **PS2**: for seekable ISO 9660 media, reads PVD sector 16, bounded root directory and `SYSTEM.CNF` content. Only identifies explicit `BOOT2 = cdrom0:\\...` as PS2; PS1 `BOOT` and unrelated ISO remain unclassified.
- **Safety**: bounded reads (descriptor 2048B, root <=64KiB, SYSTEM.CNF <=16KiB), never scans whole disc, caller owns SAF descriptor/channel. The API returns candidate platform/source and never writes to a game DB or state folder.

`NativeDiscIdentityTest.kt` exercises positive and negative synthetic bytes for GC, Wii, GDI, PS2, PS1 and generic/truncated ISOs. This is *not* real media or emulator gameplay evidence.

**Integration gate:** Wire to SAF URI / metadata and preserve existing game IDs, custom artwork, favourite sorting, companion tracks and prior game records only after independently proving normal CARTRIDGE → Open/Resume → gameplay for that system. For ambiguous CHD/RVZ/multidisc require a real parser or saved one-time selection, not folder heuristics. Do not run full-ROM checksum rescans or delete records on provider access failure.
