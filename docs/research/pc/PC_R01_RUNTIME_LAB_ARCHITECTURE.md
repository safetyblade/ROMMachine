# PC-R01 — PC runtime / lab architecture boundary

Programme: PC-00 (#299), PERF-PC substream of PERF (CONTROL-03 #304).
Gate: **PC-R01** — the independent runtime/lab architecture that every later
PC gate (R02–R07) builds inside. Controlling sources: #299 (architecture —
non-negotiable; failure record; recovery amendment 17 Nov 2026) and #305
("Resume at PC-R01, not old PR #306 or the merged shared-host patch").
Audited state as of: 2026-11-28, ROMMachine master
`1e2e75c80bc3f30cd873136edee8472e6a37d6ff`.

This document defines the boundary. It integrates **nothing** into the
production runtime, claims **no guest execution**, and changes no code.

## Why this gate exists — the failure it answers

#299 records the failure this architecture prevents from recurring:

- The PC-F4 keyboard/mouse bridge modified the **shared** LibretroDroid
  console host patch and recipe; the master APK then rejected the outdated
  immutable host receipt (run 44065484302).
- Rebuilding that host exposed patch-application failures against pinned
  LibretroDroid `8835c309…` (`input.cpp`, `environment.h`; run 44066726783).
- Isolation commit `f3e6ca2e` restored the verified console recipe. The PC
  experiment was preserved under `spikes/pc/native-host/` with its own
  workflow (`.github/workflows/cartdeck-pc-native-lab.yml`); the standard
  APK's PC-native input protocol is disabled. **The lab patch remains
  broken, not a working engine** (#299, verbatim status).

Lesson, stated as a rule: PC work must never again be able to invalidate a
console artifact, receipt or save. Isolation is the architecture, not a
phase.

## The lab — what it is

The **lab** is everything PC-experimental that is not yet promoted:

- Source spikes under `spikes/pc/` (today: `native-host/` experiment,
  `verify_legacy_core.py`, `audit_core_binary.py`, `audit_wine_reference.py`,
  `pc04_sources.json` / `pcr02_sources.json` provenance records, and the
  mechanical verify scripts + synthetic tests of the R02/R03 gates).
- The dedicated lab workflow `.github/workflows/cartdeck-pc-native-lab.yml`,
  separate from the standard APK workflows.
- Any lab APK build. A lab APK **must** carry an isolated install ID and
  isolated storage and **cannot overwrite production data** (#299).

The lab is where engines are audited, patched, built and first executed.
Nothing in the lab is user-facing product, and no lab output is evidence
about the production app until its gate says so.

## The boundary — lab-only vs promotable

| Concern | Lab-only (never ships as-is) | Promotable (after its gate) |
|---|---|---|
| Host / native code | PC patches against the shared console host; the `native-host/` experiment | A PC runtime built as its **own** adapter with PC-specific patches, dependencies and protocol version |
| Build inputs | Spike scripts, one-off build helpers (`spikes/pc/build_pc_core.sh`) | PC-specific build inputs in the selective CI-01 manifest, with PC artifact namespaces and PC receipts |
| Artifacts | Lab APKs, lab receipts | Bundled runtime inside the **one** production CARTDECK APK (no external emulator installer, downloader or storefront — #299) |
| Identity / data | Lab install ID, lab storage | Cartridge UUIDs, profiles, guest/install data preserved across independent rollback (#299) |
| Capabilities | Anything the lab binary happens to do | Only capabilities the **selected runtime advertises as a verified protocol**; unknown capabilities fail closed with a truthful diagnostic |

Hard rules (from #299, non-negotiable):

1. Protect the established console host, its patches/ABI/protocol, engine
   receipts and native selection, controller fixes, normal library/ISO
   routing, APK identity/signing and existing saves. PC failures cannot
   block normal APK assembly or invalidate unrelated console receipts.
2. No replacing the console AAR; no using its Maven coordinate for a PC
   production artifact; no weakening receipt checks; **no automatically
   promoting lab outputs to master**.
3. PC input is enabled only after the actual selected runtime advertises a
   verified protocol. Runtime lifecycle, input and display isolation are
   required.
4. Selected, licence-cleared engines ship **bundled with CARTDECK**;
   the engine-only external repository is REL-ENG-01 (#307) and is a
   release artefact, not a runtime dependency and not proof of CARTDECK's
   GPL corresponding-source duties.
5. Only user-provided lawful game installers/files/discs. No bundled
   Windows or game images, no rights bypass, no unsupported compatibility
   claim.

## Graduation — how a core/system crosses the boundary

A PC core/system graduates by passing the gate chain in order; each gate
produces evidence the next gate consumes. Skipping a gate is how the
PC-F4 failure happened.

| Gate | Question it answers | Evidence artefact |
|---|---|---|
| **PC-R01** | Is the runtime/lab boundary defined and enforced? | This document; lab isolation in tree (spikes, lab workflow, disabled PC-native input in the standard APK) |
| **PC-R02** | Is the actual shipped binary what we think it is — identity, licence, ABI, API surface, capability? | Binary audit + machine record + mechanical verify script (DOSBox Pure: PR #386) |
| **PC-R03** | Does a synthetic guest payload execute end-to-end on the real path? | Synthetic `.dosz` package + source-level package/registry verification (this PR, preparation); **on-device execution proof is the gate's remaining half** |
| **PC-R04** | Does the real target work — persistent Win95/98 guest + Shandalar — with owner media? | Device test record: install → boot → game → save → exit → reopen (#305 PC-04C sequence) |
| **PC-R05** | Is it integrated as product — Shelf/CARTMOD, cartridge identity, input, profiles, SafeSave? | Integration checkpoint (`PC_R05_INTEGRATION_CHECKPOINT.md`) + product code behind the capability boundary |
| **PC-R06** | What does the parallel modern route (Wine/Box64/FEX) contribute? | Research records (`PC_R06B_COMBINED_RUNTIME.md`); research only, never a shipping claim |
| **PC-R07** | May this runtime ship? | Owner **GO / CONDITIONAL / NO-GO per runtime** (#299 research gate R5); promotion is an owner act, never automatic |

Cross-reference: #299's research sequence (R0 source/native audit → R1
source/licence/ABI candidates → R2 architecture → R3 media/install/save
design → R4 game/device test plan → R5 owner decision) maps onto
R02 → R02 → R01 → R03/R05 → R04 → R07 above; the PC-R01–R07 naming is the
owner-approved roadmap naming of the #305 recovery amendment.

## Gate status after this morning (2026-11-28)

| Gate | Status | Basis |
|---|---|---|
| PC-R01 | **DEFINED by this document** — boundary and graduation chain recorded; enforcement mechanisms already in tree from the `f3e6ca2e` isolation. Owner acceptance of this document is a separate act. | #299 architecture + failure record |
| PC-R02 | **DONE, pending owner/QA review** — DOSBox Pure audit landed as draft PR #386: binary identity/licence/ELF/API PASS; upstream source revision UNPROVABLE (→ #307); final-APK inclusion NOT PROVEN (G2 build gate). | PR #386 |
| PC-R03 | **PREPARATION landed with this PR** — synthetic `.dosz` package builder + package/registry verification script + tests. **On-device synthetic guest execution: NOT DONE, NOT CLAIMED.** | This PR |
| PC-R04 | **OPEN** — requires lawful owner-supplied Win95/98 + Shandalar media; out of scope of any unattended run. Gameplay NOT TESTED (#305). | #305 |
| PC-R05 | **OPEN** — integration checkpoint exists; no product integration of a PC runtime has been promoted. | `PC_R05_INTEGRATION_CHECKPOINT.md` |
| PC-R06 | **OPEN (research)** — Wine/Box64 combined-runtime research recorded; Winlator v11.2.0 is technical reference only (#305). | `PC_R06B_COMBINED_RUNTIME.md` |
| PC-R07 | **BLOCKED** — no runtime has the R03-device + R04 proofs a promotion decision needs; Decision per runtime remains the owner's. | #299 |

## What this gate does not decide

- It does not repair the broken `native-host/` lab patch; that is lab work
  inside the boundary, sequenced by later gates.
- It does not select Wine/Box64 over DOSBox Pure (or vice versa); R06 is
  research, R04 is the first gameplay proof, DOSBox Pure first (#305).
- It does not prove the DOSBox Pure binary lands in the final assembled
  APK — that remains the G2 build gate recorded by PC-R02.
