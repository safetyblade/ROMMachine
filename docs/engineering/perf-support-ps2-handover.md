# CARTDECK Support and PS2 delivery handover

PERF-SUPPORT-01 and PERF-PS2-UX-01 extend the existing [IMP-001 work](https://github.com/safetyblade/ROMMachine/issues/329). The source baseline is master `36939cfc03a23d5b87950ab68053a012b0eef151`. Libretro evidence comes from the on-demand adapter merged in [#331](https://github.com/safetyblade/ROMMachine/pull/331). Native evidence comes from the existing engine reports and session journal. The draft introduces a shared application-side session checkpoint and report schema. The existing engine registry, managed process locks, save leases, engine-specific save paths, and promotion gates remain the launch and storage authorities.

## Implementation register

| Package | Candidate coverage | Acceptance still required |
| --- | --- | --- |
| SUP-01 | Shelf-side failure routing, private engine PID and launch interval matching, interrupted-process recovery, explicit teardown deadline for integrated hosts | Inject active hangs and interrupted cleanup on physical devices, verify shelf access and another launch. A bare in-game hang still depends on Android delivering an exit or the player initiating teardown. |
| SUP-02 | Shared SIGNATURE recovery view with direct copy, report, advanced diagnostics and shelf actions. Play! offers explicit retry and conditional Enhanced Compatibility. | Device renders and controller/remote focus verification. Context-sensitive recovery is partial across failure categories and hosts. |
| SUP-03 | Versioned report and local history, Settings intake, Libretro, the shared native gameplay menus and PC. Bounded traces, build revision, structured identity and optional user-selected screenshot. | More subsystem adapters, PC runtime-specific metrics, structured effective configuration, and physical preview/privacy acceptance. |
| SUP-04A | Android email chooser with ZIP and a separate text-only fallback. Central recipient is `safetybladegaming@gmail.com`. | Recipient and attachment verification in representative email clients. |
| SUP-04B | Offline copy from recovery, Libretro Diagnostics, and support report views. | Physical clipboard and controller verification. Host-specific developer probe actions remain available. |
| SUP-04C | Bounded ZIP with readable and JSON entries, SAF file export and read-only FileProvider sharing. | Device picker and share target verification, cancellation and retention behavior. |
| SUP-05 | The private HTTPS client contract is wired behind an empty verified endpoint. Production submission is disabled. | Approved intake URL, private attachment storage, server-side idempotency and abuse controls, duplicate grouping, owner notification and a verified acknowledgement from the real endpoint. |
| PS2-01 | First launch prompt, existing validated-copy importer, library folder discovery and single-candidate import confirmation. | Device testing with valid, invalid, missing and replaced BIOS, lost permission, and providers with different SAF capabilities. |
| PS2-02 | Automatic, Quick Play and Enhanced Compatibility labels and preferences. Existing engine continuity precedes the default for new cartridges. Explicit Play! retry and alternative are supported. | Full switch/reopen and save or memory-card evidence. ARMSX2 gates in #218 and #232 still apply. |
| PS2-03 | Pinned build and upstream source assessment below. | BR-003 reproduction, ANR root cause, baseline performance and any controlled engine change. |
| PS2-04 | Small offline GEM advisory set, exact PS2 ISO serial when detected, title-only suggestions when the serial is unavailable. | Broader data coverage and physical compatibility observations. CHD, CSO and ISZ serials remain uncaptured by the ISO reader. |
| QA-01 | Source checks and automated regression coverage are tracked by the draft PR. | Exact merge-tree APK, install-over, screenshots, hardware input/rotation matrix and independent QA. |

## Engine coverage

| Route | Checkpoint and failed launch | Live evidence |
| --- | --- | --- |
| Legacy and modern Libretro, including Dreamcast, Dolphin and PSP | Shared private `:game` route with captured PID | Existing IMP-001 on-demand Libretro adapter |
| Play! | Existing PS2 stage journal and shelf-process supervisor | IMP-001 with selected core digest, frontend options and graphics when available |
| ARMSX2, Cemu, native PS3 | Shared checkpoint and existing engine-specific evidence journal | Shared native menu with host report |
| hakuX and Xenia | Shared checkpoint and existing native result | Shared native menu with host report |
| DOSBox and Wine integrated runtimes | Shared checkpoint with host process and requested runtime | Shared reporting with game and host identity. Runtime-specific measurements remain uncaptured. |
| Application, shelf, controller, appearance, GEM, SafeSave and CARTMOD | Settings intake | Player description and common application and device identity. Dedicated subsystem evidence remains partial. |

The recovery report retains technical stage evidence. An Android exit is attributed only when the process name, PID and start time match. First-frame evidence remains a lifecycle observation. Save-write status stays unknown unless a relevant source has recorded it.

## Example report

Synthetic example using BR-003's reported facts. The placeholder values illustrate unavailable or generated fields.

```text
CARTDECK SUPPORT REPORT
id: 00000000-0000-4000-8000-000000000003
schemaVersion: 1
source: failure recovery
category: Crash or freeze
game: Conflict: Desert Storm
console: PS2
engine: Play!
discSerial: NOT_CAPTURED
version: 1.17.0-DEBUG
versionCode: 91591676
buildRevision: UNKNOWN
description: The opening cinematic stuttered before the game stopped responding.

lastVerifiedStage=CORE_FRAME
GL_RENDERER=Adreno (TM) 840
androidProcessExit=reason=6; status=0; timestamp=1795684393611
saveWrite=UNKNOWN
```

```json
{
  "id": "00000000-0000-4000-8000-000000000003",
  "schemaVersion": 1,
  "createdAtMillis": 1795684393611,
  "createdAt": "2026-11-26T09:13:13.611Z",
  "source": "failure recovery",
  "category": "Crash or freeze",
  "game": "Conflict: Desert Storm",
  "console": "PS2",
  "engine": "Play!",
  "session": "NOT_CAPTURED",
  "discSerial": "NOT_CAPTURED",
  "version": "1.17.0-DEBUG",
  "versionCode": 91591676,
  "buildRevision": "UNKNOWN",
  "buildChannel": "NOT_CAPTURED",
  "manufacturer": "samsung",
  "deviceModel": "SM-S948B",
  "androidVersion": "NOT_CAPTURED",
  "androidApi": 37,
  "abis": "arm64-v8a",
  "description": "The opening cinematic stuttered before the game stopped responding.",
  "frequency": "NOT_CAPTURED",
  "diagnostic": "lastVerifiedStage=CORE_FRAME\nGL_RENDERER=Adreno (TM) 840\nandroidProcessExit=reason=6; status=0; timestamp=1795684393611\nsaveWrite=UNKNOWN"
}
```

## BR-003 and Play! source assessment

- BR-003 identifies Play! revision `1f27232772e484bfb79364b751b84f41f4c87481`, host `0.14.0-cartdeck3`, and selected SHA-256 `79f2ecdea33b75f490702f177c404a1f1f4271fea6ea7da2f65fc897f042337b`. Android reported reason 6, or ANR. The disc serial and the blocked thread are unknown. Cinematic instability and shelf lockout remain separate defects.
- The pinned recipe builds ARM64 Release with NDK `29.0.14206865`, Android API 28, static C++ runtime, 16 KiB maximum page alignment and retained symbols. The tracked patches route private storage and connect the Java VM host bridge. CMake enables the Libretro target and disables standalone Play!. [Build recipe](../../scripts/ci/build_ps2_upstream_core.sh)
- Pinned Libretro source declares the resolution multiplier, presentation mode and bilinear filter. CARTDECK already requests native resolution at 1×, fit presentation, and bilinear filtering off. The standalone Android Play! renderer options do not establish that the integrated GLES frontend supports an equivalent Vulkan or audio path. [Pinned Libretro source](https://github.com/jpd002/Play-/blob/1f27232772e484bfb79364b751b84f41f4c87481/Source/ui_libretro/main_libretro.cpp)
- Upstream master resolved to the pinned revision during reconciliation. The upstream compare reported zero commits ahead. Experimental branches are present, with no measured benefit for BR-003 in this workspace. No Play! binary was rebuilt for the application changes. [Upstream comparison](https://github.com/jpd002/Play-/compare/1f27232772e484bfb79364b751b84f41f4c87481...master)
- PCSX2 lists Conflict: Desert Storm `SLUS-20549` as playable on desktop. Its index also lists `SLES-50901` and `SLES-50902`. BR-003's disc edition is still unknown. Upstream Play! reports known issues for Soulcalibur III and Gran Turismo 4. Hidden Invasion `SLUS-20301` is a candidate working fixture based on an independent upstream observation. [PCSX2 index](https://github.com/PCSX2/pcsx2/blob/51e3036bdb7cad7ae7e25ad8caa438b06f538c4e/bin/resources/GameIndex.yaml) · [Soulcalibur III](https://github.com/jpd002/Play-Compatibility/issues/374) · [Gran Turismo 4](https://github.com/jpd002/Play-Compatibility/issues/2876) · [Hidden Invasion](https://github.com/jpd002/Play-Compatibility/issues/2889)

## Product Owner physical acceptance

1. Install the matching signed ARM64 APK over the previous candidate. Confirm cartridge identity, shelf, artwork, installed games, permissions, player profile, controls and existing saved progress.
2. Open a first PS2 cartridge without BIOS. Continue with Quick Play, return to the shelf, reopen, and confirm onboarding stays dismissed. Repeat with a valid BIOS already installed.
3. Import user-owned BIOS through the picker and library folder. Test invalid and unreadable input, replacement and revoked library access. Verify each PS2 engine keeps its own save associations across switch and reopen.
4. Test Conflict: Desert Storm on the reported handset with the exact disc serial. Compare the same opening cinematic in Play! and eligible ARMSX2. Run Soulcalibur III, Gran Turismo 4 and Hidden Invasion through a sustained segment, audio/input checks and save/reload. Record device, app build, engine hash, media route, renderer, frame cadence and thermal conditions.
5. Inject a startup failure, crash, hang and interrupted exit. Return to the shelf, launch another cartridge and restart CARTDECK. Verify that the failed cartridge only launches again on explicit action and the report belongs to the failed process.
6. Use Settings, live gameplay and failure recovery to make reports. Copy offline, inspect JSON and ZIP, attach/remove an approved screenshot, cancel an export, try two email applications, confirm the central recipient and ZIP, and repeat with no mail client. Check deletion and expiry.
7. Capture recovery, reporting and first-launch setup in portrait, compact landscape, tablet and TV. Verify each action is reachable with touch and the appropriate controller or remote.

The draft has no independent physical sign-off. The PR checks are the source of truth for the exact build revision, version code, APK digest and retained native receipts.
