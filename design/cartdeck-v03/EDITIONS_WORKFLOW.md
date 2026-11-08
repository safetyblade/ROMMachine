# Adding a CARTDECK edition

1. Copy `editions/original.json` to `editions/<edition-id>.json`.
2. Modify **only** the `edition` object: ID, approved status, shell/trim/accent/focus/on-colours. Every locked key (identity, core screen ratios, modes, input guardrails) must stay byte-for-byte equivalent to Original.
3. Run `python design/cartdeck-v03/build_theme_assets.py` from the repo root. The builder validates colours and principal WCAG text contrast, preserves the D-pad and A/B vector path geometry, writes recoloured copies into `generated/<edition-id>/`, and leaves the master SVGs unchanged.
4. Test Original vs the new edition in the **independent preview** shipped in the controlled Drive package, including press/focus/disabled states, 48dp touch targets, portrait/landscape, low brightness and colour vision scenarios.
5. Cassia reviews that the app still feels like CARTDECK. Jonathan approves the edition. Daniel ensures no theme asset changed native input, emulator core, save RAM/state, performance or user accessibility preferences.
6. Only after verified P0 APK, GB/GBC/GBA gameplay and save persistence may Elliot integrate the approved asset pack in P1. Marcus performs independent input/screen/save regression.

**Appearance-only contract:** app logic uses native/vector rendering plus runtime colours. Never put touch hit zones, save metadata, ROM paths, licensed box art, executables, network URLs or emulator configuration inside edition files. Green is an unapproved demonstration, not a shipping theme.

Complete v0.3 editor and preview ZIP: https://drive.google.com/file/d/18KoC8cX3CR2WruXgQTM0gohCdx_evYxu/view
