# CARTDECK GC-01H / PERF-02 — Landscape Aspect and Surround: complete design V1

**Status:** Design package ready for later implementation; engineering/source and hardware acceptance **not** claimed. Owner direction: 16 November 2026. Visual design lead: Sienna Vale design lane, architecture constraints from Daniel Mercer. Tracked by [PS2-RSRC/DISPLAY-01 #284](https://github.com/safetyblade/ROMMachine/issues/284), [PERF-02 #60](https://github.com/safetyblade/ROMMachine/issues/60), [GC-01 #262](https://github.com/safetyblade/ROMMachine/issues/262). Do not interrupt current CI-01 / GC-01C checkpoint. The source visual studies and measurable tokens are in `design/cartdeck-v03/surround/`.

## 1. Locked product decision

- **Every game session begins in AUTO aspect**, even if WIDESCREEN was selected during the last session; no hidden last-used widescreen preference and no automatic remapping. Auto is the sole initial/default state.
- Player may change to **WIDESCREEN** in CARTDECK's in-game Display route only if a verified *native renderer/game-specific* widescreen mode exists. Do not equate a 16:9 Android surface with widescreen guest video. Never silently stretch, crop, zoom or patch original 4:3 content. No experimental hacks automatically enabled; any future distortion feature needs new approval.
- **Surround defaults to STANDARD**, now **slightly more visible** than today's thin 6dp maximised frame. The second appearance choice is **ORNAMENTAL**, a deliberate, thicker premium CARTDECK chassis with coherent edition-specific detailing.
- Surround choice may persist as a presentation-only preference and, where supported, with named appearances. It does not affect guest video aspect, core settings, ROM identity or saves.
- Screen-first invariant: every source pixel remains visible and correctly proportioned; decorations use the noninteractive region *outside* the final video rectangle. The optional ornamental choice may allocate a modest protective chassis margin and thereby reduce the *overall displayed size* where needed, but must never crop, distort, hide HUD, obstruct physical/touch controls or trigger an engine reset. Player can return to Standard immediately.

## 2. In-game Display menu — approved information architecture

Existing CARTDECK `GameMenuTheme` / `GameMenuPanel` and existing menu host/adapters own presentation and commands. Add one shared `Display` route only where host displays can safely implement it. No direct player-facing Libretro/ARMSX2/Xenia submenus.

```text
CARTDECK  /  DISPLAY
ASPECT RATIO
  ◉ Auto                                  Recommended · resets on next launch
  ○ Widescreen                            Genuine 16:9 where available
     [Unavailable] This game has no verified widescreen output.  (when unsupported)

SURROUND
  ◉ Standard                              Balanced · visible chassis edge
  ○ Ornamental                            Heavier milled surround

  [Preview: symbolic representative glass/surround, no game capture]
  [Return to Game]                        [Back]
```

Copy contracts:
- `Auto`: **"Original proportions. Selected automatically on every launch."**
- `Widescreen` when verified: **"Use this game's supported widescreen output."**
- `Widescreen` unavailable: **"Widescreen is not verified for this game. Auto preserves the original picture."**
- `Widescreen` fails after selection: **"Widescreen couldn't be applied. Auto has been restored."** Keep the running game and display safely if possible, never claim changed output without native confirmation.
- `Standard`: **"A clean CARTDECK rim around your game."**
- `Ornamental`: **"A thicker CARTDECK frame with details from your appearance."**
- `Ornamental` tight clearance: no pop-up; design simplifies automatically while selection remains recorded. The preview shows responsive compact finish.
- No "Fill" label, no unqualified "full screen" mode implying aspect stretch.

Interactions:
1. Opening Display does not change aspect or suspend existing controller pairing. A selected radio item visually updates immediately. Engine video changes only after the backend confirms the aspect option.
2. On first game boot (including game restart/next cartridge/close/reopen) aspect initializes to Auto. Rotate phone or connect/disconnect controller during same game session: retain selected mode only if backend still supports it, recompute geometry; otherwise fall back to Auto with nonblocking explanation.
3. Widescreen option is disabled + explanatory text if backend/title/region/revision evidence or native support is absent. A source-title hint alone cannot enable it.
4. Back/Guide/Menu/remote works identically on handheld, Bluetooth controller and Android TV. Focus is visible; 48dp minimum controls; scrolling and 200% text do not cover video.
5. Surround selection is reversible without restarting the guest. Preview is a rendered schematic from semantic tokens, not a frame-grab, runtime GPU probe or separate emulator surface.
6. User may change Surround globally through **Appearance > Gameplay Surround** (Standard/Ornamental), with same preview. In-game quick change updates the selected appearance safely and atomically; named appearances follow their saved surround setting where present. Aspect is always session-only and not part of Appearance.

## 3. Renderer and capability contract

Video source truth differs per host. Do not infer guest display from input SurfaceView dimensions.

**Aspect authority hierarchy**
1. Actual backend-reported guest *display* aspect / pixel-aspect-corrected image dimensions (not raw framebuffer size when pixel aspect differs).
2. A verified title/region/revision widescreen option **and explicit successful native acknowledgement**, with measured output/host-correct geometry. Distinguish engine 16:9 setting from true patched/widescreen gameplay.
3. Legacy conservative 4:3 guest presentation *fallback* when actual aspect is unknown. Mark diagnostics `UNKNOWN / UNVERIFIED`, never represent as validated pixels.

State diagram:
```text
SESSION_CREATED -> AUTO -> AUTO_PRESENTING
                           | user Widescreen + VERIFIED capability
                           v
                      APPLYING_WIDE
                       /         \
                  ACK+aspect     reject/timeout/mismatch
                     |               |
                WIDE_PRESENTING -> AUTO_RESTORED (explain once)
                     |             ^
           title change/rotation/reinit unsupported
                     +--------------+
NEW SESSION always resets aspect to AUTO
```
- The widescreen control is a *request*; 16:9 aperture is not proof of success. No state writes until a confirmation policy is met, and never block render/SDL/Vulkan threads while waiting.
- Read backend capability before showing active Widescreen. ARMSX2 must trace source-native aspect and actual `patches.zip`/widescreen patch conditions in #284; an unverified generic patch is not enough.
- PS1/PSP, original Xbox, Xenia, Cemu, Libretro, Wii/DS use their own truthful capability adapters; no universal cheat or mutation to shared renderer settings.
- No extra GPU surface, PixelCopy, shader pass, frame cropping, black-bar painting atop guest output, or forced frame callback on menu open.
- Reuse `NativeViewportPolicy.fit`, `inwardPixels`, existing GC-01B edgeGuard=1px and `NativeRendererAudit`; introduce one pure `DisplayAspectSelection` state resolver in later implementation if beneficial.

## 4. Artwork construction — CARTDECK hardware, not generic blank bars

The noninteractive shell must fill actual spare space from the measured safe window to the video boundary; it must **never** overlap the video or functional PadKit touch bounds.

Two permanent, engine-independent surrounds:

### STANDARD — stronger default (revision of current thin frame)

- Core silhouette remains existing CARTDECK bevel/chamfer with rounded interior glass; no competing logo.
- Increase visible rim from current maximised 6dp to **10dp design target** (responsive 7–10dp when tight). Main chassis token `chassis`, thin darker machined lip `bezel`, interior `glass`.
- Accent rail: **2dp design target**, active `accent` at restrained ~55–70% opacity, broken at reserved logo/control seats; two short corner returns, not continuous neon.
- Add thin tactile shadow/separation using `secondary` and `glass`. Static, GPU-cheap Canvas draw. No animated LEDs by default.
- At 4:3 game within wider landscape, fill the side *surround* with restrained chassis material instead of flat, unrelated black; preserve any genuine black bars inside guest frame.
- Keep unused side gutters visually subordinate to the live game.

### ORNAMENTAL — thicker premium optional frame

- **26dp target outer manufactured band** on 600dp+ height landscape and roomy TV/tablet; **18dp target** on medium devices; **12dp minimum simplified ornamental finish** on narrow/tight devices. Purely design-intent targets, not unconditional space reservations.
- Layer order: outer chamfered `chassis` wall -> `secondary` inset shoulder -> inset `bezel` rail -> narrow `glass` lip -> image aperture.
- A pair of stepped/milled rails follows left and right exposed gutters, with cross-cut **C-slot-inspired diagonal mill marks**, restrained panel seam, one micro fastener at top and bottom of each sufficiently roomy side. No generic faux game console branding, excessive gradients, brushed metallic texture downloads or flashing borders.
- Accent uses the same semantic appearance tokens. Use 1.5–2dp restrained segments/points, not a solid luminous rectangle. Upper/bottom detail collapses when video is close to the edge.
- **Opt-in visual size tradeoff:** prefer to consume pre-existing side gutters. If there is insufficient room, allow a *bounded, symmetric* reduction of video size of at most 5% along the limiting dimension for Ornamental **only**, preserving full guest content/no crop and rechecking touch seats. If even that cannot safely fit, keep ornamental identity through an 8–12dp compressed edge rail. Never create 26dp by obscuring content.
- No buttons are decorative substitutes for game controls; no ornamental hit targets; do not move Start/Select or alter controller bindings.

### Appearance alignment — locked semantic mapping

| Active appearance | Chassis/material finish | Ornament language | Accent |
|---|---|---|---|
| **Original** | Graphite `#303B49`, dark inset `#1C2430` | Precision machined chamfers and short parallel teal rails | Teal `#4ED8C4` |
| **Pink** | Satin rose `#F2A9C9`, berry inset `#C873A0` | Soft double-rail and subtle jewel facet; tasteful not toy-like | Berry `#8D315E` |
| **Green** | Mint/sage `#9BC7A8`, woodland inset `#446F5C` | Organic step-cut facets, balanced mechanical seams | Lime `#D4F47D` |
| **Stealth / Acid** | Graphite `#151C20`, olive `#465840` | Technical louvres, slim hazard chevrons from existing pattern/stamp rules | Acid `#9BC74A` |
| **My Appearances** | Resolved custom `chassis/secondary/bezel/glass` | Same CARTDECK geometry, custom pattern/stamp clipped to visible chassis only | Custom `accent`; optional existing lighting subject to reduction setting |

Use existing `CartdeckEditionTokens`, `CartdeckSurfaceDesign` exclusion geometry and existing `CartdeckLighting` (when enabled) rather than a new global skin engine. Avoid assuming `Mono` is an available player edition; only Original/Pink/Green are currently valid choices.

## 5. Geometry, constraints and responsive acceptance

**Inputs:** `safeWindow` = actual app content minus Android `safeDrawing`/cutout/gesture and TV overscan, `videoContentAspect` = verified effective display aspect, `controlExclusions` = current PadKit/PivotArc/GripArc/touch seats, `brandExclusions` = C-slot/wordmark. All geometry must be recomputed on rotation, multi-window, split screen, TV layout and controller hotplug.

1. Compute maximal correct-aspect video rect within allowed screen while respecting selection's surrounding band and existing controls. Place exactly once; renderer remains engine-owned.
2. Standard reserves visible 7–10dp only where safe; Ornamental prioritises gutters, then bounded 5% shrink if needed, then compresses ornaments. No new chrome under source pixels. Do not assume full-device aspect is 16:9.
3. All decoration/drawing is strictly **outside** `videoRect`; no canvas over image. Tracing: `safeWindow`, `videoRect`, requested/effective mode, source evidence, surround variant and available gutters in opt-in diagnostics only.
4. Use shared one-pixel inward rounding `NativeViewportPolicy.inwardPixels`; ensure no upper edge bleed or fractional pixel loss. No double application of inset or overscan.
5. When touch controls occupy a side, ornamentation clips away from PadKit hit targets and button inventory; preserve >=48dp targets, Start/Select, DS stylus, Wii U Cemu pointer bars.
6. Portrait is **unchanged by default** (existing manufactured shell and 4:3/DS glass). This programme updates landscape and applicable TV/tablet presentation; no surprise portrait relayout.
7. Landscape phone controller connected: game-first frame and visible surround with immediate Menu access. Landscape phone touch: original control layout preserved; don't substitute ornament for controls. TV: default game-first and 32dp safe overscan, decoration responsive; no TV remote considered gamepad.
8. Widescreen 16:9 can reduce available side-gutter detail—ornamental treatment becomes an edge rail, not forced black side bars.

The comparison vector is **a schematic**, not proof that any real game supports 16:9 or that a particular title/frame will match the sample.

## 6. Persisted state (future implementation, design only)

`aspectMode` = `AUTO | WIDESCREEN` belongs to the active gameplay session and resets to `AUTO` on every new game session, even same title/relaunch. No legacy 4:3 persisted override may silently take priority.

`surroundStyle` = `STANDARD | ORNAMENTAL` belongs to presentation/appearance and defaults to `STANDARD`; can be saved for global appearance or named `My Appearances`. Do not modify Room or `CartridgeSaveBinding`.

Suggested compatible `ShelfAppearance` evolution: optional `surroundStyle` in its existing atomic `shelf-layout-v1.json` appearance envelope, and optional field on `NamedAppearance`; increment validated `appearanceVersion` from current 2 to 3 **only when implemented**, allowing reads of 1/2/3. If migrated profile lacks field, Standard. Preserve unknown future format as error/retain, no reset, and keep unrelated sections/positions intact. Application settings that already own fullscreen/landscape control must be reconciled rather than introducing a second conflicting setting.

Runtime settings controls do not force a write to cartridge identity/ROM URI, engine config, PS2 memory cards, Xenia profile, Cemu NAND or PS3 data. Reversible rollback must render old records as Standard without throwing.

## 7. Source anchors and implementation responsibilities

| Concern | Existing source | Future bounded work |
|---|---|---|
| Native aperture fit | `.../feature/game/NativeViewportPolicy.kt` | Replace fixed ARMSX2 aspect-as-proof with verified guest source/capability; retain safe fallback and inward rounding |
| Native landscape display | `.../feature/game/XboxGameControls.kt` | Pass session aspect selection and surround style into presentation, no input owner change |
| TV/maximised visual chassis | `.../feature/game/CartdeckMaximisedFrame.kt` | Implement two noninteractive surround styles around measured video |
| Touch-deck shell | `.../feature/game/CartdeckConsoleShell.kt` | Share visible manufacturing vocabulary, omit ornament in PadKit/brand/video exclusions |
| Libretro glass | `.../feature/game/CartdeckViewportPolicy.kt` | Preserve DS/GB/GBA, 1px raster guard and no-stretch constraints |
| Colours | `.../mobile/shared/compose/ui/CartdeckEditionTokens.kt` | Use current edition roles; no new theme resource families |
| Appearance storage | `.../feature/shelf/{ShelfLayout,ShelfStore}.kt` | Optional backward-compatible `surroundStyle`; no Room migration |
| Settings/menu | `.../feature/gamemenu/{GameMenuActivity,SharedGameMenu}.kt` | Display route, focus, capability-disabled reason, preview, no action leaks |
| Backend report | `.../feature/game/NativeRendererAudit.kt` and ARMSX2 JNI host | Effective frame display aspect/verified native widescreen response evidence; no false PASS |
| PS2 source resources | `scripts/ci/package_ps2_runtime_support.py`, `Armsx2Engine.resources()` | #284 `patches.zip` verification and warning-free healthy launch; separate from ornamental paint |
| CI | CI-01 verified native artifacts | Android-only UI change should not rebuild native cores; exact-SHA APK source test |

## 8. Design acceptance cases — do not claim device PASS

| ID | Scenario | Required visual/interaction result |
|---|---|---|
| L01 | PS2 4:3 / Auto first launch | Correct non-stretched 4:3; balanced Standard chassis around spare gutters |
| L02 | PS2 real 16:9 / Auto | Correct renderer-reported widescreen without forced clipping |
| L03 | Select supported Widescreen | Engine acknowledges guest display mode; video + surround reflow with one viewport |
| L04 | Select unsupported Widescreen | Disabled with truthful reason; Auto remains active |
| L05 | Last session set Widescreen, relaunch same cartridge | **Auto** selected; last session never overrides boot |
| L06 | Standard default / Original, Pink, Green, Stealth Acid | 7–10dp stronger rim and edition accents; safe source pixels |
| L07 | Ornamental / all appearances | 18–26dp desired band when space; variant-specific rails; no HUD/control collision |
| L08 | Ornamental + native 16:9 on narrow device | Ornament compresses, source remains fully visible; no fake sidebars |
| L09 | Touch mode + GripArc/PivotArc active | All buttons including Start/Select accessible; no decoration across hit targets |
| L10 | TV and 200% text | 32dp overscan retained, DPAD/remote focus visible, controls >=48dp |
| L11 | Rotate / split-screen / controller disconnect | Recompute once; no transient cropped/duplicate renderer; touch fallback works |
| L12 | DS dual screen / Wii U stylus | Input transformation unchanged; no invalid visible video rectangles |
| L13 | Old appearance schema, named saved profile, rollback | Standard fallback, custom profiles/ROM saves unchanged; no destructive decode |
| L14 | Save-state, pause, menu during aspect application | No native engine deadlocks, fake saved-state completion or lost menu |
| L15 | ARMSX2 patch resource warning, clean game launch | #284 separate source fix: valid `patches.zip`; zero unnecessary native technical toasts |
| L16 | Invalid source metadata or backend failure | Auto conservative safe fit; status UNVERIFIED/FAILED, never fabricated widescreen |
| L17 | Standard vs Ornamental screenshots across 4:3 and 16:9 | Measured geometry within safe bounds; no raster bleed; ornament stays outside video |

## 9. Future implementation order / stop gates

1. **H-DISPLAY-01** — pure source-aspect capability and Auto/Widescreen state resolver tests; no app renderer switch without backend evidence.
2. **H-DESIGN-02** — Standard/Ornamental shell drawing in measured Compose layer with existing appearance tokens; snapshot/geometric tests. Never replace the native engine.
3. **H-UX-03** — shared Display menu, Android TV focus, settings, preview and appearance-safe persistence; migration tests.
4. **H-PS2-04** — integrate ARMSX2 verified source/patch condition from #284, source-native widescreen if supported; proper errors and zero irrelevant startup toasts.
5. **H-QA-05** — linked host regression tests, exact-SHA CI/APK, accessible screenshots and owner physical trials with before/after geometry. Preserve Play! rollback and installed game data.

This is **design complete for a future implementation handoff**, not a visual/hardware acceptance. No code merges on current CI-01/GC-01C branches. Implementation should stay within its own bounded branch/PR and respect original full programme order.
