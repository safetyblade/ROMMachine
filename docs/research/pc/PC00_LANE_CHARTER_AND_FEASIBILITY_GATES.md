# CARTDECK PC-00 — Dedicated PC Gaming Research Lane
**Status:** RESEARCH / FEASIBILITY ONLY · **Owner decision:** 17 November 2026 · **Implementation:** NOT AUTHORISED.
**Product Owner:** Jonathan Prestwich · **Project Director:** Cassia Rowan · **Architecture:** Daniel Mercer · **Engineering feasibility:** Elliot Shaw · **Product/UX:** Sienna Vale · **QA:** Marcus Reid · **Game curation:** Roxy Quinn.

## Product objective and independent lane boundary

Establish two first-class forms of **local PC gameplay within the existing CARTDECK application** without conflating PC game-store access, Windows execution, and optical media installation:

- **PC Modern** — player-owned storefront libraries and standalone Windows game executables, running locally via licensed, practical Android ARM64 CPU/graphics/Windows compatibility layers.
- **PC Classics** — original DOS and Windows-era game installations from lawful optical images, installers and existing directories, including a *MicroProse Magic: The Gathering / Shandalar* Windows 95/98 case study.

Both share CARTDECK's single cartridge shelf, profile and Junior entitlements, input/menu/display, catalogue metadata and truthful save-management plan. Different underlying runtimes are acceptable. A game-store frontend, source repo or CD image is **not itself proof of runnable gameplay**.

This is **an independent research lane**. It does not sit inside PERF-02, GC-01D/H, the three-console recovery queue, GEM or CartMods delivery stages. Architecture and public-source review can happen in parallel, but no runtime source changes or APK/CI changes are approved.

## Issue ownership / navigation

| Register | Scope | Status |
| --- | --- | --- |
| [PC-00 #299](https://github.com/safetyblade/ROMMachine/issues/299) | Parent controlling research programme, decisions and milestones | RESEARCH |
| [PC-01 #300](https://github.com/safetyblade/ROMMachine/issues/300) | Modern PC, GameNative, Winlator, MiceWine and Wine/FEX/Box64, storefront and exe | RESEARCH |
| [PC-02 #301](https://github.com/safetyblade/ROMMachine/issues/301) | Classic PC disc images, Win9x guests, Wine legacy routes, install flows | RESEARCH |
| [PC-03 #302](https://github.com/safetyblade/ROMMachine/issues/302) | Shared PC cartridge architecture, security, controls, profiles, saves, QA | RESEARCH |
| [EXP-95 #195](https://github.com/safetyblade/ROMMachine/issues/195) | Existing Shandalar proof candidate, linked under Classic PC | RESEARCH |

**Source/rights rule:** evaluate actual pinned source, branch/revision, complete dependency and redistribution obligations, proprietary account/OS/DRM restrictions and game assets. An open-source repository is not proof it is safe or legal to embed a whole frontend. No DRM/copy-protection bypass, unlicensed Windows/retail media redistribution, no website credential harvesting.

## Current preliminary architecture — research hypothesis, not approval

| Concern | Research direction |
| --- | --- |
| Embedded Modern runtime | GameNative as storefront/source reference; compare Winlator/MiceWine, Wine/Proton + Box64/FEX/Hangover, Android integration constraints |
| Classic PC runtime | Inspect actual CARTDECK DOSBox Pure first; compare Wine 32-bit and DOSBox-X Win95/98 only if needed |
| Graphics/CPU | Samsung SM-S721B Exynos 2400e/Xclipse 940, Android 17, ARM64, 16KB, stock Vulkan baseline, translate DX9/DX11 where actually possible; do not assume Adreno-specific drivers |
| Optical media | Read-only SAF mount; validate signatures and companion cue/bin tracks; distinguish install media from runnable game; multi-disc and CDDA support must be backend-proven |
| Storage and identity | Stable game/title/edition/source identity, installed runtime environment, per-player writable save inventory; no extension-only ISO routing; no unsafe direct Room migration in research |
| Profiles/Junior | Manager-approved PC installs/linked accounts and Junior allowlist; per-player isolated saves, installation and account/privacy boundaries |
| Menu/display | Consistent CARTDECK Play, install/setup and Return to Shelf, controller + mouse/keyboard + TV escape, per-game options in Advanced |
| SafeSave/GEM | Export only verified save artifacts, not OS VHD/ROM/installer/prefix by assumption; GEM catalogue/DeckFit status distinct from installed or actually working games |
| Runtime distribution | On-demand runtime components only if source/licences, signing, integrity and update/rollback are feasible; minimise base APK bloat |
| Compatibility | Explicit tiers RECOGNISED / INSTALLABLE / BOOTS / RENDERS / INTERACTIVE / SAVES / OWNER ACCEPTED, no universal supported-PC flag |

## Research milestones and stop/go gates

| Gate | Owner | Evidence required | Status |
| --- | --- | --- | --- |
| **PC-R0 — Current source audit** | Daniel + Elliot | CARTDECK's actual DOSBox Pure pin and host capabilities; latest comparable projects' pinned source/licensing; hardware/API constraints | NOT STARTED |
| **PC-R1 — Candidate matrix** | Daniel + Elliot | GameNative, Winlator, MiceWine, Wine/Box64/FEX and DOSBox routes by feasibility, legal, embedding, 32/64-bit, GPU, media and save | NOT STARTED |
| **PC-R2 — Architecture ADR / rights** | Daniel | isolate native host, Wine vs Win95 guest split, store auth/privacy, disk mounting, distributions, save and rollback; GO/NO-GO | NOT STARTED |
| **PC-R3 — Product design** | Sienna + Daniel | one-app UX, import/install/play, per-game controls/mouse, Junior, profiles, SafeSave, GEM, TV | NOT STARTED |
| **PC-R4 — Smoke and QA plan** | Elliot + Marcus | bounded lawful test fixtures, Shandalar staged Win9x or Wine path, testing metrics, reversible device-spike proposal, no test PASS assumed | NOT STARTED |
| **PC-R5 — Owner decision** | Jonathan | written GO/CONDITIONAL/NO-GO per Modern storefront / standalone exe / classic disc / Shandalar route / optional native port; separate implementation issue only after GO | PENDING |

**Owner choice:** Research results may independently conclude Modern GO and Classic NO-GO or vice versa. Do not force implementation order or a single emulator architecture.

## Boundaries and dependencies

- **Current emulation recovery (execution already commissioned):** [PS3 #297](https://github.com/safetyblade/ROMMachine/issues/297), [Wii U Bayonetta 2 #296](https://github.com/safetyblade/ROMMachine/issues/296), [Xbox360 #295](https://github.com/safetyblade/ROMMachine/issues/295), tracked by [PERF-02 #60](https://github.com/safetyblade/ROMMachine/issues/60).
- **GC/CI:** [GC-01 #262](https://github.com/safetyblade/ROMMachine/issues/262), [PR #294](https://github.com/safetyblade/ROMMachine/pull/294). Research must not touch branch, native engines or APK, menus, display, controller or artifact producer paths.
- **Shared future contracts:** [profiles #289](https://github.com/safetyblade/ROMMachine/issues/289), [SafeSave #290](https://github.com/safetyblade/ROMMachine/issues/290), [vault #293](https://github.com/safetyblade/ROMMachine/issues/293), [GEM #194](https://github.com/safetyblade/ROMMachine/issues/194), [QA #248](https://github.com/safetyblade/ROMMachine/issues/248), [post-emulation experience #253](https://github.com/safetyblade/ROMMachine/issues/253).
- **Not approved:** direct third-party source imports, unpinned APK/OS/game downloads, public PC console support claim, account integrations, new native builds, changed app schema/signing/ROM roots or save ownership.

## First research delivery for owner

Return a table of realistic candidates, high-risk unknowns, legal/source status, fit to Samsung Xclipse, likely install game media flows and embedded integration practicality; then a specific architecture and *unexecuted* proof recommendation. Include a separate Shandalar Windows 95 feasibility judgment. Every claim of gameplay must be tied to actual device/source evidence; any untested item must be labelled UNVERIFIED.

**Research evidence is not an implementation authorisation.**