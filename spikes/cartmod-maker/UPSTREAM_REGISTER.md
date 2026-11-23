# Upstream code and toolchain register — source links, not vendored dependencies

Research date: 23 November 2026. **No source code, binaries, ROMs or SDKs from these repositories have been imported into CARTDECK.** Licences shown here describe upstream top-level declarations only; review each embedded component, sample asset, compiler and runtime before any reuse.

| Candidate | Repo | Use | Evidence / caution |
| --- | --- | --- | --- |
| GB Studio | https://github.com/chrismaltby/gb-studio | GBC touch visual-event/editor model and native ROM export reference | MIT top-level; Electron desktop editor, not Android SDK. Official CLI supports \`make:rom\`, emits \`.gb\` even for colour titles; GBVM and toolchain components need review. |
| GBDK-2020 | https://github.com/gbdk-2020/gbdk-2020 | Actual GB/GBC compilation, mapper/banking, asset generation | Includes SDCC + libraries; do not assume arm64 Android compiler binary exists. Review all licences. |
| Butano | https://github.com/GValiente/butano | Main proposed GBA 2D engine/runtime foundation | zlib; dependent on devkitARM or Wonderful Toolchain, examples exist; not a visual Android maker. |
| GBA Studio (eoinjordan) | https://github.com/eoinjordan/GBA-Studio | Workflow, resource schema and Butano code-generation research | MIT top-level; project documents partial editor status; not production validated for CARTDECK. |
| GBA Studio (dackmin) | https://github.com/dackmin/gba-studio | Alternative visual Electron/Butano reference | Distinct project with similar name; license, maintenance and code quality must be independently verified. |
| mGBA | https://github.com/mgba-emu/mgba | Independent game output boot and behavioural regression | Emulator/test reference only; standalone mGBA success does not prove integrated CARTDECK engine/save behaviour. |

Research sources:
- https://www.gbstudio.dev/docs/build/
- https://github.com/chrismaltby/gb-studio/blob/develop/README.md
- https://gbdk.org/docs/api/docs_toolchain.html
- https://gvaliente.github.io/butano/getting_started.html
- https://www.gbastudio.io/docs/concepts/engine-overview/
- https://www.gbastudio.io/docs/build-and-release/build/

## Decision for initial scaffold

1. Do not copy GB Studio/GBA Studio's editor application into the APK or embed development tools.
2. Adopt a **small original versioned data contract** plus safe preview. Investigate native runtime feasibility in isolation.
3. Use GB Studio/GBDK and Butano as proven compilers/engines; **pin actual versions and licences only when an adapter implementation starts**.
4. Keep PC/Android APK/SafeSave and the existing third CARTMOD lane outside research changes.
5. Target only genuine GBC and GBA artifacts. PSP remains a later target and separate technical decision.

Do not imply the source authors endorse CARTMOD or that an upstream GitHub project is safe to download without validation.