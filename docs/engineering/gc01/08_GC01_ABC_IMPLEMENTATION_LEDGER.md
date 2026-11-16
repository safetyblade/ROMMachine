# GC-01 A–C implementation and verification ledger

This is the existing GC-01 lane and draft PR #294. No protected merge or release.
The owner-tested regression/rollback reference remains `f6d3fd66b372a514cebc23fa28e9569f27c4b66c`,
run [43805557009](https://github.com/safetyblade/ROMMachine/actions/runs/43805557009),
artifact `14197824895`: 29/29 owner-reported interim checks passed. This is not independent QA.

The engineering branch reconciles accepted runtime source `9ba74868` with controller/CI
handoff `84bebfae` in `4e750d3f`. Accepted Wii U source fixes are retained.
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

## Requirement ledger

| Criterion | Implemented source / verification | Acceptance status |
| --- | --- | --- |
| A1 common menu contract across Libretro/native hosts | Shared capability profiles/adapters; genuine Resume, Save & Load guidance/actions, Controls, universal Display, supported Game Settings and safe Shelf exit. Unsupported operations are omitted/disabled/explained. | Source implemented; current correction CI pending |
| A1 every nested page compact | Shared 56dp minimum rectangular row, focus border, theme materials; preference links/switches use the same row in menu scope. Save slots, Controls, Display, Game Settings, Advanced and Troubleshooting inherit it. | Automated screenshot/row checks pending current CI |
| A1 normal header | Page, shortened game title, console and Back. No engine hashes or pause internals. | Regression assertions added |
| A1 / C6 Advanced separation | Concise Advanced links; raw engine build/pause information and detailed core settings live in Diagnostics. Genuine recovery remains reachable. | Regression assertions added |
| A2 branded loading | Phase, available game/system identity, fixed visible Cancel, safe insets and scrollable content. | Earlier full suite passed; latest menu increment pending CI |
| A2 cancellation and failure recovery | One preparation job, idempotent cancellation, cancel-and-join before shutdown/exit, safe native boundaries, no late staging relaunch. Recoverable native failure routes retained. | LaunchPreparation race tests pass; physical rotation/background/write cases pending |
| A3 invocation and suppression | Delivered Menu/Guide, touch entry, Android Back, TV route and configurable L3+R3 fallback. Matched press/release and per-device chord ownership; unmatched releases cannot close a newly opened menu. | Unit tests pass; physical OS delivery/host matrix pending |
| B1 landscape cartridge detail | Side-by-side art and scrollable compact actions; whole original art fits measured bounds. Portrait Open/Setup stay reachable. | Bounds/artwork/Open/Setup regression tests pass |
| B2 library bounds | Content-height cap, reduced density/font-scale response, existing Scaffold/system insets and lazy focus/scroll paths; no library schema/order migration. | Responsive policy, shelf facing/wrapping/drag tests pass in prior 541-test suite; latest correction rerun pending |
| B3 gameplay display | NativeViewportPolicy/renderer ownership retained. Decorative border changes do not request stretch/crop. Existing DS/Wii U touch geometry and TV clearance retained. Physical-pad touch controls hide and restore on disconnect. | Automated geometry tests; physical console/rotation/touch matrix pending |
| Display all entry points | Global Settings > Display; every Cartridge Setup > Display; all native/Libretro game menus > Display. | Shared model/route tests pass |
| Display precedence and reset | Explicit UUID-keyed ROM fields > global fields > legacy/safe defaults; Use Global Default deletes only the display override. | Round-trip, reset, corrupt/future-file and multi-writer tests pass |
| Display Border | Unset landscape Off; portrait Standard unchanged. Off/Standard persisted independently, immediate presentation update without game restart. No Ornamental control. | Default/override tests pass; physical On/Off checks pending |
| Display supported capabilities | Original aspect retained; real reported core options include DS layouts and other existing display choices. Metadata scoped to selected core and installed app version; values revalidated against the running core. Unsupported native controls explain limitations. | Catalog/namespace/value validation tests pass; current increment CI pending |
| Display live/restart behavior | Border presentation observed across processes. Existing core-variable update boundary applies supported choices on return; renderer-only options persist for next launch. No automatic game restart. | Persistence tests pass; physical core behavior pending |
| C1 identification | Descriptor, VID/PID, sources, known family evidence, reported buttons/motion ranges; remotes with only DPAD do not qualify. Generic/uncertain layouts receive testing/calibration guidance. | Hardware-profile and remote-exclusion tests pass; actual devices pending |
| C1 normalization | Physical face/DPAD/stick/press/shoulder/trigger/start/select/menu vocabulary; RX/RY versus Z/RZ; BRAKE/GAS trigger aliases; hat edges; driver ranges/deadzones and duplicate logical trigger ownership. | Translation/range/trigger tests pass |
| C2 authentic defaults | Existing valid user mappings restored; intentional RetroPad face translations retained. Default remains Console Authentic. No automatic Modern Function presets. | Mapping/key translation tests pass |
| C2 guided calibration | Select physical position and capture its authentic output; test first on unknown pads. Neutral/deadzone/right-axis calibration and warnings use actual observed input. | Source implemented; owner must verify ambiguous physical positions |
| C3 unified editing routes | Settings > Controllers, Cartridge > Controls and game-menu Controls share the editor/store architecture. Xbox legacy maps are read as the initial map and retained until explicit Save/reset. | Current unified Xbox regression CI pending |
| C3 editing and rollback | Device/ROM scope, source-to-console output labels, explicit Save, conflict review/swap, Undo, Reset draft, restore saved/previous mapping, protected Menu/Back. | Draft/profile round-trip and rollback tests pass; current scope-cache correction pending CI |
| C4 live tester | Live buttons/chords, normalized axes and trigger meters, raw diagnostics only on explicit tester, reported/observed capabilities, prolonged/repeated-input warnings and calibrated output. | Probe/normalizer tests pass; hardware exercise pending |
| C5 ownership | Descriptor-based saved preferences, distinct runtime IDs/ports, explicit reassignment, reconnect retention, per-ROM overrides and legacy compatibility. Unowned native P2 input is not folded into P1. | Port swap/reconnect/collision and empty-device tests pass |
| C5 native port limits | Native attachment is capability-based; some current JNI hosts expose only P1. Additional unavailable ports are not claimed as attached. Descriptor collisions require reconnect verification instead of invented durable identity. | Explicit limitation; physical multi-pad matrix pending |
| C7 consistent navigation | Shared compact focus rows, DPAD and hat navigation, Android logical Confirm/Back, matched escape edges, touch fallback and safe Shelf exit. | Menu/hat tests pass; controller-only physical end-to-end pending |
| Identity/save/library safety | Additive presentation/controller files only; no SaveBinding, cartridge UUID, Room schema, ROM relocation, save/memory-card or signing-identity migration. Existing collections/art/favourites and engine layouts retained. | Identity/save/artwork regression tests pass; install-over physical check pending |

## Evidence and CI

Before the latest P0 nested-menu correction, the full local suite passed **541 tests**;
all **169 Python CI/source tests** passed. The latest correction must pass the complete
suite, lint and connected APK gates before delivery; these earlier counts are not
acceptance of subsequent source edits.

Ten retained native inputs were reverified against the current recipe identities.
Nine are reused. Only Cemu/Vulkan was rebuilt for accepted Wii U native-source fixes:
[43846998492](https://github.com/safetyblade/ROMMachine/actions/runs/43846998492), source
`291e1520e222fd9cd50a1081ab66e3b9bb3b6b40`, artifact `14215629161`, archive SHA-256
`78a0b964709d0a094dcad25f8ee3a4a6861fb89e7e57db5be627e7e625d43fb8`.
Receipts retain source pins, toolchain/ABI identities, all output checksums and notices.
See retained `work/gc01-abc/current-native-inventory.json` for the ten-component ledger.

CI-01 FAST_TEST_APK/FULL_INTEGRATION_APK remains the opt-in acceptance path. No normal-path
promotion and no implicit full native rebuild. Current APK workflow/artifact/checksum and
actual timings are pending the final exact-source run.

Rendered menu images are automated production-component fixtures, not physical acceptance
or screenshots from a native game session. Owner screenshots referenced in the amendment
were not attached in the current workspace/issue attachment links, so pixel-for-pixel
physical comparison is not claimed.

## Focused physical acceptance still required

Jonathan: install over the baseline without clearing data; library/art/favourites and saves;
Xbox Controls/Advanced screenshots in both orientations; every menu's focus/Back/escape;
Display before launch and in-game on GB/GBA/PS1/PS2/Wii U/Xbox/Xbox 360; global override/reset;
landscape Off/Standard switching and portrait continuity; DS/Wii U touch alignment;
controller family/button/axis calibration, mapping Save/Undo/reset/rollback and reopen;
P1/P2 and same-model disconnect/reconnect; loading cancel during staging/initialization,
repeated Back, rotation and backgrounding; full landscape cartridge bounds and short shelves.

Marcus must independently review evidence and record QA findings. No physical or independent
acceptance is asserted here. No A–C completion claim is made solely from mapping-row presence.
