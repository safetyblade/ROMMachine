# CARTDECK — Android development baseline and update contract

**Status:** ACTIVE — development/test baseline authorised by Jonathan Prestwich, 8 November 2026.
**Project Director:** Cassia Rowan · **UI / console experience:** Sienna Vale · **Engineering:** Elliot Shaw · **Architecture:** Daniel Mercer · **Independent QA:** Marcus Reid.

## Baseline selected

**CARTDECK standalone v2001** is the approved foundation for further app development.

- Source SHA: `7cd600f87914c776155e261693ae0abb9b54b756`
- Original APK SHA-256: `70ccf3e8f59ffc670ed40a273a6fdcfd15c6b4c9202a478c8db15b8119205576`
- Package installed: `dev.prestwich.cartdeck.debug`
- Version code: `2001`
- ARM64 test variant: `freeBundleDebug` with opt-in `-Pcartdeck.dev.standaloneId=true`
- Debug signing certificate SHA-256: `ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8`
- Build evidence: https://github.com/safetyblade/ROMMachine/actions/runs/42461321501
- Verified baseline file and provenance: https://drive.google.com/drive/folders/1OJASR9jpRrsqQLSoYOglFwmovpGJ6Z27
- User acceptance: installed and opens correctly on user's Android phone. Most initial behaviours appear to pass. **This is a user-confirmed launch baseline, not independent full regression acceptance.**

## Accepted issue at baseline

**CD-UX-NOTIF-01 — notification status-bar icon retains old upstream artwork.**

Root cause confirmed in `NotificationsManager.kt`: game running, library scan, core install and save synchronisation each used `R.drawable.ic_lemuroid_tiny`. Existing APK contains the historic tiny upstream icon. Fix this by using a new, white monochrome `R.drawable.ic_cartdeck_notification` with the exact approved C-slot master geometry. Preserve notification IDs, foreground-service behaviour and channel registration. No visual change to emulator game screen or touch controls.

## Every subsequent development APK must

1. Build from latest approved CARTDECK `master` and use the standalone workflow `.github/workflows/cartdeck-standalone-debug.yml`.
2. Keep **`dev.prestwich.cartdeck.debug`**, same DEBUG certificate and versionCode strictly above the installed previous build. The debug key is inherited; **not a production key**.
3. Ship as `CARTDECK_STANDALONE_ARM64_DEBUG.apk` plus checksum, source SHA, package/signature/version evidence.
4. Update via install-over-the-top. **Do not uninstall, clear storage or reset game data.**
5. Verify launch, library, ROM picker permission, GB/GBC/GBA play, save RAM, manual save state, controller/touch and notification icon at least at each major merge.
6. Attach a release/update evidence record to GitHub and the Drive preview build folder. Preserve previous APKs for rollback/reference.
7. Never change emulator/core/save internals for a cosmetic/UI work package without a separately accepted architecture and QA gate.
8. Only modify current CARTDECK display assets in Sienna's controlled edition family. The approved default remains Original; Pink and future editions must remain separately selectable and editable.

## Next implementation sequence

- **BR-N1:** tiny notification icon correction → build next standalone APK and verify in-place update from v2001.
- **P1-A:** branded home, guided Add Cartridges, Continue, physical bookshelf and editable covers.
- **P1-B:** console shell presentation and approved D-pad / face button assets; hit zones unchanged.
- **P1-C:** console / floating / fullscreen presentation modes, preserving consistent viewport sizes even for later PS1 titles.
- **P1-D:** boot experience and settings refinement; all motion optional/non-blocking.

Sienna and Cassia control the presentation specification; Elliot implements feature packages; Marcus independently tests runtime behaviour. Design v0.4 universal asset kit may be consumed after its approval and asset checks, not substituted for proven runtime.

## Boundaries

- Current debug certificate is inherited, and public distribution is not authorised.
- Production `dev.prestwich.cartdeck` and secure prestwich.dev-owned signing need a separate migration and source/licensing clearance.
- Old `com.swordfish.lemuroid.debug` installation remains independent; do not claim its saves migrated.
- Retain upstream source attribution, GPL notices and applicable emulator-core licence restrictions.

**Result:** v2001 is our accepted **development starting line**; future successful versions become upgrade-qualified only after the above package, signing and save checks.
