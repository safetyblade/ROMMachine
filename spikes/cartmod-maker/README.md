# CARTMOD Maker — GBC/GBA isolated scaffold

**Status: research spike only.** No CARTDECK navigation, Android APK, emulator core, commercial game, live save or canonical source file is changed by this directory. Do not merge into an active release without the PC-first and owner gates in [#212](https://github.com/safetyblade/ROMMachine/issues/212) and [#304](https://github.com/safetyblade/ROMMachine/issues/304).

## What works now

- One **versioned, open, editable** project JSON contract (\`project.schema.json\`, v1).
- Two **original** tiny templates, \`Pocket Puzzle\` and \`Pocket Shop\`; no copied brand/game assets.
- A **deterministic reference preview**, \`maker_reference.py\`, supporting a tile grid, actor movement, blocked tiles, interactive tiles, state variables, conditional rules, actions, and step limits.
- A **bounded contract validator** against malformed and oversized inputs.
- \`tests/test_contract.py\` checks the prototypes, rule semantics, safety limits, target budgets, and JSON round-trip.
- \`ANDROID_UI_SPEC.md\`, \`EXPORT_ADAPTER_CONTRACT.md\`, and \`UPSTREAM_REGISTER.md\` describe the future touch-first Android integration and native compiler/runtime work.

## Run without dependencies

From the repository root:

\`\`\`sh
python3 -m unittest discover -s spikes/cartmod-maker/tests -v
python3 spikes/cartmod-maker/maker_reference.py validate spikes/cartmod-maker/examples/pocket-puzzle.cartproj.json
python3 spikes/cartmod-maker/maker_reference.py preview spikes/cartmod-maker/examples/pocket-puzzle.cartproj.json --actions R,R,D
python3 spikes/cartmod-maker/maker_reference.py preview spikes/cartmod-maker/examples/pocket-shop.cartproj.json --actions I,I,I
\`\`\`

Expected sample results: Puzzle \`won=true\` after moving right, right, down; Shop \`coins=6, stock=0, won=true\` after three interactions. The player may edit sample JSON in any text editor and retest the behaviour. This is **not yet a drag-and-drop Android editor**.

## What is NOT present

- No native \`.gb\`/\`.gbc\` or \`.gba\` export, toolchain install or emulator integration.
- No CARTMOD menu or compiled Jetpack Compose screen.
- No claim that Python preview means the game runs on a Game Boy / Game Boy Advance.
- No PSP, 3D, arbitrary scripts, third-party artwork, or online service.
- No actual versioned Android save project storage or SafeSave registration.

The \`targets\` array currently expresses **design intent**, not availability of a build adapter. The schema is experimental; future revisions need explicit migrations.

## Engineering sequence after PC gate

1. Author a limited data-driven native GBC runtime against GBDK/GB Studio references; compile once through a verified desktop/CI toolchain. Boot and save it in an independent emulator.
2. Implement a safe GBC packer that injects validated project-data resources into the proven runtime and produces a **real** ROM with correct header, memory banking, title and checksums. Compare Python/Kotlin preview and native behaviour.
3. Repeat with a separately built GBA runtime using Butano and its target-specific memory/graphics/saving constraints.
4. Write the Kotlin/Jetpack Compose Android editor and on-device offline persistence; connect its data contract and preview first, native build adapters only after validation.
5. Bring signed/hashed outputs into CARTDECK as new, stable, user-approved cartridges without changing existing originals or SaveBindings. Confirm actual install-over and device QA.

See also: [Drive design hub](https://drive.google.com/drive/folders/1TVoxc3PRttSr_gPtYVqO0B2k6xw8tGPG).