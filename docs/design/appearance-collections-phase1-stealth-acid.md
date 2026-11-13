# CARTDECK — Appearance Collections, Phase 1

**Owner direction: 14 November 2026** · Sienna Vale (visual design) · Elliot Shaw (engineering) · Cassia Rowan (product) · Marcus Reid (QA).  
**Design status:** implementation candidate, device acceptance open.

## Player information architecture — in this exact order

1. **Base colours** — three immutable, existing first-party CARTDECK editions: Original / Pink / Green. Their source assets, materials and control geometry remain untouched.
2. **Custom designs** — curated CARTDECK-designed finishes. Phase 1 includes **Theme 001 — Stealth / Acid**, an original tactical-colour palette inspired by collectible card/tactical interfaces. This is *not* bundled Metal Gear artwork, game IP, a ROM skin or a gameplay overlay.
3. **My appearances** — the existing user-owned style library and its colour editing. Select, save as new, copy, rename, delete, edit colour roles (including precise hex), reset roles, and choose accessibility preferences. Existing atomic Apply/Cancel remain the only commitment boundary.

The live console/shelf preview remains visible between the curated collection and personal editing. A theme selection is a local draft until Apply. User styles are never converted to factory/curated themes.

## Stealth / Acid v1 palette

| Role | Value |
| --- | --- |
| Graphite chassis | #151C20 |
| Military olive secondary | #465840 |
| Acid accent | #9BC74A |
| Amber focus | #E5A13A |
| Light mint lettering | #D6DED2 |
| Screen bezel | #0B1112 |
| Button housing | #253028 |
| Shelf backboard | #17211A |
| Shelf deck | #364337 |
| Shelf focus | #C6E468 |

All remaining material tokens are in `CartdeckCuratedDesigns.kt`. The original consoles and pre-existing game artwork must retain their authentic content and colour.

## Implementation boundaries

- The catalog is a data object containing immutable first-party palette mappings: `CartdeckCuratedDesigns`. Later special designs can register additional entries without new renderer paths.
- `ShelfAppearance.curatedDesignId` is an **optional** selection identity, stored separately from `activeProfileId`. Curated selection is represented by existing semantic colour overrides, so current console and shelf painters automatically display it.
- A personal copy captures the actual colour roles in a new `NamedAppearance` and **clears curated identity**; user-owned changes never mutate immutable curated designs.
- ShelfStore appearance sub-schema advances from **2 to 3**. V1/V2 reading remains supported, and the same AtomicFile still commits appearance state and shelf layout without touching ROMs, cartridges, save data, app identity or emulator settings.
- No new Android permission, online theme source or asset download; no runtime engine or input change; no independent theme render pipeline.
- Never introduce a forced gameplay badge/banner or shrink/maximise the viewport differently because a theme was selected.

## Later phases — not yet claimed working

- **Phase 2:** original matte patterns, tactical-grid engraving and optional sticker/stamp/safe-area controls (Issue #75).
- **Phase 3:** LED/glow Off / Static / Reactive and per-zone colour/brightness from supported native asset masks. Lighting defaults OFF; only display controls when backed by actual behaviour.
- **My appearances advanced:** extend personal appearances to support the same pattern/decal/lighting controls when delivered, with reversible per-profile storage and no new mandatory step to launch a game.

## QA acceptance

- Base colours appear first, then Custom designs, then My appearances on compact Android portrait; current selection is unambiguous and live preview updates immediately.
- Stealth / Acid is selectable, reversible with Cancel, durable after Apply/relaunch/install-over and uses the real shelf/console material tokens.
- Reopening Appearance reselects Stealth / Acid; switching to Base clears its active identity. Switching to an existing personal profile also clears it.
- Starting from Stealth / Acid and using Save as new produces an independent personal copy with the palette retained. Renaming/duplicating/removing that copy never edits the factory design.
- Existing V1/V2 named appearances, colours, shelf sections, game order and save data persist with no loss. Unknown newer appearance schema is not silently overwritten.
- Readable shelf text/focus and accessibility remain; 48dp targets, TalkBack, high-contrast and reduced-motion behaviour should be device-tested.
- All native console controller inventories, GripArc, PivotArc, ROM operations, launch/resume, native game content and PS1/PSP/DC/GC/PS2/Xbox builds are unchanged. Known release blocker #135 is separate.
- Tests/CI, signed ARM64 APK, owner handset screenshots, install-over and Marcus independent QA required before an implementation PASS.

**Rollback:** revert the feature branch/PR. Existing appearance versions remain available; do not install a rollback build over user-edited v3 appearance files without first confirming the old reader's schema compatibility.
