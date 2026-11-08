# CARTDECK — Sienna Vale — Arc-Rail Production Art-Lock Review v0.5.1

**Date:** 8 November 2026  
**Status:** Visual-lock candidate / approval requested; **not yet production-locked**.  
**Direction:** Jonathan + Cassia have approved one original CARTDECK cartridge family, Original/Pink editions, side-on shelf → front cover → explicit Open/Resume.  
**Source of truth:** `design/cartdeck-v05/cartridge-family.json`, `design/cartdeck-v05/cartridge/*.svg`, inherited C-slot mark and v03 edition JSON.

## Fidelity audit (based on exact merged source SVGs)

| Review point | Finding | v0.5.1 disposition |
| --- | --- | --- |
| Silhouette | Single 320×390 faceted front, 94×340 spine, one family for GB/GBC/GBA/future PS1 | **Retain** |
| Branding | Canonical C-slot/wordmark should be runtime overlays, not bespoke per system | **Retain**; brand placement slots documented |
| System badges | v0.5 front overlay and spine each baked their own badge socket and a separate badge SVG was also supplied | **Corrected:** remove the baked sockets; render **one** independent badge component in the specified front/spine bounds |
| Badge proportion | Existing front 78×28 and spine 60×34 badge bounds were notably different | **Candidate:** spine 68×30, front unchanged, preserve round-pill design with responsive corner radius |
| Pink interactive ink | Pink `accent` #8D315E versus `bezel` #743D5A is **1.07:1**; non-legible for meaningful stroke/text | **Corrected:** badge and art border use `focus` (#FFF3F7 on Pink, #4ED8C4 on Original); text uses `onButton` |
| Pink shell focus | Pink `focus` on Pink `chassis` is 1.72:1 | **Rule:** use `onChassis` for shell-selection outline, `focus` for dark-bezel interactive focus |
| Cover art window | 240×200 at (40,113), landscape-ish 1.2:1 | **Retain pending owner review:** default `contain` matte, explicit user choice to crop; never force crop to wallpaper/cover |
| Fallback imagery | Abstract original geometric Atlas artwork exists; no proprietary game imagery | **Retain** as baseline; art variety may be separate later |
| Long spine titles | Vertical text window 46×171; long names would overflow if rigid font scale | **Rule:** ellipsise visually; full title/system via TalkBack and front detail; support font scaling |
| Edition identity | v03 Original/Pink both share exact SVG paths; personal colours cosmetic only | **Retain** |

### Contrast pairs

- Original: `onChassis/chassis` **10.32:1**, `onButton/bezel` **16.41:1**, `focus/bezel` **10.28:1**.
- Pink: `onChassis/chassis` **6.59:1**, `onButton/bezel` **7.59:1**, `focus/bezel` **7.59:1**.
- The previous Pink `accent/bezel` **1.07:1** must remain decorative-only, never reused as meaningful badge/focus border.

Actual phone bitmap screenshots, Android font scale and TalkBack input still require native validation; static vector contrast results are not complete accessibility acceptance.

## Cassia / Jonathan final art-lock decision

Review the two versioned studies prepared by Sienna:

1. **Cartridge face/spine Original–Pink comparison:** exact source geometry, original Atlas fallback, dynamic title/badge overlays.
2. **393px phone-scale bookshelf comparison:** consistent density, short vertical title, system badge, selected state, Original and Pink.

Approve the following specific features as one permanent CARTDECK family: faceted upper shoulders, bottom service lip, side rails, reserved art frame, 1-piece badge semantics and original C-slot mark. Do not request distinct GB/GBC/GBA or PS1 cartridge silhouettes.

If approved, change `cartridge-family.json.state` from `design-v0.5.1-visual-approval-pending` to `production-art-locked-v0.5.1` in a **separate explicit approval commit** recording Jonathan/Cassia acceptance. Do **not** mark locked or merge this source change as runtime simply because the design CI succeeds.

## ASSET-H1 / Elliot integration boundary

ASSET-H1 #30 may continue preparing generic Android vector/source generation and semantic colours now. In its traceability manifest, resolve the final front/spine/badge resource set from **these v0.5.1 candidate sources only when approved**. Do not use superseded v0.4 bookshelf rectangles or double-draw a badge component.

P1-A1 #27 wires native Compose: sideways shelf → select cover (not launch) → Change/Reset local art → explicit Open/Resume. It must preserve game identity, library DB, imports, save RAM, manual states, package/signing and accessibility preferences. P0 independent QA #9 and public distribution gates stay separate.

**No emulator, game renderer, PadKit, ROM, save or Android application source changes are part of this design review.**
