# CARTDECK GC-01 — EXECUTE THE APPROVED EIGHT-WORKSTREAM IMPLEMENTATION
**This is a CODEX EXECUTION ORDER, not a research/roadmap prompt.**
Repository: safetyblade/ROMMachine. Owner Jonathan. Engineering Elliot. Design Sienna approved by owner. Architecture Daniel. QA Marcus. Director Cassia.
Issue #262 is the complete implementation programme; readiness #266, design #265 and requirements #264 are complete for their respective scopes. Current known master: 7362f442f88dc2de2260030ab11fc08e24fb8271. Update this only by reading master at actual start.

READ IN FULL BEFORE CODING:
1. docs/engineering/gc01/01_CASSIA_PROGRAMME_REQUIREMENTS.md
2. docs/engineering/gc01/02_DANIEL_SIENNA_DESIGN.md
3. docs/engineering/gc01/04_SIENNA_COMPLETE_UX_DESIGN.md
4. docs/engineering/gc01/05_SIENNA_SCREEN_PROTOTYPE.html (reference UX only, not Android implementation)
5. docs/engineering/gc01/03_ELLIOT_MARCUS_EXECUTION_VERIFICATION.md (exact CI, test and risk gates)
6. docs/engineering/GC01_GAMEPLAY_CONSISTENCY_EXECUTION_CONTRACT.md and #262.
Design files currently on engineering/gameplay-consistency-gc01 are docs-only and DIVERGED from master: do NOT merge or rebase historical native code from them into master. Preserve existing latest master. Create separate code implementation branch off current master only AFTER active emulation Codex lane relinquishes runtime integration; follow exactly current user approved scope.

GOAL: Deliver **all eight** approved gameplay workstreams as a consistent CARTDECK player experience. Do NOT return a plan instead of code; actually change source and open testable PR(s). One single CARTDECK-owned native Compose menu; reuse current GameMenuActivity, SharedGameMenu and XboxGameMenu. Do not introduce parallel input/render/menu architectures. Preserve Cemu Vulkan lifecycle merged in PR #263.

IMPLEMENT:
A. Canonical CARTDECK menu: real host-owned capabilities and asynchronous outcome, Save&Load only if supported; original Xbox recovery guard stays safe, X360/PS2/WiiU native UI with truthful pause/exit/status; TV DPAD.
B. Actual game framebuffer source-aspect FIT to measured safe viewport no crop/no stretch; top bleed fixed; DS 2 screens and stylus, WiiU letterbox touch and swapchain unaffected, phone tablet TV and controller overlay.
C. Enabled Bluetooth/USB controller detection, original port and per-console semantics, initial setup, capture/Undo/Reset/Test/calibrate, axes/held/release/chord/hotplug and touch fallback; no global duplicate listeners.
D. Wii Sideways, Nunchuk, Classic sourced from core capability, truthful lack of real IR/motion.
E. Both Standard+GripArc and PivotArc+GripArc, both sides, G1–G4 stable, 48dp target geometry and ALL native controls; safe session-only fallback without overwriting owner preference.
F. Modern Function only exact verified title/region/revision game actions, explicit opt-in, Console Authentic safe default and Custom mapping/Undo. Do not claim test matrix theoretical actions as game verified.
G. Xbox360 offline Xenia account/profile persistence, actual game-native saved progress roundtrip and install-over; no fake emulator save state.
H. PS1/PSP quality and regression tests, existing systems GB/GBA/DS/Wii/DC/GC/original Xbox plus new PS2/WiiU/X360 appropriately capability-gated; source-isolated rollback.

ENGINEERING SEQUENCE: Actual latest source and current PR audit at start (no PR collision), then code in cohesive reviewable source increments; shared menu/capability outcome first, continue every A–H workstream until either fully delivered or explicit evidenced blocker. Create issue-specific PRs linked to #262, never force merge. No splash-only or source-only claim of gameplay pass. Do not ask owner to redesign or choose an implementation order: work out dependencies in source. If PS2/WiiU host lacks a feasible pause operation, render explicit unsupported text, do not fake it.

CI EXACT: .github/workflows/cartdeck-connected-engines-apk.yml connected APK workflow, ktlintCheck :lemuroid-app:testFreeBundleDebugUnitTest :lemuroid-app:assembleFreeBundleDebug plus verify_standalone_apk.sh, verify_native_bundle_apk.py, verify_connected_apk.py. Actual master baseline run #43678592274 success, APK artifact ID 14146834742 (GC-01 not implemented in that build). Every test build: exact source SHA, pinned native bundle provenance, CI URL/step status, apk SHA256/size, versionCode, package dev.prestwich.cartdeck.debug, actual signing certificate fingerprint as extracted, ABI, rollback and evidence paths. Do not invent SHA/signature or version.

QA: Marcus independently tests per scenario matrix in 03 document, physical Samsung device and controller when actually available, tablet/TV if available, 2 screen DS, WiiU resize, X360 native profile save/reopen, PSP mod standalone same hash, PivotArc+GripArc. Mark untested NOT TESTED rather than PASS; compile green != gameplay. Preserve Room/cartridge UUID/SaveBinding/SRAM/native cards/HDD/NAND, ROM/SAF, art/shelf/appearance, physical config, install-over signature. No uninstall/clear data.

FINAL DELIVERY to #262: per A–H exact changed files/PRs, CI and testing outcome, APK artifact with true hash, device matrix, blockers, rollback, Marcus findings and Cassia recommendation for Jonathan's acceptance. If blocked, state precise cause and continue other independent work. Do not close #262 until implemented and evidence recorded; do not silently shrink scope.
