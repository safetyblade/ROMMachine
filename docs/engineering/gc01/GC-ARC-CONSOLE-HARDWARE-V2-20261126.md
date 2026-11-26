# GC-ARC-V2 — complete console-native PivotArc and GripArc hardware

> **Owner-revised rollout 26 Nov 2026 (supersedes earlier phase ordering below):** First **prove full-piece GBA GripArc** physically using the successful `e7815a71` APK; second build/accept **all PivotArc** console designs; third build/accept **all full-piece GripArc** console designs; finally implement **Joint as placement-only**, without altering either completed pad. The [live 33-console register](GC-V2-CONSOLE-BUILD-TRACKER.md) is the single source of progress truth. Legacy V2B/V2C labels below describe implementation scope, not the new execution order.


Owner decision (26 November 2026): **solo PivotArc accepted**. Its original hardware outline, density, logo/wordmark, control-relative spacing and standalone visual language are locked. The submitted screenshots show PivotArc + GripArc still overlapping/compromising this accepted panel and Standard + GripArc still presenting as a separate L-shaped accessory. Therefore retire the concept of a single universal GripArc dock as the *final* product presentation.

## Product rule

Each console must have a deliberately designed controller assembly, not independent floating controls or an accessory pasted on afterwards. Four selectable user modes remain: Standard; PivotArc; GripArc (a complete native-layout console controller, retaining existing persisted STANDARD_GRIPARC enum ID for migration); PivotArc + GripArc (one compact, collision-free derived assembly). The game viewport stays fixed and controls remain at least 48dp touch targets. Positions, visuals and held-pointer interactions must be preserved independently. Do not turn off inaccessible inputs or silently distort accepted solo designs.

**Important**: This document is a complete design/coverage contract, not a claim of completed engineering or physical QA. For now, the existing Standard + GripArc remains an accessory implementation until a full-shell renderer is delivered and accepted. Never label a new mode physically passed from a source mapping test.

## Architecture

1. **Console inventory**: `ArcConsoleHardwareCatalog` assigns all 33 registered `SystemID` entries to physical-input families with an exhaustive Kotlin `when`. Do not infer runtime key mappings from family tags.
2. **Console profile**: each supported console/controller variant needs an explicit signed-off control map, PadKit/native IDs, physical art recipe, screen exclusion zone, touch coverage, utility rail, left/right hand and orientation rules. Multiple native pad variants are separate profiles (Genesis 3/6, arcade 4/6, PSX standard/DualShock, Wii Classic/Sideways/Nunchuk/GameCube).
3. **Shared industrial design**: keep the accepted solo PivotArc bevels, materials, key collars, mechanical button wells and wordmark. GripArc becomes its own **full console-profile controller chassis**; the programmable G1–G4 controls are machined into that face rather than rendered as an isolated corner tower.
4. **Combined mode**: derive from the same **console-specific** approved PivotArc controller profile, with a one-body GripArc variant preserving the primary cluster's relative positions. Move/scale the *whole* compatible hardware unit, never independently scatter A/B/L/R/Hold/utility keys; account for the true body silhouette, not just touch rectangles. If an entire body cannot fit, honestly show Unavailable and keep Standard: no overlapping fallback.
5. **Runtime separation**: legacy PadKit overlays can use the mobile screen solver. Native independent SDL/Vulkan hosts (Xbox, Xbox360, PS3 where available) need their own control-layer bridge before a new PivotArc profile is exposed. No fake Libretro ID or unsupported controller.
6. **Persistence**: preserve per-cartridge `ArcControlMode`, G1–G4 mappings, left/right and offsets; never change ROM IDs, native save roots, virtual HDD, SafeSave or source game metadata. A renderer replacement must migrate read-only from the existing v1 choices.
7. **Visual acceptance**: identical device, game and appearance, compare solo and full GripArc and combined variants. Match manufacture-quality contours, density, exact screen cutout and touchscreen ergonomics. Screenshot parity is a gate, not a substitute for playing, hold A/B, gestures, menu, audio, save/reopen and install-over.

## Family design queue — every registered console

| Family | Consoles | Physical requirements |
|---|---|---|
| Two-button | GB, GBC, NES, SMS, GG, NGP, NGC (Neo Geo Pocket Color), PC Engine | Compact D-pad + native A/B + correct start/select/option; distinct per-console legends |
| Four-button/shoulders | GBA, SNES | Preserve GBA accepted shell, SNES 4 face keys and shoulders |
| Six-button | Genesis, Sega CD | Different 3/6-button physical profiles and core device descriptors |
| Handheld dual direction | WonderSwan, WonderSwan Color | D-pad plus separate Cursor cluster; correct orientation |
| Dual-screen | NDS, Nintendo 3DS | Dual display exclusion; stylus/mic/lid controls; 3DS circle pad |
| Single analogue | PSP, N64 | PSP accepted analogue + D-pad + PS face legends; N64 C-buttons/analogue/Z and C directions |
| Dual analogue | PSX, PS2, PS3, Dreamcast, GameCube, original Xbox, Xbox360 | Native face legends, true analogue/sticks, trigger identities, PS DualShock variant, DC triggers, GC C-stick/Z, Xbox Black/White |
| Arcade | FBNeo, MAME2003Plus | 4-button and 6-button arcade controls; actual core mapping per selected pad |
| Atari | Atari 2600, Atari 7800 | Different 1-button vs 2-button semantics; console options |
| Wii remote | Wii | Classic/sideways/nunchuk/GC profiles distinct; motion/pointer unmapped until real capability |
| Desktop | DOS | Keyboard, mouse and gamepad/input preset; never assume a two-button pad |
| Handheld options | Atari Lynx | Options/Pause keys preserving existing input IDs |
| Wii U GamePad | Wii U | Console-specific input and native runtime proof before enabling |

## Implementation gates

- **GC-V2A**: inventory + per-system design contract (all 33), conservative mapped two-button expansion (NES/SMS/PC Engine), coverage tests. *Source-level work only.*
- **GC-V2B**: render true console-native full GripArc hardware for each supported family; retire corner L-piece when the corresponding replacement passes. Reuse all native controls and G1–G4 programmable assignments; no hidden buttons.
- **GC-V2C**: build proper combined per-console PivotArc + GripArc bodies without independent dock overlay, silhouette collision tests, portrait handset snapshots, left/right and existing profile migrations.
- **GC-V2D**: native-host adapters, remaining uncommon controllers and complete 33-console audit; mark unsupported cases explicitly until their runtime controller exists.
- **GC-V2E**: exact-head Kotlin/Room/GEM/native builds, signed install-over APK, owner + Marcus physical acceptance per console. No silent fallback, no merge until accepted.

## Current status

GC-V2A implementation started on its own PR from merged master. No new full GripArc chassis or combined console unit is claimed to be implemented by this first commit. Solo PivotArc owner-approved screens remain the immutable visual baseline.
