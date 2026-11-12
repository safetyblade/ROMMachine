# Wii / Xbox source and distribution obligations

Engineering incorporation is owner-authorised. Public distribution is a separate owner decision. This inventory records source obligations and unresolved review points; it is not legal clearance.

## hakuX source baseline

The hakuX checkout is `a7cdb5a975e31730d488c47f6f10440b16c1e6f4`, with FEX submodule `52018bf46a8279b22bce738548ab3f40db467064`. The upstream `LICENSE` states GPL-3.0-or-later and expressly requires complete corresponding source for binary distribution. Preserve `LICENSE`, `COPYING`, every applicable third-party notice, modifications, build scripts and exact source/dependency revisions. A notices ZIP or links to upstream repositories alone are not a corresponding-source handoff. The hosted engineering workflow must retain a downloadable archive containing the actual compiled source trees (including statically linked components), patches and recipes, with a SHA-256 manifest, beside the APK.

The source archive must also identify the CARTDECK frontend revision and its Android/Gradle host integration. Do not include user games, proprietary firmware, keystores or credentials. Standard Android platform/NDK system components are toolchain requirements, not bundled game content.

## Compiled-component inventory

| Component | Upstream notice / obligation |
| --- | --- |
| hakuX native engine, HLE kernel, device code | GPL-3.0-or-later; complete corresponding source and modification notices |
| xemu MCPX/APU portions | Per-file LGPL-2-or-later and GPL-2-or-later; retain headers and both licence texts |
| `native/dev/apu/vp/svf.h` | Upstream explicitly records version ambiguity: source says GPL without a version; xemu header says GPLv2. Compatibility/distribution review remains open. Do not silently mark it cleared. |
| ADPCM-XQ, Qualcomm SGSR | BSD-3-Clause notices |
| nxdk kernel export definition | CC0-1.0 |
| FXAA / PCSX2 shader adaptation | GPL-3.0-or-later |
| FEX / unordered_dense / volk / VMA / fmt | MIT-family notices, plus FEX's separately licensed components |
| SDL3, including copied Android Java glue | zlib licence; preserve notice and mark modifications |
| adrenotools and its bundled ELF hook libraries | BSD-2-Clause; preserve patch provenance and dependency notices |
| glslang | Preserve full upstream multi-part licence (not a single simplified SPDX assertion) |
| SPIRV-Reflect | Apache-2.0; retain applicable NOTICE and licence |
| nv2a_vsh_cpu | Unlicense notice |
| range-v3 | Boost-1.0 and all bundled notices |
| rpmalloc | Upstream permissive notice |
| xxHash | BSD-2-Clause |
| SoftFloat, cephes | Preserve full upstream licence files and per-file notices |
| libslirp | Preserve full COPYRIGHT plus per-file notices; include actual source used |
| Dolphin Wii/GameCube | Preserve pinned Dolphin GPL source, upstream COPYING and matching Sys resources; Wii reuses the existing binary rather than adding a different untracked engine |

The final machine-generated source manifest and archive are authoritative for exact dependencies compiled into the artifact. All static dependencies need source/notices even when `DT_NEEDED` shows only a small shared-library set.

## Documented dependency deviations

The native builder selected these published revisions when the short revisions in hakuX's Android README could not be found in the available upstream histories:

- nv2a_vsh_cpu: `1115255708c10c4841b65dcd2223262e7a316598` rather than documented `561fe80`.
- libslirp 4.7.0: `3ad1710a96678fe79066b1469cead4058713a1d9` rather than documented `26be815b`.

These are explicit source deviations, not claims of an unchanged upstream baseline. Build and host tests may establish compatibility of tested code paths; they do not prove Android gameplay compatibility. Preserve these revisions in the build manifest and corresponding-source archive.

## Required distribution review

Before any public release, resolve the `svf.h` version/compatibility question, check the complete combined frontend/engine licence obligations, verify all notices against the actual selected dependency trees, and provide the full source handoff for the delivered binary. No proprietary game media, BIOS, dashboard or commercial Vulkan driver is authorised for bundling by this work.
