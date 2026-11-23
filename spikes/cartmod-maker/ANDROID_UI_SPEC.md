# Android Maker UX contract (future Compose implementation)

**Not yet compiled or wired to CARTDECK.** The spike supplies data contracts for a later \`CARTMOD > Maker (Beta)\` route. The current app's navigational and save controls remain unchanged.

## Route hierarchy

- \`MakerHome\`: title and Beta flag; **New game**, **My projects**, **Examples**, **Import project**; no account.
- \`NewGame\`: target choices **GBC** and **GBA** (badged Planned until their real exporters work); choose \`Grid Puzzle\` or \`Little Shop\`. Do not present an inactive output as available.
- \`MakerEditor\`: app bar with project title, Undo, Redo and Save. Pixel/tile canvas in centre. Bottom compact modes: **World**, **Objects**, **Rules**, **Art**, **Test**.
- \`PropertySheet\`: selecting a tile/object opens a bottom sheet. Numeric fields, toggles, target pickers, palette controls; full touchscreen targets and accessible text.
- \`RuleEditor\`: simple event cards: **WHEN** [Enter / Interact] **ON** [tile] **IF** [variable comparison value] **DO** [Set / Add variable]. Show a friendly error if values have incompatible types or the GBC budget is exceeded. No text scripting.
- \`Preview\`: start/stop without modifying source project; arrows/DPAD + Interact. Clearly label this an editor preview, not a console-emulation pass.
- \`Build\`: show *Unavailable: native GBC/GBA adapter not verified* until a genuine ROM pipeline passes; no fake success, extension-only renaming, or hidden network work.
- \`MyProjects\`: explicit **Export editable project**, **Import**, **Duplicate**, **Delete** with confirmation and storage limits.

## Touch interactions

Tap-to-place is the primary accessible interaction; drag-to-paint optional. Pinch zoom and two-finger pan navigate the world without placing tiles. Long-press opens properties. Undo/redo is command-based; editing a rule has reversible changes. On tablets/landscape show canvas and inspector side by side. Android Back returns to editor and prompts to save only when changes exist.

## Layout and state architecture

Use Jetpack Compose screens and ViewModel-like state holders only after PC gate. Proposed \`MakerProjectRepository\` persists local editable JSON via atomic staging/rename and explicit SAF import/export; avoid untrusted path traversal, unsafe archive extraction and overwriting existing projects. Use versioned migration from schemaVersion 1. Editor preview should be implemented as a pure shared Kotlin rules interpreter over the same project vocabulary, tested against the Python contract fixtures. Do not depend on internet, AI, users' ROMs, or an external build service for basic authoring.

CARTDECK's cartridge UUID and SaveBinding are separate from Maker's projectId. One source can produce multiple immutable versions; update an existing user's generated cartridge only through an explicit, transactional action. Do not access arbitrary emulator save paths.

## Initial example gallery

- Pocket Puzzle (original) — 5x5 tile maze with goal trigger.
- Pocket Shop (original) — two stock items, sell action, cash and win state.
- Vowmarked and Clockwork Quarry — **not bundled** until owning project teams approve gameplay, canon and source assets. Examples must be user-editable copies; originals remain unchanged.

## Accessibility and QA

Focus and touch targets >=48dp where possible; TalkBack labels for tiles/actors; font scaling, contrast and colour-independent state; no hover-only actions. Verify portrait, landscape, low-memory/resume, denied SAF import, Android back, Junior settings, TV/controller focus and offline state. Proposed hardware-specific warnings are advisory before native exporter gate, not measured resource reports.