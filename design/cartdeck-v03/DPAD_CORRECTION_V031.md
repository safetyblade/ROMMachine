# CARTDECK D-pad design consistency correction — v0.3.1

**Review-only design change. No Android application modifications.**

## Finding

The approved v0.2 Original/Pink device concept has a compact, chassis-integrated angular D-pad with two short horizontal direction bars and a small circular centre hub. The merged design-source PR #6 instead substituted a large circular mounting disc with four triangular arrows and a prominent plus-shaped centre. This changed the identity of the hardware and was not specifically approved.

## Corrective source

- `controls/dpad-master.svg` now has one transparent-background chamfered cross silhouette, understated metal edge, two bars and a small central hub. No circular mounting plate.
- SVG shape geometry is identical across editions; editable `data-cd-fill` and `data-cd-stroke` semantic tokens express colours.
- `build_theme_assets.py` generates recoloured D-pad assets for Original/Pink/Mono/Green from the same master and rejects any large circular base.
- Four independent `arrow-*.svg` assets remain available for optional accessibility cues or floating controls, but are NOT baked into the default console D-pad.
- `test_dpad_consistency.py` covers the approved geometry, different palette outputs, edition invariants and absence of a ring.

### Approval and dependencies

- Original and Pink colour directions approved; Green and Mono are demonstrations only.
- Cassia/Jonathan must review corrected visual against v0.2 concept before final asset lock.
- No interactive hit area, D-pad mapping, touch latency, emulator rendering, ROM scanner or save changes are proposed. Visual size is adapted by the native layout, not this SVG.
- Existing P0 and eventual P1 implementation gates remain unchanged.

### Reproduce

```sh
python design/cartdeck-v03/build_theme_assets.py
python -m unittest discover design/cartdeck-v03 -p 'test_dpad_consistency.py'
```

Approved reference: CARTDECK Creative Refinement v0.2, first board showing the Original/Pink hardware side by side. Issue #4; follows merged PR #6.
