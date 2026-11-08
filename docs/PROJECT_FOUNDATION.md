# CARTDECK — Native Android Project Foundation (Active)

Status: Active project | Name CARTDECK approved 2026-11-08 | Owner Jonathan Prestwich
Project director and console experience lead: Cassia Rowan
Studio governance: Helena Ward
Product owner: Jonathan Prestwich
Engineering lead: Elliot Shaw
Design: Sienna Vale
QA: Marcus Reid
Repository: safetyblade/ROMMachine
Base branch: master

## Product vision
A polished, offline-first native Android home for legally obtained retro games. Users import files from their device, see a welcoming library, press Play, and reliably resume. Phone and tablet first; preserve Android TV/controller capability without making TV the first-release acceptance blocker.

**Approved product name:** CARTDECK by prestwich.dev. ROMMachine remains the repository name pending a separately approved GitHub rename.
Do not change the app ID, package name, app icons, store listing, or release signing until product naming approval. Do not use Nintendo/Pokémon marks in app branding.

## Source foundation and constraints
- Existing repository is a Lemuroid-based application; extend rather than rewrite.
- Preserve its ROM scanner, game library, LibretroDroid integration, gamepad, save and core handling until verified alternatives exist.
- Existing modules include lemuroid-app, lemuroid-touchinput, retrograde-app-shared, bundled-cores, and an upstream lemuroid-cores submodule.
- Root tooling currently declares Android Gradle Plugin 8.4.0, and wrapper uses Gradle 8.10.2. Do not mass-upgrade until the baseline compiles.
- Original app ID is com.swordfish.lemuroid; proposed fresh ID after approval is dev.prestwich.CARTDECK.
- The source currently has inherited release-signing credentials/configuration. Do not use upstream signing credentials. Generate a dedicated key, keep it out of the repository, and source credentials from secured CI secrets.
- Maintain Lemuroid copyright notices, source attribution and applicable GPLv3 obligations. Audit each bundled core, redistribution rights, third-party artwork, and dynamic download practices before release.
- Do not ship copyrighted commercial ROMs, copyrighted game art or proprietary BIOS files without necessary rights.
- No external ROM download catalogue, scraped links, or cloud account in Phase 1.

## P0: Technical foundation / first playable APK
1. Capture repository SHA and submodule SHAs, build tool versions and dependency state.
2. Check baseline assembly with recursive submodules using the existing Gradle wrapper. Document exact commands, environments and any failures. Do not claim a build succeeded without CI artefact or log.
3. Prove installation and launch on a supported Android device or emulator.
4. With legally distributable test/homebrew ROMs, test GB, GBC and GBA game launch; touch controls; external controller; save to cartridge SRAM; close/relaunch/resume; save state; importing files with Storage Access Framework.
5. Inventory current screens/navigation and design-retained functions; take evidence screenshots.
6. Define safe, minimal product-brand migration plan across app label, applicationId, namespace, TV banner, intent schemes, assets, notifications, settings and internal persistence paths. Avoid breaking source saves.
7. Add deterministic build CI that produces an unsigned/debug APK as a GitHub Actions artifact; include tests/lint where feasible. No live signing secrets in repo.
8. Record known failures, compatibility limits and recommended next changes in docs/P0_BASELINE.md.
9. Keep code changes on feature branches, PR review, and avoid commits directly to master.

## P0 acceptance gates
- CI produces downloadable debug APK from a pinned, repeatable source revision.
- Mobile app launches and scans a selected directory.
- At least one tested, lawful GB/GBC/GBA homebrew ROM per platform plays with appropriate controls.
- In-game progress survives an app restart and manual save/restore has no observed corruption.
- Attribution and bundled-core licence inventory documented.
- No ROM/BIOS files packaged in the APK.
- Original Lemuroid functionality not silently removed.

## Phase 1: Branded user experience (after name approval)
- Original device-first product icon, TV banner, palette and design tokens; owner: Sienna.
- Recent Games, Consoles, All Games, and Favourites with a single clear Play action.
- Clean per-game resume/save panel, settings and controller pairing.
- Local-only library metadata fallback; no copyrighted cover-art scraping without rights/permission.
- Keep Lemuroid's working navigation/emulation logic unless test results justify a change.

## Non-goals for initial APK
- Proprietary games/ROM downloads, 3DS/PSP quality promises, link-cable trading, cloud sync, cross-device backups, social features, achievements, emulator core rewriting, monetisation.

## Working model
Helena: scope, gates and release approvals.
Elliot: technical baseline, builds and integrations.
Sienna: brand and interaction design.
Marcus: device matrix and acceptance proof.

## Next actions
- Engineering: run P0 technical baseline and bring a real APK for review.
- Design: develop CartDeck, CartShell and CartPort naming/logo studies; do not lock app name without owner approval.
- QA: prepare lawful homebrew ROM and GB/GBC/GBA test matrix.
- Legal/licensing: verify GPLv3 compliance, core licensing and distribution method before public APK.

## Revised North Star — a device, not a launcher (8 Nov 2026)
The app must feel like playing on a real piece of gaming hardware. The emulator remains implementation infrastructure, not the consumer-facing experience. Like AUTV, preserve a beautiful direct core flow and keep optional capabilities out of the way of playing.

### Experience principles
1. Open the app into a personalised retro device home, not an emulator dashboard.
2. 'Insert cartridge → boot → play → suspend → resume' should be a simple, consistent interaction.
3. Authentic-looking custom shell, screen bezel, D-pad, start/select, face buttons, subtle haptics, LCD simulation and optional original boot animation/sound. Avoid copying protected hardware trademarks, logos or distinctive trade dress.
4. Device presentation adapts to console family: original monochrome handheld, colour handheld, wide GBA-style shell; DS dual-screen later. Screen art and controls must not obscure gameplay.
5. Phone: touch-based virtual hardware, ergonomic positioning and rotation. TV: controller-first ten-foot virtual console, full-screen gaming rather than persistent huge phone bezel. Tablet: scalable hybrid.
6. One light-touch control overlay for pause, save/load states, fast-forward, screenshot, video settings, controller remapping, exit. Hide it during normal play.
7. Per-game cartridge SRAM saving and resume reliability outrank any animation; include original boot aesthetic only after baseline passes.
8. Everything works offline with user-supplied lawful game files; no piracy, copycat Nintendo graphics or forced account.

### UX work package
- Design three sketches: (a) physical virtual handheld gameplay; (b) cartridge shelf/insert flow; (c) Android TV living-room console flow.
- Define optional hardware shell and full-screen immersive modes, including how the device controls map to touch/gamepad.
- Motion and haptics should be optional and lightweight; accessibility includes adjustable button sizes and high-contrast modes.
- Brand/icon study: CARTDECK (approved). Product owner approval before implementation.

### Repository independence
- Repository is presently based on Lemuroid. If confirmed as a fork, prefer GitHub's Leave fork network when eligible. The operation is irreversible and removes GitHub PR/issue metadata; preserve the content of the documentation branch / PR #1 before detaching. All upstream copyright attribution and licences continue to apply, even after detachment.
- Github Issues is a separately configurable repository feature and does not require detachment.
- If detachment is unavailable, use a separate clean repository and migrate the relevant code/history with a mirror push. Keep the old fork until migration/CI verified.

## PROJECT CONTROL — GOOGLE DRIVE
Project folder: https://drive.google.com/drive/folders/1O4Bc1p9IAEkiSWCPuauhtDEirGYX0r_1
00 — Controlled project record: https://docs.google.com/document/d/1PT4hNMylTfM2OIuWYBy-Ezd5kTXdae1I4A50g4poUX4/edit
01 — Delivery roadmap: https://docs.google.com/document/d/1wlURBkCoASXTAOFbXrNQWzODEbnVuMJBUz4l68Vac4Q/edit
02 — Source/licensing research register: https://docs.google.com/document/d/1GVPPHmMar0iSsPlDNYT1f9_lVWSKDqx4dLf4LorJWk0/edit
03 — Device-first console UX: https://docs.google.com/document/d/1qpLQiUYQ9bAazeZI8T4zZWOo8A3p5Ffux3CO35Abp9k/edit
04 — QA and release gates: https://docs.google.com/document/d/1XyX6RQCXp02yXLZxU2iqodnGKzaD2e8a9dY0w-pEnZI/edit
05 — Cassia Rowan lead role/revive: https://docs.google.com/document/d/1pK_16WC59D9k2VbBrtt5B5LQN0_vgMXL7B_08JpMmvc/edit

Do not treat any roadmap or source research description as a passed APK test. User confirms the Lemuroid fork was detached 2026-11-08; verify repository metadata, PR history, issues settings and branch state live.
