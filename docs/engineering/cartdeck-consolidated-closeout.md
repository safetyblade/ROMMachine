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
  Hosted/targeted test execution is recorded on PR #385 with the stage
  return; no APK was built at this stage, per protocol.
