# GC-01 A–C implementation and verification ledger

This is the existing GC-01 lane and draft PR #294. No protected merge or release.
The owner-tested regression/rollback reference remains `f6d3fd66b372a514cebc23fa28e9569f27c4b66c`,
run [43805557009](https://github.com/safetyblade/ROMMachine/actions/runs/43805557009),
artifact `14197824895`: 29/29 owner-reported interim checks passed. This is not independent QA.

The engineering branch retains its existing GC implementation and integrates accepted
PERF PR #298 (`7196ab242bd3e7e053ea144e6170ba89d8c0d2fa`) through normal merge
`f19fc2eb7b574bdfb366cf0022687cf12b6c34f4`. Cemu loaded-core verification, Xenia
buffered telemetry, strict native reuse, APK allowlists and PS3 distribution controls are retained.
PS3 packaging, Junior implementation, GC-01D/E/F/H, save/profile migrations and emulator
upgrades are excluded. PS3's separately controlled seven-port source remains intact.

## Source checkpoints

- `ac109b8a`: visible loading cancellation, single preparation owner and cleanup boundaries.
- `04fdeac5`: compact home menu and shared global/cartridge/gameplay Display model.
- `291e1520`: adaptive landscape detail and height-constrained shelves.
- `24f0ee27`: normalized input, live tester/calibration, persistent mappings and port ownership.
- `34eded4b`: native hat-switch navigation in focused menus.
- Current correction: compact nested preferences and mapping rows, readable labels,
  Advanced > Diagnostics, unified Xbox editor with legacy-map preservation, engine-reported
  display arrangements, fixed loading Cancel placement and remaining regression corrections.
- `5238f770`: retain TV/legacy cartridge Setup > Display/Controls routes using the same
  stable cartridge identity; use truthful host port capacity; shorten registered ROM suffixes;
  reserve delivered Guide events before legacy input remapping.
- `ac9a5067`: SDK 34 fixture supplies Android 14 InputManagerGlobal queries. The original
  SDK-only correction removed NoClassDefFoundError but exposed a controller-discovery timeout;
  the complete focused fixture now passes without weakening assertions.
- `0c003bf96f4e32e5824882a8bae82535421a77d0`: verified implementation head (line-wrap correction).

## Requirement ledger

| Criterion | Implemented source / verification | Acceptance status |
| --- | --- | --- |
| A1 common menu contract across Libretro/native hosts | Shared capability profiles/adapters; genuine Resume, Save & Load guidance/actions, Controls, universal Display, supported Game Settings and safe Shelf exit. Unsupported operations are omitted/disabled/explained. | Automated host/menu tests pass; physical host behavior pending |
| A1 every nested page compact | Shared 56dp minimum rectangular row, focus border, theme materials; preference links/switches use the same row in menu scope. Save slots, Controls, Display, Game Settings, Advanced and Troubleshooting inherit it. | Automated portrait/landscape Xbox Controls/Advanced render and 52–60dp row checks pass |
| A1 normal header | Page, shortened game title, console and Back. No engine hashes or pause internals. | Regression assertions pass |
| A1 / C6 Advanced separation | Concise Advanced links; raw engine build/pause information and detailed core settings live in Diagnostics. Genuine recovery remains reachable. | Regression assertions pass |
| A2 branded loading | Phase, available game/system identity, fixed visible Cancel, safe insets and scrollable content. | Loading UI and cancellation tests pass; physical lifecycle cases pending |
| A2 cancellation and failure recovery | One preparation job, idempotent cancellation, cancel-and-join before shutdown/exit, safe native boundaries, no late staging relaunch. Recoverable native failure routes retained. | LaunchPreparation race tests pass; physical rotation/background/write cases pending |
| A3 invocation and suppression | Delivered Menu/Guide, touch entry, Android Back, TV route and configurable L3+R3 fallback. Matched press/release and per-device chord ownership; unmatched releases cannot close a newly opened menu. | Unit tests pass; physical OS delivery/host matrix pending |
| B1 landscape cartridge detail | Side-by-side art and scrollable compact actions; whole original art fits measured bounds. Portrait Open/Setup stay reachable. | Bounds/artwork/Open/Setup regression tests pass |
| B2 library bounds | Content-height cap, reduced density/font-scale response, existing Scaffold/system insets and lazy focus/scroll paths; no library schema/order migration. | Responsive policy, shelf facing/wrapping/drag tests pass in final 546-test suite |
| B3 gameplay display | NativeViewportPolicy/renderer ownership retained. Decorative border changes do not request stretch/crop. Existing DS/Wii U touch geometry and TV clearance retained. Physical-pad touch controls hide and restore on disconnect. | Automated geometry tests; physical console/rotation/touch matrix pending |
| Display all entry points | Global Settings > Display; every Cartridge Setup > Display; all native/Libretro game menus > Display. | Shared model/route tests pass, including all registered consoles in legacy/TV cartridge routes |
| Display precedence and reset | Explicit UUID-keyed ROM fields > global fields > legacy/safe defaults; Use Global Default deletes only the display override. | Round-trip, reset, corrupt/future-file and multi-writer tests pass |
| Display Border | Unset landscape Off; portrait Standard unchanged. Off/Standard persisted independently, immediate presentation update without game restart. No Ornamental control. | Default/override tests pass; physical On/Off checks pending |
| Display supported capabilities | Original aspect retained; real reported core options include DS layouts and other existing display choices. Metadata scoped to selected core and installed app version; values revalidated against the running core. Unsupported native controls explain limitations. | Catalog/namespace/value validation tests pass |
| Display live/restart behavior | Border presentation observed across processes. Existing core-variable update boundary applies supported choices on return; renderer-only options persist for next launch. No automatic game restart. | Persistence tests pass; physical core behavior pending |
| C1 identification | Descriptor, VID/PID, sources, known family evidence, reported buttons/motion ranges; remotes with only DPAD do not qualify. Generic/uncertain layouts receive testing/calibration guidance. | Hardware-profile and remote-exclusion tests pass; actual devices pending |
| C1 normalization | Physical face/DPAD/stick/press/shoulder/trigger/start/select/menu vocabulary; RX/RY versus Z/RZ; BRAKE/GAS trigger aliases; hat edges; driver ranges/deadzones and duplicate logical trigger ownership. | Translation/range/trigger tests pass |
| C2 authentic defaults | Existing valid user mappings restored; intentional RetroPad face translations retained. Default remains Console Authentic. No automatic Modern Function presets. | Mapping/key translation tests pass |
| C2 guided calibration | Select physical position and capture its authentic output; test first on unknown pads. Neutral/deadzone/right-axis calibration and warnings use actual observed input. | Source implemented; owner must verify ambiguous physical positions |
| C3 unified editing routes | Settings > Controllers, Cartridge > Controls and game-menu Controls share the editor/store architecture. Xbox legacy maps are read as the initial map and retained until explicit Save/reset. | Unified Xbox editor render and labels pass; physical mapping pending |
| C3 editing and rollback | Device/ROM scope, source-to-console output labels, explicit Save, conflict review/swap, Undo, Reset draft, restore saved/previous mapping, protected Menu/Back. | Draft/profile round-trip, rollback and scope-cache regressions pass |
| C4 live tester | Live buttons/chords, normalized axes and trigger meters, raw diagnostics only on explicit tester, reported/observed capabilities, prolonged/repeated-input warnings and calibrated output. | Probe/normalizer tests pass; hardware exercise pending |
| C5 ownership | Descriptor-based saved preferences, distinct runtime IDs/ports, explicit reassignment, reconnect retention, per-ROM overrides and legacy compatibility. Unowned native P2 input is not folded into P1. | Port swap/reconnect/collision and empty-device tests pass |
| C5 native port limits | Native attachment is capability-based; some current JNI hosts expose only P1. Additional unavailable ports are not claimed as attached. Descriptor collisions require reconnect verification instead of invented durable identity. | Explicit limitation; physical multi-pad matrix pending |
| C7 consistent navigation | Shared compact focus rows, DPAD and hat navigation, Android logical Confirm/Back, matched escape edges, touch fallback and safe Shelf exit. | Menu/hat tests pass; controller-only physical end-to-end pending |
| Identity/save/library safety | Additive presentation/controller files only; no SaveBinding, cartridge UUID, Room schema, ROM relocation, save/memory-card or signing-identity migration. Existing collections/art/favourites and engine layouts retained. | Identity/save/artwork regression tests pass; install-over physical check pending |

## GC-F1 — automated verification

Verified implementation head: `0c003bf96f4e32e5824882a8bae82535421a77d0`.
GitHub PR checkout: `8db4ce42d9c0a36d5bfdeeee592e3ac6ad441d5d`.
Both have tree `e08624c6887e4c14e3ab33ba34a3da828e038cde`; the synthetic PR checkout
contains no extra application change.

[Run 44009495057](https://github.com/safetyblade/ROMMachine/actions/runs/44009495057):
whole-project `ktlintCheck` passed; **546 Kotlin tests passed, 0 failures/errors/skipped**
across 128 suites; **173 Python CI tests passed**. The local full suites also passed.
Gradle used standaloneId, phoneArm64Only, p1emu, in-process Kotlin, no configuration cache
and two workers. Recursive `lemuroid-cores` stayed pinned at `fee2e824525daa22bcf318f96127fe43fa8a15ad`.
Of these Kotlin results, 43 relevant menu/Display/controller/loading/cartridge/shelf/viewport/
save-binding suites account for 190 passing tests. Counts are actual JUnit XML totals.

The first integrated lint attempt found one overlong Controls intent line. It was wrapped
in `0c003bf9`; no test was disabled or assertion weakened. Superseded run 44009321524
was cancelled. Fresh api.github.com and jitpack.io requests returned HTTP 200; GitHub
authentication succeeded without printing credentials, and the branch workflow executed.

## GC-F2 — selective APK evidence

F2 configuration: `include_ps3=false`, `use_ci01_all_native=true`,
`measure_apk_reuse=false`, `publish_apk=true`. This is the existing gated FAST-equivalent
path; no native full rebuild is authorised and reuse is not promoted to a new normal path.

All ten immutable native receipts pass against the integrated source recipe, toolchain,
ABI and output checksums. **Ten reused, zero rebuilt during this closure.** Cemu/Xenia
are reused from successful PERF run 43947615252 after strict verification, rather than
reusing the older GC/pre-fix artifacts. Cemu atomic64 repair attestation passes.
Native pins, producer runs, artifact IDs, archive/receipt/native checksums are recorded in
`GC01_CLOSURE_NATIVE_RECEIPTS.json`; licence notices and corresponding-source bundles
remain in the verified producer artifacts and existing packaging path.

Verified candidate: [APK artifact 14296792285](https://github.com/safetyblade/ROMMachine/actions/runs/44009495057/artifacts/14296792285),
`CARTDECK_CONNECTED_PS2_WIIU_X360_ARM64_DEBUG`, **196,928,299 APK bytes**.
APK SHA-256: `a0af941f23fcb9c49645dfec9cdd2982b25ec3cee3f4f5842bd32b579753a38f`.
Artifact ZIP SHA-256: `8f369608e600bc14b0c0af5ec4def81f884038cc2a4d02299819e11cc5c1db15`
(independently downloaded and matched against GitHub's digest).

Package `dev.prestwich.cartdeck.debug`, versionCode **90829006** (> baseline 90747016),
versionName `1.17.0-DEBUG`; signing certificate SHA-256
`ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8` matches the baseline.
Source stamp is the exact CI checkout `8db4ce42d9c0a36d5bfdeeee592e3ac6ad441d5d`.
All **42 native libraries are ARM64 and pass 16 KB PT_LOAD alignment**. Stored native ZIP
entries also have a mandatory 16 KB check (this APK has zero stored native entries).
CI verified 29 bundled payload files, the patched LibretroDroid host, five legacy/modern
engines, Wii, hakuX, Xenia, Cemu/Vulkan and both ARMSX2 page models. Independent local
verification matched all ten components to the delivered native hashes. For ARMSX2,
the established `llvm-strip --strip-unneeded` step changes file hashes: both original and
packaged hashes are retained, BuildIDs are preserved by CI, and every loadable segment's
bytes were independently compared (excluding ELF section-table metadata in the ELF header).
No native engine recompilation is involved.

The connected APK repeated **546 Kotlin tests with 0 failures/errors/skips** before delivery.
Evidence: F1 artifact `14295867962`; unit/UI render artifact `14296388625`;
exact native crash-symbol artifact `14296657416` in the same successful run.
The one measured checks/assembly invocation took **357.084 seconds**, with dependency
caches retained and 2,751 native input files unchanged. This is **not a cold/warm APK
benchmark**; `measure_apk_reuse=false` was requested.
`GC01_CLOSURE_APK_VERIFICATION.json` retains machine-readable package, source, checksum,
native, test and timing evidence.

Engineering recommendation: **candidate ready for owner physical acceptance**.
This does not close Jonathan's hardware acceptance or Marcus's independent QA gate.


## GC-F3 — functional regression scope and limitations

The requirement ledger above accounts for A/B/C and universal Display. All referenced
automated regression suites pass on the implementation tree. `Game.kt` and
`RetrogradeDatabase.kt` are byte-identical to the owner-tested f6d3fd66 baseline;
Room remains v13 and no save, SaveBinding, ROM, UUID or memory-card migration is introduced.

Rendered menu images exercise production components under Robolectric. They are not
physical screenshots or native gameplay validation. Xbox Controls/Advanced were visually
inspected in the generated landscape renders: compact rows, readable labels, concise
Advanced, no header hash/pause internals. Portrait, landscape and theme captures are
retained by CI. Owner screenshots referenced earlier are not attached in the current
workspace, so a pixel-for-pixel owner screenshot comparison is not claimed.

Hardware acceptance remains open for Android-delivered Guide/Home differences, uncertain
physical button identities, same-descriptor reconnect identity, engine pause/cleanup,
DS/Wii U touch alignment, real TV focus and install-over save continuity. Some native JNI
adapters expose P1 only; additional attached native ports are not invented. PS3 is excluded
and its seven-port source and distribution-clearance gate remain separately controlled.
No cold/warm APK benchmark is claimed: reuse measurement is explicitly disabled for F2.

## Focused physical acceptance still required

Jonathan on Samsung: install over the baseline without clearing data; library/art/favourites and saves;
Xbox Controls/Advanced screenshots in both orientations; every menu's focus/Back/escape;
Display before launch and in-game on GB/GBA/PS1/PS2/Wii U/Xbox/Xbox 360; global override/reset;
landscape Off/Standard switching and portrait continuity; DS/Wii U touch alignment;
controller family/button/axis calibration, mapping Save/Undo/reset/rollback and reopen;
P1/P2 and same-model disconnect/reconnect; loading cancel during staging/initialization,
repeated Back, rotation and backgrounding; full landscape cartridge bounds and short shelves.

Android TV: repeat library/cartridge Setup and in-game Display/Controls with remote/gamepad
DPAD, Confirm and Back; verify selected focus stays visible, controller-only Resume/exit,
landscape safe areas, loading Cancel and disconnect touch/accessibility fallback.

Marcus must independently review evidence and record QA findings. No physical or independent
acceptance is asserted here. No A–C completion claim is made solely from mapping-row presence.

## Rollback and acceptance boundary

Baseline APK SHA-256: `9669c7e0eeae5c64c7b8f7c353efb7ba9a063e92b5cb28a05d0494198ee7f97e`,
versionCode `90747016`, package `dev.prestwich.cartdeck.debug`, certificate SHA-256
`ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8`.
Retain run 43805557009/artifact 14197824895 and the approved rollback workflow. Do not
uninstall, clear app data or force a lower-version install merely to demonstrate rollback.
This branch remains a draft engineering candidate; no protected merge, release publication,
owner physical acceptance or independent Marcus acceptance is performed here.
