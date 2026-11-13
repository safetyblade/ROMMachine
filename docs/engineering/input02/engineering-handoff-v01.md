# INPUT-02 — Bounded Codex / Elliot handoff (implementation requires owner-approved matrix)
**Use after owner reviews #176 and [draft PR #175](https://github.com/safetyblade/ROMMachine/pull/175).**
**Do not implement all six issue scopes in one PR. Use connector-first on latest master, rebase and check existing #122/#135/#162 work.**

## Input layer — [#177](https://github.com/safetyblade/ROMMachine/issues/177)
Inspect `InputDeviceManager`, `LemuroidInputDeviceGamePad`, `InputBindingUpdater`, `InputClassGamePad` and `GameViewModelInput`. Implement testable `PhysicalInputAction` normalizer mapping Android gamepad sources to SOUTH/EAST/WEST/NORTH, DPAD, LEFT/RIGHT_STICK, L/R bumpers, L/R triggers (analog 0..1), Start/Select/Menu and clicks. Ensure descriptor-safe device identity and multiplayer port ownership. Do not delete the existing default A/B and X/Y flips unless pure mapping tests prove a replacement, and preserve users' custom bindings. Add capability fixtures. Keep feature off/rollback and do not touch emulator cores. Return one focused PR and actual tests.

## Semantic profile resolver — [#178](https://github.com/safetyblade/ROMMachine/issues/178)
Using approved evidence/native-key maps from #176 and normalized positions from #177, implement versioned per-cartridge, core/controller-type Modern Function profiles. Verify title/edition matches before auto-activation **after opt-in**, unknown titles stay Console Authentic, suggested genre requires Apply. Map original controls, NOT UI letters or Android KEYCODE numbers. Implement trigger digital hold/hysteresis, analog pass-through only where supported, duplicate-source ownership, menu exclusion and hotplug/revert. Cases: SNES Super Mario World B=Jump SOUTH vs Mario Kart B=Accelerate RT; GBA kart A=Accelerate RT; PS2 GT3 Cross RT; Dreamcast analog RT. Include fallback and no save migration. One focused implementation PR; no UI cosmetics added.

## Controller UX — [#179](https://github.com/safetyblade/ROMMachine/issues/179)
Sienna designs a simple per-cartridge Controls page and controller test wizard. Offer Console Authentic / Modern Function / Custom; readable 'action → modern physical button → original console input'; marked Verified/Suggested/Custom, one-click Undo and safe state migration. TV menu focus/remote/controller accessible; touch UI only when permitted by Display workstream. Tests cover persistence and unknown fallback. Keep existing edition/material designs; no changes to game rendering.

## Content catalogue — [#180](https://github.com/safetyblade/ROMMachine/issues/180)
Audit manuals/source, genre and game identities with review status. Research seed in matrix v0.1 is not a runtime profile database. Provide verified entries and coverage dashboard by console × genre, highlight unsupported special Wii/DS/motion cases. No copyrighted ROM downloads or guessing.

## QA — [#181](https://github.com/safetyblade/ROMMachine/issues/181)
Use QA matrix and per-device input logs. Exercise actual gameplay, not compile alone. One integrated install-over-compatible ARM64 APK after each bounded PR qualifies; no release or auto-activation without Marcus and owner verified real hardware results and rollback.

## Required status response per lane
`Source head | PR | main changes | CI PASS/FAIL/NOT TESTED | On-device PASS/FAIL/NOT TESTED | Console exceptions | blockers/next action`.
Avoid verification loops; do not mark architecture tests as game-specific input acceptance.
