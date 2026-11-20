# UI-UNI-01 — Universal gameplay appearance

Owner accepted UNI-A on device. UNI-B consolidates appearance resolution without changing engine or input geometry. Retain the accepted A checkpoint at 7007b177b289b6b713fbe66313b4f532aa77fac6.

## UNI-C — design approved by owner, implementation candidate

- C01: replace separately drawn screen side rail assets with flush, recessed bezel accent channels.
- C02: align left and right upper chassis accents with the central cartridge indicator, using shared mirrored geometry.
- C03: connect the upper accents and side channels to the existing lighting settings (off, static, supported game feedback and controller activity). Off is recessed and unlit; high contrast disables decorative light. Native hosts without real feedback must not simulate it.
- C04: consistent bezel thickness and finish, with measured video exclusion and dual-screen protection.
- C05: integrated speaker and indicator detailing.
- C06: verify all editions and custom appearances; no per-engine artwork branches.

Do not change aperture placement or control geometry in C; adaptive layout is UNI-D. Keep maximised, TV and borderless modes game-first and respect the existing renderer lifecycle. No native binaries, save data, or storage migrations for this UI lane.

## Landscape acceptance (owner approved)

With surround enabled, use recessed channels and shared materials in portrait and landscape. Add control housings only in measured residual chassis with actual touch inputs; compact branding must not displace the aperture. Maximised mode remains minimal and borderless remains undecorated. Reuse side-rail zone for crown and vertical channels, screen zone for horizontal channels and control-well zone for indicators. Native hosts use baseline lighting until genuine feedback is available. UNI-D owns positioning changes.

## Owner acceptance revision

Standard portrait accepted. Landscape hardware, a selectable minimal surround, and GripArc alignment require correction. Approved: Hardware / Minimal / Off for each orientation, preserving saved STANDARD as Hardware, OFF as Off and inherited per-cartridge choices. Physical controllers retain game-first presentation. Standard landscape hardware reserves measured pad areas before fitting the viewport. Minimal deliberately keeps the existing overlay policy and paints only a measured thin surround. TV remains game-first.

GripArc final approved direction: close-fitting L around the selected native hand group, preserving the Standard constellation with modest proportional scaling and shifts. All four programmable buttons G1–G4 are independently available on every supported controller; mirror for left-hand mode. Do not scatter controls with dock-first free-space packing, remove original inputs, shrink touch targets below their safe minimum, or silently change saved settings. Resolve a shared deck, controls, dock and viewport together, including native hosts. This is a separate solver implementation and owner acceptance gate, not delivered by the display-menu and standard landscape increment. PivotArc/combined modes and all engines remain explicit validation requirements.

## C09–C13 owner-approved material follow-up

Permanent accent inlays are separate from emitted light. Reuse palette roles for every appearance, never hardcode Arcana. Apply engraving after landscape housing fill with measured control exclusions. Retain screen and housing geometry; use small Standard landscape pad constraint clearances. GripArc and composite housing share the inlay/lighting renderer; the close-fitting four-control solver is still outstanding. Verify appearance, glow, control clearance and preservation of native inputs on device before acceptance.

## C14 / C15 / D2 owner-approved follow-up

Landscapes use paired geometric engraving bands and independent artwork seats. If clearance fails, omit the paired band rather than scatter tiles. Preserve the same appearance roles and keep controller surfaces clean. Lighting behaviour and finish are independent: Solid / Glow / Neon, with Glow the legacy default and intensity/colour/zones retained. Standard landscape controls use session-only radial scale capping and shared housing clearances, preserving native inputs, rotation and saved settings. This is a candidate fit, not device-verified. GripArc close-fit solver and other adaptive configurations remain D3/D4 work.

## UNI-D3 — Adaptive controller fit (implementation candidate)

Owner accepts existing portrait and landscape hardware locations; lower parallel purple trim overlap is cosmetic and must not block UNI-D. Suppress the redundant landscape lower seam while retaining paired artwork bands, permanent material inlays and the existing bezel.

Standard + GripArc must be eligible on landscape as well as portrait when touch controls and hardware surround are enabled. Reserve side-deck widths before fitting video; never crop/stretch actual emulator pixels or steal input events. Use a shared finite placement solver with complete native inventory, G1–G4 mapped independently, preserved profile counts and user mappings. New unconfigured profiles expose four slots by default; existing saved counts survive unchanged. On narrow viewports or unsafe fits, retain standard controls and explain the constraint. Do not silently present an overlapping layout. The first implementation extends combined inventory placement with measured landscape side-space and post-fit overlap checks. More compact close-to-control grouping requires owner/device acceptance, not an assumed pass.

## UNI-D4 / D5 — next increments

D4: optimise portrait whitespace and responsive landscape, tablet and TV allocation without moving accepted chassis hardware, losing dual-screen composition, or changing physical controller/Maximised/Off semantics. D5: validate every supported inventory, four G mappings, mirrored left/right, PivotArc combined, screen exclusion, portrait/landscape, small handsets, tablet/TV, profiles and saves. Hold final acceptance until physical input testing.

## SIGNATURE — Cassia approval located

The controlling source is GC-01 issue #262, updated 20 November 2026. Fixed application brand: midnight #13131A, neon pink #FF4FA3, electric cyan #19F5DA. SIGNATURE is also an additional selectable deck appearance; retain Original and custom appearances. Approved for bounded additive UNI follow-up after the current GripArc checkpoint, not implemented in this D3 candidate. Launcher icon, startup/loading and universal tokens remain in scope; do not change logo geometry.
