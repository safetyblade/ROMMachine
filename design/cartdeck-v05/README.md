# CARTDECK cartridge family v0.5 — Arc-Rail

**Cassia cartridge-family lock is controlling.** Every imported game is rendered as a single original CARTDECK cartridge, never a legacy Game Boy, SNES or PS1 cartridge lookalike. GB/GBC/GBA and later PS1 differ through **text labels and metadata badges**, not cartridge shape. Original and Pink share exact path geometry.

## Editable masters
- `cartridge/front-base.svg`: one physical face and matte art window base.
- `cartridge/front-overlay.svg`: independently tintable frame edge and common badge socket.
- `cartridge/spine-base.svg`: side-on shelf cartridge, same physical cartridge as front.
- `cartridge/back-base.svg`: companion rear face if rotation is used.
- `cartridge/system-badge.svg`: generic pill, overlaid with runtime `GB`/`GBC`/`GBA` label; PS1-ready without new geometry.
- `cartridge/fallback-atlas.svg`: wholly original abstract landscape, no borrowed game art.
- `cartridge-family.json`: geometry, artwork crop bounds, edition, badge and accessibility handoff.

**Brand mark is NOT redrawn.** Use `../cartdeck-v03/brand/cslot-master.svg`, with consistent scale. The face and spine show system via badge, title via Compose text; do not bake system names, third-party game characters or franchise art into source assets.

## Screen ordering
Library books are **standing sideways spines**, tightly spaced and legible. Tap a spine → slide/rotate to the **front** cartridge, including badge, system name, title and optional artwork → explicitly **Open** or **Resume**. Customise Cover / Reset Cover uses existing `GameInteractor`/`CustomCoverManager` local file workflow. Empty cover uses fallback atlas plus original generated text overlay; never an empty box or huge initials-only tile.

## Edition rules
Appearance is semantic token data from v03 Original/Pink JSON. No duplicate SVG paths per colour. Do not tint user-supplied art; shell/frame/badge materials change only. Green is a personal custom-colour demonstration, not an officially approved shipping preset. Maintain readable contrast after applying custom colours.

## Native rendering contract
Every master is editable SVG, not automatically an Android runtime resource. Compose or reviewed VectorDrawable conversions must support per-layer colours and clip user artwork inside the exact art window. Prevent focus/pressed states from deforming cartridge geometry. Save shelf scroll position and selected game ID; preserve ROM records, file grants, game saves, notification permissions and current standalone package/signing.

## Engineering acceptance
- 3+ games from GB/GBC/GBA render **identical silhouette, dimensions, bezel/badge placement** in one shelf, Original and Pink.
- The only distinctions are player-customised cover art, readable title, system badge and cosmetic edition.
- Missing artwork uses original fallback; user art survives app restart/update and resetting cover returns to fallback.
- Tap spine never launches; explicit Open/Resume invokes current launch callback exactly once.
- Back restores shelf selection and scroll. TalkBack reads complete game title and system, min 48dp touch.
- No commercial ROMs or copyrighted game-art embedded in repo.
- No Android runtime changes in this design PR. Integration remains Issue #27, and P0-B #9 independent gate is open.
