# CARTDECK consolidated engineering closeout

Draft [PR #385](https://github.com/safetyblade/ROMMachine/pull/385) continues from
`57d940e4f163c0dfa9fc93beae28b07f82b9bb74`. The 21-item owner register records
8 PASS, 6 PARTIAL, 6 FAIL and 1 NOT TESTED. These statuses remain as received.
The earlier 41-item ledger governs inherited scope.

## Owner register

| ID | Feature | Owner status |
| --- | --- | --- |
| 01 | Install-over and continuity | PASS |
| 02 | Hardware finish and placement | PARTIAL |
| 03 | PS2 ARMSX2 GripARC | PARTIAL |
| 04 | GameCube GripARC | FAIL |
| 05 | Xbox GripARC | PARTIAL |
| 06 | Xbox 360 GripARC | PARTIAL |
| 07 | XBLA launch | PASS |
| 08 | XBLA full-game recognition | FAIL |
| 09 | Failed launch recovery | PASS |
| 10 | Metroid Prime Trilogy | FAIL |
| 11 | SafeSave icon | PASS |
| 12 | Support report workflow | PARTIAL |
| 13 | Support offline submission | PASS |
| 14 | Live support ticket | FAIL |
| 15 | Controller factory reset | PASS |
| 16 | Nintendo 64 GripARC | FAIL |
| 17 | Wii GripARC | FAIL |
| 18 | Wii U GripARC | PARTIAL |
| 19 | Wii U touchscreen | PASS |
| 20 | Wii U pointer protection | PASS |
| 21 | Physical controller | NOT TESTED |

## Programme matrix

| Package | Implementation and evidence | Remaining engineering and physical acceptance |
| --- | --- | --- |
| CLOSE-01 GripARC geometry | IMPLEMENTED first-pass uniform chassis transform, right-panel Xbox stick click and programmable seats, larger standalone analogue caps, and geometry export for each orientation. C01 (Claire, 28 Nov 2026): GameCube landscape now resolves through explicitly authored left and right hardware panels plus a utility rail, allocated together with the gameplay aperture from the measured safe area and viewport; it never falls through to the legacy seat projection or the compact directional fallback, and a regression test (`gameCubeLandscapeUsesAuthoredPanelsAndUtilityRail`) requires that fit on the reference handheld fixture — larger D-pad (88dp) and analogue stick (92dp), console-authentic face cluster (large central A, B low-left, X/Y kidneys above), programmable seats contained in the panels, Menu/Start contained in the rail, no target on the aperture. | Legacy projection remains in use where the transform fails for the other consoles (C02). Finish responsive authored alternatives and coordinated aperture allocation. The actual SM-S721B safe bounds and viewport measurements are still needed to turn the reference handheld into a handset regression. Verify direct GripARC load and reach on target hardware. GameCube landscape physical acceptance remains owner QA — source fit is not a device PASS. |
| CLOSE-02 Standalone | IMPLEMENTED source coverage remains 45 of 53 configurations. The manifest coverage test accounts for all 106 combinations and exports authored and resolved geometry for registered cases. VERIFIED route registration is source evidence. | Finish Wii Nunchuk stick and touch, shared Aim & Fire, physical assignments and Play! integration. Arcade two/three requires verified game-specific FBNeo and MAME action inventories. DOS keyboard and mouse require actual dispatch in an eligible integrated host. Existing PC-lab input support does not establish this production route. Protected PS3 delivery and Joint are DEFERRED. |
| CLOSE-03 Xbox 360 | OWNER ACCEPTED discovery and extensionless launch. The isolated test suite retains package/entitlement checks and the application suite owns engine selection checks. New diagnostics record STFS/SVOD class, title ID, profile creation, matching entitlement and whether the client supplied the license argument. | Full-game recognition remains FAIL. Correlate the redacted evidence with the effective native profile and supported ownership data. Verify launch, progress persistence and reopen with a legitimately owned title. |
| CLOSE-04 PERF | Previous Wii video-gap metrics remain available. Pinned host assessment establishes FIFO delivery and resampling, a two-frame emulation catch-up limit, and separate frontend and core duplicate-frame policies. | The historical Metroid export has no guest-speed, audio underrun, shader-stall or loaded-core evidence. Reproduce on the affected device and correlate metrics before changing runtime policy. The original Xbox fringe, presentation, cache and geometry need source-to-swap captures under the existing [PERF plan](XB-01-03-ORIGINAL-XBOX.md). |
| CLOSE-05 Support | IMPLEMENTED exact snapshot export in ZIP, JSON and TXT, one-tap clipboard, local-save feedback, distinct history identity, current Libretro session/cartridge fields, in-process submission exclusion and immutable consented report editing. | Physical process restart and file-picker flow remain to be verified. Live delivery is BLOCKED from this workspace. Public DNS returned NXDOMAIN for the configured hostname and the proxy rejected HTTPS CONNECT. Verify server deployment, private storage, idempotent receipts and operator notification using synthetic data before certifying submission. |
| CLOSE-06 GC | IMPLEMENTED live Libretro shader updates from effective display choices on the emulation thread. VERIFIED all 88,246 pinned catalogue records, taxonomy and 1,235 versioned feature records. Personal-data paths were retained. SafeSave dock icon and prior continuity are OWNER ACCEPTED. | Verify actual on-device pixel changes and preserved aspect ratio. Continue only evidenced catalogue review and preserve reviewed DeckFit and special-input holds. Estimates are catalogue data. |
| CLOSE-07 PC | VERIFIED [opt-in PC assembly and Linux DOS persistence](https://github.com/safetyblade/ROMMachine/actions/runs/46094711060) and [native Wine ARM64, WoW64 translator and owner-test APK assembly](https://github.com/safetyblade/ROMMachine/actions/runs/45923433415). The [combined Wine proof](https://github.com/safetyblade/ROMMachine/actions/runs/44497559415) failed at translated GDI, registry and save persistence. | Android ARM64 guest execution, installed Windows application persistence, selected EXE launch, integrated input/display and SafeSave boundaries require the gated runtime proof. Shandalar remains the first owner-facing target after engineering graduation. |
| CLOSE-08 Deferred scope | DEFERRED Maker under #394, Riivolution activation and Joint. Research, validators and fenced source remain in the PR. | Riivolution activation requires proof of patched-content boot in the native Dolphin path. Protected PS3 packaging remains on the existing master and distribution route. |

## Evidence and owner QA delta

- Geometry evidence is produced by `GripArcRenderingTest` at
  `lemuroid-app/build/reports/gc01-menu-screens/griparc/standalone-manifest-coverage.json`.
  Each case records both orientations, the fixture safe and viewport rectangles,
  the selected arrangement and the available authored seats, resolved targets and cap sizes.
- When the remaining controller work is complete, verify the corrected GripARC cases
  on the SM-S721B in both orientations. Check reach, simultaneous controls,
  independent release, utility containment and direct launch with GripARC selected.
- Recheck Xbox 360 full-game recognition and progress after reopen. Preserve
  the existing discovery and launch pass.
- Create two support reports and compare their IDs, builds and timestamps across
  history, clipboard and device exports after restart. Verify live receipt and
  reference only once the intake is reachable. Retain manual fallback.
- Verify a SmartVisual change in visible gameplay and preserve aspect ratio.
  Repeat Metroid and original Xbox performance cases after the corresponding
  causes have been established and addressed.
- Test physical controller programmable assignments and lifecycle release
  after implementation. Keep accepted Wii U pointer protection and touch as
  targeted regression checks. No repeat of the full inherited register is required.

## Claire stage log

- C01 — GameCube landscape authored panels + utility rail. Starting SHA
  `ad6866dd5f18cd3bdef909506caf39c35201d523`. Files:
  `StandaloneGripArcLayout.kt` (dedicated `resolveGameCubeLandscape`,
  fit label `authored GameCube landscape panels + utility rail`),
  `GripArcRenderingTest.kt` (new regression test), this closeout.
  Geometry verified against the reference fixture (safe 8,8–732,352;
  viewport 180,20–560,290): left/right panels 166dp wide, rail 56dp,
  all 14 native + 4 programmable targets inside their zone, inside the
  safe area, clear of the aperture and mutually non-overlapping.
  Hosted verification (reported by Cassia, 28 Nov 2026): Kotlin source
  gate, Room migration, connected APK and UI merge gates PASS at
  `c4d89b9e`; dedicated Xbox 360 engineering PASS; isolated PC runtime
  laboratory FAIL (recorded under C06). No owner QA round on that APK.
- C02-A — authored landscape geometry generalised (Claire, 28 Nov 2026).
  `resolveAuthoredLandscape` now applies the C01 architecture to every
  standalone configuration: the measured viewport divides the safe area
  into left/right hardware panels and a utility rail; each console keeps
  its own authored seat map and authentic arrangement (reference x picks
  the zone, position is the authored proportion of the zone, y is the
  authored proportion of panel height, one uniform size scale, 42dp
  floor). Landscape no longer runs the legacy seat projection or the
  compact directional fallback at all — the only other landscape route
  is the uniform chassis transform, and a configuration that fits
  neither authored route returns no fit. Portrait routes are unchanged
  in this checkpoint (authored first; legacy projection remains the
  portrait fallback for PSP/DS-class densities — portrait authored
  migration is still open). Verified before push by a faithful
  simulation of the resolver across all 45 fixtures: zone-authored
  landscape fits 43/43 non-GameCube fixtures (the 2 GameCube-controller
  fixtures use the C01 dedicated resolver), and the coverage regression
  now asserts an authored landscape fit label for every fixture
  (`landscapeAuthored`), plus a dedicated N64 landscape regression.
  Register (C02-D, partial): 45/53 configurations implemented;
  landscape authored 45/45 implemented configurations; portrait
  authored-first 45/45 with legacy fallback retained on failure;
  native dispatch unchanged from the recovered state; physical
  acceptance unchanged (owner register governs). C02-B (eight missing
  configurations with real dispatch), C02-C (Play!, physical
  programmable assignments) and full C02-D verification remain open
  and are reported as such in the C02 stage return — not claimed here.
- C02-B checkpoint 1 — FBNeo/MAME digital2 + digital3 (Claire, 28 Nov
  2026). New first-class controller variants following the PCE_SIX
  pattern: `TouchControllerID.ARCADE_2` / `ARCADE_3` (appended to the
  enum, existing ordinals unchanged), native layouts `Arcade2` /
  `Arcade3` (same left side and same key events as ARCADE_4 buttons
  1–2 / 1–3, so dispatch is identical per seat), solo profiles
  `arcadeButtons(2|3)` = the first N numbered seats of the four-button
  arrangement (seat 1 = Y, 2 = B, 3 = X) with their Hold companions and
  the Coin rail, standalone inventories/seat maps derived from the
  four-button authored maps, programmable guest codes via the existing
  BUTTON_1 + seat-number rule, `ControllerConfigs` FB_NEO_2/3 and
  MAME_2003_2/3 with new `controller_arcade_2/3` strings, and both
  systems' GameSystem config lists now offer all four arcade variants.
  Coverage fixtures added for FBNeo and MAME2003Plus digital2/digital3
  (49/53 configurations; 4 remaining: Wii nunchuk_stick, Wii
  nunchuk_touch, DOS keyboard_action, DOS mouse). Hosted verification
  pending at push time. Cassia's C02-A conditions carried forward: the
  42dp floor is a floor, not a target — explicit primary-control
  minimums and explicit programmable-control dimensions are C02-D
  deliverables, and portrait authored migration remains open.
- C02-B checkpoint 2 — investigation record for the remaining four
  configurations (Claire, 28 Nov 2026; no code changed, nothing faked):
  - Wii nunchuk_stick / nunchuk_touch: the guest inventory already
    exists (`PivotArcSoloProfiles.wiiNunchuk(pointerStick)`, native
    `WII_NUNCHUK` layouts, per-cartridge Wii controller selection), but
    the design authority (packet WII.md, GA-D31C-R2 superseded by R3)
    makes Aim & Fire a SHARED capability across Standard, PivotARC and
    standalone GripARC, in both IR modes and both orientations: one
    typed pointer/fire session below the presentation layer, a
    per-cartridge assigned guest fire key (default B), an Aim Only /
    Aim & Fire selection in Universal Controls, and the GripARC fourth
    programmable seat bound to that shared action. The missing
    interface is that shared session: today pointer contacts, guest
    digital dispatch and programmable seats are separate routes with no
    coupling owner, no per-source fire ownership, and no lifecycle
    neutralisation point. Building a GripARC-only coupling would
    violate R3 ("no separate duplicated implementations"). Next option:
    implement the shared Nunchuk pointer/fire session at the input
    routing layer first (Standard + PivotARC + GripARC together), then
    register both standalone variants with the seat maps the packet
    specifies. Recorded as a genuine blocker for C02-B, not a geometry
    gap.
  - DOS keyboard_action / mouse: the design authority (packet DOS.md,
    GA-D33) itself classifies variants B and C as "conditional
    capability designs, not currently proven registered modes". The
    only keyboard route in the current host is the DOS layout's
    THUMBL key event (keyboard icon seat); there is no verified bridge
    for arbitrary guest key down/up (Esc/Enter/modifiers/chords) or
    relative mouse motion + independent click ownership into DOSBox
    Pure from the standalone layer, and DOS_AUTO per-game mapping is
    authoritative, so no universal key ABI may be invented. Missing
    interface: a verified DOSBox Pure keyboard/mouse event bridge
    (key down/up with modifiers, relative pointer delta, click
    ownership and safe release) exposed to the touch layer. Next
    option: prove that bridge against the DOSBox Pure core with the
    packet's candidate titles, then register B and C with the packet's
    seat maps; until then the gamepad variant and keyboard entry remain
    the supported fallback, exactly as the packet prescribes.
  Register after checkpoint 2: 49/53 configurations implemented with
  dispatch; 4 recorded blocked with named missing interfaces and next
  options (above). C02-C specialist items and the C02-D register /
  explicit-minimums work remain open; no claim of C02 completion is
  made on this checkpoint.
- Formatting recovery (Claire, 28 Nov 2026, per Cassia's C02 ruling
  Assignment 1): the C02-A checkpoint failed the hosted Kotlin
  formatting gate in the new resolver / placement-helper code, which
  also blocked the connected APK build. All eight Kotlin files changed
  across C01/C02 were reformatted with the pinned ktlint (1.3.1, the
  version bundled by the repo's ktlint-gradle 12.1.0; verified clean
  under both 1.3.1 and 1.2.1 with the repo .editorconfig), including
  one manual max-line-length fix in GripArcRenderingTest. Formatting
  only: no values, logic or identifiers changed. This checkpoint is
  the formatting-only push Cassia required before further substantive
  delivery; hosted gate confirmation is hers to record.
- C02-D dimensions + C02-B dispatch evidence (Claire, 28 Nov 2026, per
  Cassia's Assignments 2 and 4): the zone resolver now carries EXPLICIT
  dimensions instead of floor artefacts — primary directional and
  analogue controls never render below 56dp, programmable GripARC
  seats are an explicit uniform 45dp, other controls keep a 42dp floor
  under authored size (the C01 GameCube resolver already had explicit
  sizes: analogue 92/66dp, D-pad 88dp, programmable 48dp). All 43
  non-GameCube landscape fixtures re-verified in simulation with the
  new dimensions (the binding constraint was Xbox RS vs programmable
  seat 4, which caps the uniform programmable size at 45dp in the zone
  architecture). Dispatch evidence for the new arcade configurations:
  `GripArcCombinedKeyMappingTest.arcadeButtonCountVariantsDispatchTheirOwnSeats`
  asserts, for ARCADE_2 and ARCADE_3, the exact numbered-seat set
  (1..N), identical seat identity and key events to the four-button
  cabinet per seat, unchanged D/Coin/Start/Menu inventory, and
  identical solo profiles on FBNeo and MAME2003Plus — i.e. GripARC
  guest dispatch (BUTTON_1 + seat number - 1) and native dispatch
  agree per seat. Portrait record (explicit, per Assignment 4):
  portrait keeps the authored uniform transform as first route; the
  legacy projection remains ONLY as a fallback for the two densities
  whose authored portrait seat maps overlap in reference space (PSP,
  Nintendo DS) — authored portrait migration requires per-console
  portrait seat redesign from the design packets and remains open,
  it is not silently claimed. C02-C specialist integration is the
  remaining independently achievable C02 item after this checkpoint.
