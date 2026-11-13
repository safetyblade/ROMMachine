# CARTDECK — Phase 2B Personal Decals
**Approved owner workstream:** 14 November 2026 · Sienna Vale (design) · Elliot Shaw (engineering) · Daniel Mercer (architecture) · Marcus Reid (QA).

## Implementation
Phase 2B is a separate draft stacked PR (#205), based on Phase 2A source branch (#196). It is **not** merged, signed or owner-device accepted by the existence of this code.

**Player path:**
1. Settings → Appearance → My appearances; select a named user appearance or create one from Original/Pink/Green/Stealth Acid first.
2. Open **Personal decals** → **Import image**. Android **OpenDocument** system picker offers PNG/WebP/JPEG.
3. Artwork is immediately copied and normalised to a **temporary app-private image**. The external URI is never persisted, shared or searched.
4. Select semantic anchor (**Upper rail**, **Lower left**, **Lower right**, **Shelf rail**); set Scale, Rotation and Opacity in the editor. The same live console preview reflects changes without modifying a game.
5. **Remove decal** and **Reset placement** are reversible draft operations. **Cancel** deletes any newly imported staged image and discards draft metadata.
6. **Apply** commits only selected staged assets to durable app-private storage, then sends appearance metadata through the existing ShelfStore save path. Updating an existing selected personal style retains its identity. Save as new and Duplicate share a controlled immutable asset reference.

## Storage / security
- Local-only UUID asset IDs. Asset path is derived from a validated UUID and never from user-supplied names or URLs.
- New images are first written with AtomicFile under application **cache/cartdeck/decal-pending**, then promoted with AtomicFile to **files/cartdeck/decals**.
- Only valid PNG/JPEG/WebP file signatures accepted, regardless of claimed MIME. Limit raw input to **8 MiB**, image bounds to **8192 px per axis / 40 MP**, downsample to **1024 px**, normalise to PNG, and keep active + staged artwork under a **64 MiB** budget.
- No remote storage, content URI retention, broad file permissions, SVG scripts, ROM downloads, saved-game modifications or art gallery scraping.
- ShelfStore removes unreferenced durable assets **after** successfully writing the new AtomicFile appearance. Multiple user appearances referencing the same decal keep their asset until the last reference is removed. Pending assets expire after 24 hours or are discarded on Cancel.
- If a previous APK overwrites V2 optional decal fields, imported decoration metadata may be lost. Existing colours/patterns and game saves stay compatible. Export/backup user artwork before intentional version downgrade.

## Model and presentation
- Optional `CartdeckPersonalDecal(assetId, anchor, scale, rotation, opacity)` on both applied `ShelfAppearance` and `NamedAppearance`.
- Appearance sub-schema remains **2** with nullable optional `decal` JSON; missing fields read as no decal.
- Invalid UUID, anchor or out-of-range/NaN scale, opacity or rotation reject before persistence.
- `CartdeckDecalLayer` is an immutable passive Compose Canvas, sharing one image decode per active asset. Drawing uses a **rotation-safe envelope** and checks measured video, controller and branding exclusions. If no safe place exists, the image simply does not render; it does not shrink/crop the viewport or move game controls.
- The renderer keeps image aspect ratio. The semantic anchor is retargeted on rotation/tablet layouts rather than storing raw phone coordinates.
- Controller-first fullscreen gameplay contains **no** virtual shell/decal overlay; shelf and library remain themed where existing UI supports it. Actual native Android TV device and ten-foot focus remain separate QA gates.

## Device/QA acceptance — NOT YET PASSED
- Import valid/invalid PNG/JPEG/WebP, corrupt/oversized and unsupported types; no loss of existing artwork or permission prompts.
- Apply/Cancel/Replace/Remove/Reset; duplicate/rename/delete; install-over from legacy V1/V2, app restart and rollback.
- Compact portrait and tall owner handset; landscape phone with touch and gamepad; tablet both orientations; DS dual-screen; controller-first TV **where platform support exists**.
- Actual game and PadKit hit zones unaffected; no clipping, no game aspect change or new emulator view. High Contrast suppresses visual decoration. Large text/TalkBack and all editor touch targets, plus performance/memory profiling.
- **Independent baseline issue #135:** the existing GripArc unit-test source references unresolved `oversized` in older CI runs. This workstream must not quietly alter GripArc input logic to force a false PASS.
- Verify the parent Phase 2A source branch is actually merged into the intended release branch before Phase 2B integration; the earlier stacked #196 merge alone does not prove that.

**Next planned phase:** Phase 3 LED Studio. No reactive lighting/toggles are claimed in Phase 2B.
