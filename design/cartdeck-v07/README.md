# CARTDECK Appearance Selector V2 — Visual Swatches & Green

**Sienna design deliverable. No native application UI is changed by this source PR.**

## User-visible change

Existing app screenshot shows an unwieldy `Shelf appearance` AlertDialog with text-only Original/Pink choices, two accessibility checkboxes, and 14 mostly empty outlined hex input fields. This is a developer-like configuration form and contradicts the device-first CARTDECK experience.

Replace with a visual selector:

1. **Choose an Edition:** three preview cards **Original / Pink / Green**. Each is a live miniature of the same Arc-Rail spine + hardware shelf, showing *real* material colours, not a decorative unrelated mockup. Selection check plus name.
2. **Live larger preview:** a shared shelf segment with two cartridges and one rail updates as cards or swatches are selected. No trademarked game art needed; use original fallback cover assets.
3. **Customise Colours:** collapsed groups: Cartridge, Shelf, Lighting and Text & contrast. Each role shows a **solid actual-colour swatch**, its readable label and current hex secondary. Tap → a group of coordinated swatch chips and optional precise picker. Avoid empty hex fields by default. Hex editing remains an advanced action.
4. **Save as Custom:** create multiple personal named editions, e.g. `Moss`, `Berry`, `Night Shift`. A custom edition inherits its chosen base and stores ONLY overrides. Factory Original/Pink/Green stay immutable. Provide rename, duplicate and delete for custom editions only.
5. **Accessibility:** High contrast and Reduce motion remain real toggles, grouped separately and respected even when a user changes palette.
6. **Apply/Cancel:** controls stay visible at bottom. Preview may be transient; Cancel restores saved appearance and does not create a custom edition or mutate game/shelf data.

## Third official edition — Green

Reuse the current v03 source colour geometry, now renamed Green and available as approved *direction*. Same CARTDECK Arc-Rail cartridge geometry and boot identity; a sage-green chassis `#9BC7A8`, dark jade bezel `#163B36` and lime focus `#D4F47D`. Green bookshelf uses smoked forest panels `#163127`, sage rails `#315443`, mint/olive metal lip `#658978`, and legible lime accent `#D4F47D`.

**This promotes the Green design direction, not native APK readiness or final on-device visual acceptance.** Source asset definitions need Elliot's 3-way native token mapping (currently `CartdeckEditionPreset` has only Original and Pink, and `canonicalShelfMaterials(pink: Boolean)` cannot represent Green). Native generated resources need updating; do not rely on default Original fallback for Green.

## Keep in scope

- Palette changes are cosmetic; **no new cartridge shapes, control mapping, emulator changes or user-cover tinting**.
- The user's vertical shelf order, named sections, empty spaces, favourites, search and games stay unchanged.
- Existing `ShelfAppearance(edition,custom,highContrast,reducedMotion)` records must survive via explicit versioned migration; don't lose unnamed V1 custom colours.
- All swatches are derived from the canonical preset/token maps, not a divergent hardcoded presentation-only swatch list.
- Multiple named custom editions live in an app-private preferences model, not ROM/game records.
- Demo screenshots/QA for 393dp, 200% font, keyboard/TalkBack, errors, Cancel and in-place upgrade.

## Engineering handoff

See `appearance-selector.json`. Existing `ShelfEditors.kt`, `ShelfMaterials.kt`, `ShelfLayout.kt`, `ShelfStore.kt`, `CartdeckEditionTokens.kt` and current shelf-screen callers are the bounded Compose targets. This is a separate UI polish lane and does not displace Elliot's P1-A2 vertical shelf, live Search or input/save QA work.

Review owners: Sienna visual, Cassia experience, Daniel migration/runtime boundaries, Elliot implementation, Marcus independent QA. Third-edition final approval remains with Jonathan/Cassia after seeing native Original/Pink/Green screenshot comparison.
