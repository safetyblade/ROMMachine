# BRAND-01 / B2 — Product identities

Design candidate, 18 November 2026. Continues B1 on the same design-only branch. No changes to the original CARTDECK C-slot, wordmark, launcher mark, hardware, runtime, or approval gates. The user-approved direction is classic by default, expressive by choice; the three specific marks, secondary accents and optional copy below require owner visual approval.

## Identity inventory

| Product | Symbol | Wordmark | Use |
|---|---|---|---|
| GEM | Rounded faceted gemstone | GEM | Discovery, recommendations, ratings |
| SafeSave | Cartridge in a protective enclosure | SafeSave | Verified save inventory, export, import and recovery |
| CARTMOD | Modular cartridge with fitted module | CARTMOD | Preparation, modifications, PC setup and installation management |

Shelf retains existing CARTDECK identity. Junior is B3, not an adult dock destination. PivotArc and GripArc remain italic signature features; full feature icon/mark work is B4. Roxy and team names remain internal, no mascot or persona assets. All B2 shapes and custom stroke wordmarks are original vector construction for review; external legal/trademark clearance is pending. No font binaries or external artwork.

## Asset suite

Each product has four independent SVG assets: mark (24-unit viewBox), path-built wordmark, full lockup and empty-state illustration (120 x 80). mark.svg also serves the compact dock; lockup.svg is the monochrome master. All use currentColor with no hard-coded edition palette or external references. Optional product accents may tint the mark only; wordmark uses semantic foreground. Lockup files are one-colour masters; compose mark + wordmark independently for a two-tone treatment. Do not distort, add bevels/glow/outline, rotate, simplify at runtime or substitute a generic controller icon. B1 original master remains the only app/launcher brand.

- Dock: mark 24dp (20dp compact) with live, readable product name and selected semantics. Do not use a full lockup or truncate the only label. Existing order and capability gates govern actual destinations. Minimum 48dp touch/focus target. On narrow/short layouts compact in one horizontal row; if more destinations cannot fit, preserve discoverability and viewport rather than shrinking targets. 
- Header: mark + wordmark once, approximately 24dp mark; if width constrained use mark and live title at B1 page role. Do not stack a second large product name, subtitle and logo on compact gameplay or shelf pages. Provide a single accessible product name; avoid duplicate announcements.
- Clearspace: at least half rendered mark height around standalone lockups where practical; compact dock uses target spacing not a full marketing lockup. Keep glyph strokes visible at actual render scale. No mark on game artwork.
- Empty-state illustration: maximum 120dp wide, optional on compact landscape/TV if it steals interaction space. Decorative, excluded from accessibility; state title and real actions remain live text. Avoid repeating the illustration alongside an identical header mark. Never display unsupported actions or fake content.
- Selected, focus, loading, error and disabled states use shared B1 tokens and text, not brand geometry or colour alone. SafeSave's enclosure is not a backup-success icon. CARTMOD's module is not proof of an installed game. GEM's gemstone is not a rating score or verified eligibility indicator. No mark for destructive confirmation; use shared semantic warnings.

## Colour

Original default UI: GEM muted amethyst #C6A6D9; SafeSave mist blue #ADC6E1; CARTMOD warm brass #D9B18D. B1 approved warm Light appearance: #725281, #365D82, #865735 respectively. Product colour is a small optional accent, not page background, required identification or status meaning. Owner approved all product marks and the CARTDECK logo reacting to the selected appearance through contrast-safe semantic foreground/accent; geometry and wordmarks never change. All three remain distinguishable in monochrome. Do not save new appearance roles, change existing editions, or infer a colour from product data. Contrast ratios for the six B1 neutral backgrounds recorded in product-identities.json; custom appearances still require real contrast verification.

## Product presentation and boundaries

GEM: For You / Browse / My Ratings. Discovery is inviting but precise. Cold start offers unpersonalised browse; never fake recommendations, user scores, ownership or legal availability. GEMRating, GemFinder and DeckFit are subordinate features; do not imply scores are interchangeable. 
SafeSave: save inventory, verified status, import/export and recovery. Emphasise the actual scope, source and state. Explicit confirmations where necessary; no cloud claim. SafeSave Vault is an optional future feature only. 
CARTMOD: workshop action cards; PC media setup, managed installations, future Patch Studio/My Sources only when available. Hide detailed instructions until requested; imported media does not equal verified install. Keep canonical shared Shelf cartridges and existing storage protections.
Product copy proposals in the manifest/specimen are for visual review, not runtime strings or release claims.

## Owner review — 18 November 2026

Light and appearance-reactive logo tint are approved for eventual integration. GEM geometry remains unchanged. SafeSave and CARTMOD original marks are retained as option A, not locked; compare alternatives in `options/REVIEW.md` before selecting the final marks. No new product assets are promoted by default.

## Integration and verification

B2 delivers design sources, manifest, offline specimen and structural/contrast validation. B5/B6 own actual component states and Android conversion after approval. Inspect real 20/24/32dp marks, small/large fonts, monochrome, theme contrast, all supported tiers, touch/controller/TV focus, and compact viewport. Keep application builds isolated until verified recovery master. Existing source-gate, complete APK/unit tests and post-merge artifact checks remain mandatory. This package does not mount feature destinations, implement services or add branding to APK yet.
