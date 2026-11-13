# CARTDECK — Stealth / Acid (Design 001)
**Sienna Vale design lane · 14 November 2026 · Owner-approved Phase 2 visual direction**

## Scope delivered for Phase 2A (PR #196; stacked on Phase 1 PR #192)

A repeatable original-design surface system, not a Metal Gear skin or a separate game renderer.

- Stealth / Acid remains **immutable** in Custom Designs. Selection applies its existing palette plus original tactical-grid pattern and a CARTDECK chevron stamp.
- My Appearances gains **Pattern:** None / Tactical grid / Camo / Diagonal hatching / Pixel relief; **Stamp:** None / CARTDECK chevron / Smiley / Signal. Users may save personal variants independently.
- Pattern and stamp fields persist on the applied appearance and every personal named profile via additive optional fields in the same V2 AtomicFile. V1/V2 records with missing keys open as None without losing colour roles, shelf sections, ROM references or saves.
- Decorative artwork uses a shared procedural, static Canvas painter and exact same semantic palette in gameplay shell, live appearance preview and library navigation. No extra runtime asset fetch.
- Original vector references: [tactical-grid-tile.svg](./tactical-grid-tile.svg), [c-slot-tactical-crest.svg](./c-slot-tactical-crest.svg).

## Design language

| Material | Token | Meaning |
| --- | --- | --- |
| Matte graphite | `#151C20` | Manufactured chassis |
| Military olive | `#465840` | Insert/side trim and restrained pattern |
| Acid green | `#9BC74A` | Primary focus/seam/detail |
| Amber | `#E5A13A` | Limited secondary warning detail |
| Light lettering | `#D6DED2` | Readable brand markings |

Pattern marks should feel laser-etched or printed onto real surfaces; no floating HUD or proprietary tactical menu graphics. Original CARTDECK C-slot branding remains independently protected.

## One saved recipe across window classes

| Display | Presentation |
| --- | --- |
| Compact/tall phone portrait | Full manufactured chassis; tactical grid in residual shell corners/panels, optional stamp where safe; PadKit native controls keep their hit targets |
| Landscape phone | Slim side/edge decoration drawn only in real non-video residual regions; no portrait-sized ornamental panel and no forced extra top badge |
| Tablet portrait and landscape | Same palette, grid and stamp using scaled tile spacing; more available surface becomes decoration, no gameplay viewport reflow |
| Controller-first / TV-class library | Same colours and subtle pattern on shelf/navigation header and background; native game with physical controller hides ornamental shell. No game image overlay or virtual touch controls |

**Real Android TV app/device support is not proven by this PR.** TV presentation remains a separate capability/10-foot focus QA gate, and no unsupported TV runtime changes should be claimed.

## Safety and accessibility invariants

1. Live emulator viewport is measured from the existing LibretroDroid rectangle. No geometry, video scaling, game pixels or input code is changed.
2. Decorative drawing requires a valid measured viewport when in gameplay shell; otherwise it does not draw.
3. Tile bounds must be fully outside the expanded viewport, PadKit control rectangles and brand-seat rectangles. Stamp relocation tries safe residual corners/side strips and hides if no safe seat exists.
4. Tile work is bounded to at most 420 cells, even on tablet or TV-sized windows. No animation, polling, shared mutable Canvas cache or new renderer.
5. Decorative pattern has low opacity. High-contrast appearance suppresses optional surface decoration in gameplay/library; accessibility control focus and main text remain higher priority.
6. Cancel discards local draft; Apply uses existing atomic ShelfStore. Personal Save as new preserves the pattern/stamp. Factory preset returns pattern/stamp to None. Older builds may discard optional decoration fields if they rewrite a V2 appearance: back up/export before downgrade.
7. No new storage permissions, user-provided art files, remote artwork, ROM modification, game save changes or emulator core changes.

## Architectural milestones

**P2A implemented in source:** original designs, cross-layout procedural painter, safe exclusion geometry, first player controls, local persistence, source tests.

**P2B not yet implemented:** user-supplied PNG/WebP via Android Photo Picker/SAF, copying into bounded app-private decorative storage, security review, placement/scale/rotate/opacity, cleanup and per-profile ownership. Daniel must approve migration, rollback, validation and storage before Elliot's separate PR.

**P3 not yet implemented:** LED Off/Static/Reactive, per-zone lighting/brightness and reduced-motion handling. Static accent lines are not lighting controls.

## Owner / Cassia / Marcus acceptance required

- Screenshots: 393dp portrait, compact portrait, landscape game touch, landscape gamepad, tablet portrait/landscape, DS dual-screen, and TV/controller-first library if available.
- Visual identity: theme visible but subtle on each layout; full screen remains uncropped; original C-slot legible; frame not confused with Metal Gear or Sega hardware.
- Geometry: zero tile/stamp intersection with active game viewport or measured PadKit hit regions; no dropped native controls including Dreamcast and dual-screen.
- Data: install-over Phase 1/older V1 and V2, Apply/Cancel/Save as new/Copy/Rename/Delete/Reset, offline restart, and optional rollback.
- Performance: no perceptible render regression; measure allocations/overdraw on owner-class Samsung hardware; no thermal or input lag increase.
- Accessibility: 48dp touch targets, high contrast, focus/label contrast, TalkBack and controller-first TV focus. Current #74 viewport and #135 GripArc bugs stay independent and open.

**Do not merge on source-only PASS.** A green CI is not physical-device approval.
