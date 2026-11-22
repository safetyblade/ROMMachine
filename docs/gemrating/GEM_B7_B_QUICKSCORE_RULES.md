# GEM B7-B — DeckFit QuickScore v0.1 (provisional)

**Decision basis:** Jonathan's 22 November 2026 direction, issue #210. This is a compact, deterministic, low-confidence editorial estimate for normal CARTDECK handset gameplay **using its built-in touchscreen controls only**. The player has already legally acquired/imported the game; it does not claim ROM availability or that a specific title boots. RoxyRating is a private editorial game-quality opinion; My GEMRating is the user's enjoyment; My DeckFit is a separate user score. QuickScore is none of those.

## Shorthand

`QuickScore /5 = clamp(1, 5, console baseline + bounded game adjustment)`, all steps **0.5** and adjustment capped at **±1.0**. The active database uses twice-points internally (2–10). A complete game-specific review may replace the generic calculation for an exact release ID. Source-specific review holds and known **required-but-unavailable** motion/IR/stylus/keyboard inputs remain **Input review / unscored** until actual CARTDECK input support is established; an input-disabled game must not get a misleading numerical "pick up and play" score.

| Console family | Baseline |
| --- | ---: |
| GB, GBC, Neo Geo Pocket and Pocket Color | 4.5 |
| GBA, NES, SMS, Game Gear, Atari 2600/7800, DS, 3DS, WonderSwan/Color | 4.0 |
| SNES, Genesis/Mega Drive, Sega CD, PC Engine, Lynx, PSP, FBNeo | 3.5 |
| N64, PS1, Dreamcast, GameCube, PS2, MAME 2003-Plus | 3.0 |
| Original Xbox, Xbox 360, Wii, Wii U | 2.5 |
| PS3, DOS | 2.0 |

## Adjustments (from reliable source-backed traits only)

| Explicit game fact | Adjustment |
| --- | ---: |
| Simple 2D control scheme on a complex platform | +0.5 |
| Deliberate, turn-based mechanics | +0.5 |
| Precise real-time controls | −0.5 |
| Complicated simultaneous button combinations | −0.5 |
| Dual-stick precision aiming or frequent camera control | −0.5 |
| Difficult small-screen HUD/text | −0.5 |
| Mapped/working but awkward motion sensor fallback | −1.0 |
| Required input cannot be performed in current CARTDECK | No score; Input review |

**Bound automatic modifiers to ±1.0**, never grant a controller bonus. Ordinary broad genre labels add only modest prior information: Puzzle/Strategy +0.5; Racing/Fighting/Shooter −0.5. Generic Action, Adventure and Role-Playing are **neutral**, because they do not establish 2D control complexity or turn-based play. Where genuinely verified editorial mechanic tags exist, they add further bounded adjustments.

### Examples

- Game Boy Color ordinary game: **4.5**. Complex simultaneous inputs: **4.0**.
- Nintendo 3DS ordinary game: **4.0**. Supported but clumsy tilt: **3.0**; unusable required tilt: **Input review**, not a positive score. `VDG:3ds:15` Beyblade Evolution is the source-identified hold pending PERF-3DS #365.
- PS2 ordinary game: **3.0**. Confirmed simple 2D controls: **3.5**. 2D turn-based tactics (e.g. exact-ID PS2 Disgaea editions): **4.0**.
- Existing 91 exact-ID 14 Nov research touch assessments remain higher-priority and are **not** overwritten by the general heuristic.

## Precedence and data-layer requirements

1. Known unsupported/missing required input: show Input review without an invented number.
2. Actual game-specific verified QA and personal My DeckFit remain separate records; QA must independently state the exact runtime/APK/handset, input, audio, image and save status.
3. Exact-ID editorial touch assessment from reviewed dataset.
4. Exact-ID/evidence-backed game traits (including only verified two-dimensional controls).
5. Broad genre adjustment from original source where available.
6. System baseline.

GEM must display **Provisional**, and details must expose estimation basis. Real runtime status is separate: configured system, title booted, gameplay-tested, saved/reopened, unsupported/unknown. Never convert a system compatibility pass into a per-game pass. Do not import historical master spreadsheet Controller ratings into public DeckFit. Player overrides do not modify source metadata or save identity.

## Delivery

- Draft [PR #368](https://github.com/safetyblade/ROMMachine/pull/368) integrates QuickScore into the existing B7-B GC child branch; not a release or merge.
- Regression samples: GBC 4.5→4.0; 3DS 4.0→3.0 for mapped-but-awkward tilt; PS2 3.0→3.5 for simple 2D and →4.0 for evidenced 2D turn-based; review hold for Beyblade Evolution; all registered systems; 88,246 release projection; existing exact-ID seeds unchanged.
- Roxy can research higher-value exact-ID trait corrections as the finite catalogue matures. Daniel designs a versioned evidence/provenance sidecar or equivalent, safely patchable by approved connector/updates. Keep installed user data, ratings, signed source shards, save bindings and offline availability unaffected.
- Sienna keeps a single simple public `DeckFit X/5 · Provisional` and a distinct user editor `My DeckFit`. Marcus must test on real Android handset **without external controller**, including portrait, landscape, DS/3DS input/motion, PS2 2D, saves, and TV-specific presentation (TV compatibility may be documented separately without raising handheld DeckFit).
