# ROM Machine / Cartkin — Project Foundation (Draft)

Status: Project approved, product branding proposed (not locked)
Product director: Helena Ward
Product owner: Jonathan Prestwich
Engineering lead: Elliot Shaw
Design: Sienna Vale
QA: Marcus Reid
Repository: safetyblade/ROMMachine
Base branch: master

## Product vision
A polished, offline-first native Android home for legally obtained retro games. Users import files from their device, see a welcoming library, press Play, and reliably resume. Phone and tablet first; preserve Android TV/controller capability without making TV the first-release acceptance blocker.

**Proposed product name:** Cartkin by prestwich.dev.
Alternatives: ROMKIN; ROM Machine (working repository name).
Do not change the app ID, package name, app icons, store listing, or release signing until product naming approval. Do not use Nintendo/Pokémon marks in app branding.

## Source foundation and constraints
- Existing repository is a Lemuroid-based application; extend rather than rewrite.
- Preserve its ROM scanner, game library, LibretroDroid integration, gamepad, save and core handling until verified alternatives exist.
- Existing modules include lemuroid-app, lemuroid-touchinput, retrograde-app-shared, bundled-cores, and an upstream lemuroid-cores submodule.
- Root tooling currently declares Android Gradle Plugin 8.4.0, and wrapper uses Gradle 8.10.2. Do not mass-upgrade until the baseline compiles.
- Original app ID is com.swordfish.lemuroid; proposed fresh ID after approval is dev.prestwich.cartkin.
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
- Original Cartkin icon, TV banner, palette and design tokens; owner: Sienna.
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
- Design: develop Cartkin and ROMKIN naming/logo studies; do not lock app name without owner approval.
- QA: prepare lawful homebrew ROM and GB/GBC/GBA test matrix.
- Legal/licensing: verify GPLv3 compliance, core licensing and distribution method before public APK.
