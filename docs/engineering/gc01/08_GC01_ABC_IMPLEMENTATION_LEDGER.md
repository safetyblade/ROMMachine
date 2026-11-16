# GC-01 A–C implementation ledger

Owner-tested regression baseline: f6d3fd66 / run 43805557009 / artifact 14197824895;
29/29 owner-reported interim checks passed. This is not independent acceptance.
Engineering base reconciles accepted master 9ba74868 with accepted controller/CI
handoff 84bebfae. Newer accepted Wii U source fixes remain intact. No renderer
geometry, engine pins, save identity/schema or Junior runtime changes are authorised.
PS3 stays outside this APK lane; its existing seven-port source remains preserved.

| Requirement | Source / automated evidence | Status |
| --- | --- | --- |
| A2 loading cancel/recovery (first increment) | Shared LaunchPreparation owns one job; Cancel/Back waits for staging cleanup; Libretro, PS2, Cemu, Xenia and Xbox staging/loader integration. Atomic-boundary, repeated-cancel and late-start tests pass; shared menu tests pass. | Source implemented; device QA pending |
| A1 compact universal menu / C6 settings separation | Existing shared adapters retained; responsive compact rows and supported destinations still being completed. | In progress |
| A3 / C7 menu invocation and navigation | Existing matched-edge and ownership primitives retained; universal configurable chord and focus integration still being completed. | In progress |
| B1 adaptive cartridge detail | Existing art and Open/Setup retained. | In progress |
| B2 safe shelf viewport | Existing sections/order/art retained. | In progress |
| B3 gameplay viewport | Owner-tested renderer geometry retained; regression tests to run with integrated source. | Verification pending |
| C1 hardware detection / normalization | Existing capability profile and probe reviewed; complete axes/family/event normalization still being completed. | In progress |
| C2 authentic mapping / calibration | Existing RetroPad positional swaps preserved; no Modern Function defaults. | In progress |
| C3 management UI / C4 live tester | Existing draft/conflict/undo primitives retained; unified routes and live calibrated editor still being completed. | In progress |
| C5 ownership / persistence | Existing port registry and descriptor preferences preserved; additive cartridge/default/calibration persistence still being completed. | In progress |

No A–C completion or physical pass is claimed by this progress ledger. Every row
must be closed or explicitly blocked in the final report. No GC-01D/E/F/H, profiles,
Junior, GEM, CARTMOD or PS3 packaging work is included.
