# Registry and PS2 recovery delivery boundary

This slice builds on merged PRs #149 (Wii controls), #150 (source preflight),
and #151 (incomplete GDI family grouping). It does not close #143.

## Implemented player path

- Console compatibility settings and cartridge details select an actual integrated
  engine, automatic recommendation, console inheritance or previous successful
  engine. Existing core provisioning, native version stores and rollback controls
  remain authoritative; no engine downloader or external emulator application is added.
- Eligibility checks Android ABI/API, advertised GLES/Vulkan, file formats, packaged
  binaries and modern support assets. Actual renderer/context creation still belongs
  to the existing host. ZIP contents remain validated by the archive loader.
- Legacy defaults and PS1/PSP opt-ins survive. Cartridge overrides are keyed by a
  hash of source identity, not a refresh-dependent database row ID. Engine changes
  disable automatic state resume and retain existing state namespaces and saves.
- Confirmed failures are scoped to engine/version/host. The default policy requires
  explicit recovery; optional fallback occurs only on another deliberate Open,
  to a previously observed eligible engine. No recursive retry loop is introduced.
- PS2 uses a supervisor in the main application process around its existing `:game`
  activity. A failed activity returns a copyable report and a shelf recovery action.
  The report records the last observed stage, selected engine provenance, actual GL
  strings when available, and Android process-exit reason/status on Android 11+.
  It excludes content names, paths and arbitrary exception messages.
- Host3 adds a non-duplicate core-video callback event without changing rendering,
  input or state behavior. It can establish core frame submission, not correct
  display pixels, audio or gameplay. Older hosts cannot mark that success; their
  frontend draw ends startup supervision to avoid killing existing gameplay.
  Background time and device sleep do not consume the PS2 foreground startup budget.
  Diagnostic update failures do not turn gameplay or exit into disk-full crashes.

## Acceptance and remaining end goals

| Path | This slice | Still required |
| --- | --- | --- |
| Registry/selection/fallback | Player controls, eligibility, persistence, state isolation and failure policy | Device selection/rollback/install-over acceptance; real Android integrations for additional candidate engines |
| PS2 | Isolated-process recovery, stage/provenance/graphics/exit diagnostics | Capture affected-phone native evidence, repair the identified Play! fault, demonstrate graphics/audio/input/memory-card reopen |
| Dreamcast | Retains merged GDI correction and existing CDI/Flycast path | Finish conditional CUE identity, exact complete-family SAF staging, playlists and phone save/reopen acceptance |
| Xbox | Existing internal SDL3/Vulkan engine and HDD/rollback paths preserved | First accepted guest presentation, Xclipse-specific diagnostics and bounded failure recovery; phone gameplay/save acceptance |
| Wii/GameCube and older engines | Existing core revisions, configurations, controls and data retained | Device regression of Wii profiles, GripArc/PivotArc, saves and install-over |

The owner's earlier Wii and Dreamcast CDI gameplay confirmations remain valid
observations. No new phone gameplay test was performed in this coding session.
PS2's native crash cause remains unestablished until actual phone diagnostics are
available. This slice must not be described as completed PS2/Xbox emulation.

Use #150's preflight before native builds. Local validation covers root Kotlin
lint, targeted registry and PS2 routing/journal tests, fast packaging/launch source
contracts, and the real patched core-frame counter harness. Local Kotlin source
compatibility is checked against the retained host2 API; full host3 compilation
and APK verification are delegated to the existing independent Actions pipeline.
Do not infer hardware acceptance from those source checks or close #143 from CI.
