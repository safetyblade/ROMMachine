# CARTDECK Junior — JR-01A Product & Interaction Design Specification
Version: 1.0 design baseline, 2026-11-16
Status: Product direction approved (Hybrid CARTDECK Junior + customisable Playroom); detailed UX specification complete for architecture review, not implementation approval.
Owner: Sienna Vale (Visual Design). Roxy: curation. Daniel: architecture review. Elliot: subsequent implementation. Marcus: independent acceptance.
Canonical tracking: Issue #288. Do not merge implementation into active GC-01/CI-01 gate.

## 0. Identity and intent
Product: CARTDECK Junior. Child-facing destination: My Playroom. Main shelf: My Games / My Deck. Parent-only space: Grown-Ups. No tagline (BRAND-01 B3 owner approval supersedes earlier tagline direction).
Hybrid: retains CARTDECK cartridge identity, game artwork, launch machinery, universal pause/menu and save bindings. Offers optional Junior-specific visual themes. Not a separate executable, ROM source, emulation core or automatic family-safety classification.
Primary target: early/non-readers around six years; inclusive of ages and tastes. Inclusive palette (not gender-stereotyped).

## 1. Experience principles / non-negotiables
- Default-deny content: only manually parent-approved cartridge IDs, never all ROMs, even when rating metadata missing. Explicit reapproval after cartridge identity changes.
- Profile-isolated gameplay sessions, autosaves, save states, screenshots, recent games, favourites and preferences. Preserve legacy adult saves. Don't promise isolation until architecture tests pass.
- No child access to unrestricted library, file import/delete, settings, cores, remap, account/network actions, purchases/external URLs, diagnostics or developer tools. Security enforced below UI at every navigation and game-launch entry point.
- No bundled copyright ROMs or artwork assumption: mockups placeholders are not content entitlements.
- Gameplay may include in-game text, violence or mechanics; CARTDECK cannot automatically change them. Parent controls curation, regional ratings informational only.
- Works offline, on touch and controller, and without GEM/GEMRating.

## 2. Entry, onboarding and return journeys
JR-S01 Choose Player: on first Junior setup show standard mode unchanged; enabled Junior profiles presented by avatar, first name/nickname, optional avatar colour. Selecting a child enters locked Junior context immediately; choosing adult/unrestricted path from Junior context requires parent auth. Remember last player only if parent setting permits; app cold launch does not unlock adult by default.
JR-S02 Create: Adult library > Collections > Create Junior profile; name/nickname optional default Player 1, age band informational, child avatar, theme, optional parent PIN setup (required if not using device-backed gate); choose game set; preview; confirm. No need for email, birth date, account, Internet.
JR-S03 Avatar: curated built-in icon set (animals, fantasy, stars, vehicles etc), no real photos necessary. No cross-child identification exposed outside local device.
JR-S04 Theme: choose Candy Garden / Dino Adventure / Space Explorer / Ocean Friends / default CARTDECK. Child can subsequently change among parent-allowed themes only.
JR-S05 Approve: all imported canonical cartridges listed only in authenticated parent editor, searchable by title/system; add individually or multi-select; ratings advisory and unknown games clearly indicated; group into folders/collections such as My Pets or Adventures. Games never automatically selected from suggested ratings. Any disallowed/removed cartridge disappears from Junior launch surfaces including deep-links/history.
JR-S06 Review: name/avatar/theme, count and explicit game names, content info unknown flags, PIN recovery check, 'Preview as child' (not a privileged bypass), Save & Enter.
JR-S07 Home: greeting/avatar, Resume [only if a currently allowed safe game and valid save exists], My Games, Favourites, optional 1-3 named collection rows; persistent Grown-Ups affordance (lock) not overwhelming.
JR-S08 Games: big original cartridge artwork with readable short labels and console icon; fallback brand-colour cartridge if no art; navigation fixed and finite; no unrestricted search, downloads or feeds; hidden metadata never auto-shows disallowed games. Sorting is parent selected or child's recent/favourite among allowlist.
JR-S09 Detail: one cover, title, simple play CTA, favourite toggle, optional 'Continue' when valid; no launch settings, age claims or technical platform info beyond console symbol; long-press does not reveal parental features.
JR-S10 In-game: existing GC-01 universal menu trigger must be consistent touch/gamepad/TV. Junior presentation Resume, Save Progress, Return to My Playroom. On save fail: preserve running session where possible, honest 'Couldn't save yet' message, retry, do not imply success. Potential platform/core missing save-state support: show only truthful available operations; in-game saves remain a separate mechanism. No destructive load/overwrite without protected explicit choice; never show other profiles' save slots. If a game cannot launch offer 'This game needs a grown-up' and safe return.
JR-S11 Favourites: child may favourite/unfavourite only allowed games; automatically prune disallowed links.
JR-S12 Personalise: parent-scoped change set, theme and permitted avatar, favourite ordering, optional sound/animation reduction; no wider permissions.
JR-S13 Gate: parent-auth on every exit to adult, unrestricted profile, settings, import/delete, developer, file picker, advanced menu and external navigation; on controller back/home/guide/cold start/deep link and launcher shortcuts; prevent UI-only bypass and stale auth tokens. Parent authentication remains usable offline; PIN recovery via authenticated OS/device owner or established secure parent credential, never a child-readable hint or 'forgot' bypass.
JR-S14 Dashboard: list of child profiles, games, appearance, progress and settings. Separate parent preview mode with verified exit back to locked Junior context.
JR-S15 Collections: add/remove/reorder/group game IDs; never move/delete ROM when removing from a child collection; on remove during active play show parent that change takes effect immediately or safely terminate session; handling must be architecture-defined, no access to disallowed next game.
JR-S16 Saves: each child owns separate save namespace by child profile + canonical cartridge identity + save type/slot; optional adult-authorised copy (copy, never move or overwrite) requires conflict preview, explicit confirmation and tested atomic operation. Child profile deletion prompts parent with count of local saves and optional export/backup where supported; retention and undo design must be defined before implementation. No silent data loss on migration, reinstall or rollback.
JR-S17 Controls: optional session reminders (not forced hard stop in MVP), age metadata display, screen-time rules in later increment only if reliably enforceable while safeguarding saves, permissions for appearance changes and creation of favourites; no spurious monitoring claims.
JR-S18 Recovery: authenticated parent procedure; if recovery unavailable explain limitation and preserve game/profile data without circumventing gate.

## 3. Responsive composition
Portrait handheld (roughly 320-479 dp): header/avatar + compact action; single prominent 'Continue'; shelf carousel 2-3 large covers across, vertical category sections; bottom navigation My Games / Favourites / My Playroom; no tiny targets; game detail full-screen; protected parent editor vertical steps.
Landscape handheld (short height, 480-900 dp wide): left avatar/actions rail, primary horizontal cartridge row, no tall stacked hero; one gesture/controller press launches; 2D focus traversal; in-game menu not obscured by virtual buttons or cutouts.
Tablet (600+ dp width): optional two-column side rail and multi-row shelves; portrait/landscape supported; parent editor split list/selected tray with confirmation; grid sizing avoids giant empty bezel.
TV 10-foot interface: safe-area margins at least 5% horizontal and vertical; large legible titles, 3-6 covers per row depending 1080p/4K scaling; bright visible focus ring and predictable D-pad order; no touch-only gesture; confirm/back/controller home/pause maps tested; parent PIN entry via controller without displaying PIN; no on-screen keyboard dependency for child.
Adaptive breakpoints are design intent not hard-coded Android px; account for font scaling, display cutouts, accessibility and screen density. Rotate without resetting selection/game/save.

## 4. Visual system
Brand-level CARTDECK remains unchanged. Junior wordmark is a child-facing lockup; avoid imitating third-party game artwork, licensed mascots or trademarks in bundled theme assets.
Themes: Candy Garden (#F9D8EE base, #B64AB9 header, #ED79AC accent); Dino Adventure (#D1EFCB, #408B58, #80BD5B); Space Explorer (#D6DEFD, #354C98, #6D89E0); Ocean Friends (#C8EDF1, #158DA2, #72CBD7). Require contrast verification, typography and focus colors beyond these exploratory hex values. Default CARTDECK theme also available.
Use 48dp minimum touch target, 56-64dp primary controls for easy interaction, 24sp+ child-visible primary heading where space allows; TV sizes scaled for viewing distance; never depend on colour alone. Respect OS reduce motion and screen reader semantic labels. Tactile cartridge cards preserve box art aspect ratio, with non-licensed fallback. Background decor low-noise; no animated video wallpapers by default.
No permanent pink or animal theme assumption.

## 5. Navigation and input contract
Touch: single tap select, Play prominent, back safe. Long press only child-safe favourite/reorder if enabled. No hidden tech menus.
Controller: D-pad/stick focus -> A/confirm -> B/back; universal menu binding from active GC-01, no conflicts with original game's Start/Select. The parent gate is reachable but never bypassed through shoulder/system buttons. Gamepad disconnect resumes via touchscreen/alternative controller; preserve selected card.
TV: visible focus state, first focus on Resume or first approved cartridge, circular navigation only where visually obvious; never automatically launch disallowed last-played item. Return to Home safe, authentic native controller quit restrictions may require system-level threat model.
Accessibility: spoken labels and game names; screen-reader focus order; optional reduced animations, high contrast; large-text layouts; no reliance on reading for primary actions; no sound essential to understand actions. DS microphone/touch requirements and platform-specific quirks shown to parent in editor (if known) not silently promised to child.

## 6. Error and destructive states
Empty collection: cheerful shelf illustration + 'Ask a grown-up to add games'; parent gate. Missing ROM or unavailable core: non-scary 'This game isn't ready' and Back, no file browser. Missing art: generic matching cartridge (never game-identity confusion). Save failure: retry/clear truth, no false success. Power-loss: preserve atomic writes. Offline: all local functions operate. Profile deleted: adult authenticated, clear data retention choice, never delete physical game files. Changed game metadata, moved ROM or remapped identity: fail closed and offer authenticated re-link; protect SaveBinding on migration and downgrade. PIN attempts: throttle and lock safely, offer recovery only via verified parent path; no permanent loss due to UI control.

## 7. Content curation and GEM future
Child collection is a first-class parent-authored allowlist. Suggestions are offered only in parent editor, tagged with provenance and caveats, strictly require approval, and must not imply safety based on game cover/artwork or GEMRating alone. GEM ratings/recommendations are optional later. Roxy can curate genre sets (pets, Barbie, puzzles etc) but no bundled games nor licensing claims.

## 8. Acceptance criteria for independent QA
A. Two child profiles with different game sets, save namespace, favourite and themes; switching never leaks disallowed games.
B. All launch surfaces (search/recent/deep links/notifications/controller/home/menu) enforce allowed canonical cartridge identity and authenticated session.
C. Full parent onboarding, PIN authentication, recovery, create/edit/delete collection and child profile on touch and controller.
D. Child navigation on portrait handheld, landscape handheld, tablet portrait/landscape, and TV without needing reading, touchscreen or keyboard where unsuitable.
E. Save/resume and rollback continuity across supported engines and DS-specific controls; intentional negative tests for interruption and corrupted/missing state.
F. Remove/disallow during session, library scan, wrong game IDs, changed ROM location, uninstall/reinstall and app upgrade — no wrong game or save data accessed.
G. Accessibility/contrast/focus targets, reduced motion and high contrast validated.
H. Feature flag disabled by default until release approved; existing adult library UX, appearance and GC-01 gameplay menu unchanged.

## 9. Work packages and handoff
JR-01A (Sienna) — screen flows/design system/responsive specs/interaction states (this document).
JR-01B (Daniel) — technical design: parent auth threat model, canonical collections schema, stable profile IDs, SaveBinding isolation/migrations, permission checks in repository and launcher, rollback.
JR-01C (Elliot) — implement only on separately authorised branch, after active CI/GC physical checkpoint.
JR-01D (Marcus) — independent QA plan + explicit hardware acceptance, no claiming compatibility before physical testing.
JR-01E (Roxy) — optional parent-facing curated recommendations from existing imported games, subject to suitability.
Deferred: mandatory play timers, external sync, remote parental monitoring, online discovery inside child's space, public profiles/social, GEM-based autoadmission, additional emulation engines.

## 10. Explicit design vs architecture boundaries
This document approves behaviour and presentation, not readiness of authentication, age-label datasets, child protection legal claims, OS kiosk control or save isolation. Daniel must resolve all those constraints before engineering. Sienna design complete for JR-01A; review of mockups on real devices remains a separate stage.


## JR-01A Amendment 1 — Controller-approved design changes, 16 November 2026

**This amendment supersedes all conflicting baseline sections.** In particular: no multi-cover shelf/grid in Junior mode; universal player profiles, not Junior-only profiles. Product design only. Do not change active GC/CI implementation.

### Universal player architecture — PROF-01
- CARTDECK has multiple local player profiles, console-style, supporting at least three but with no hard-coded limit; each has immutable playerProfileId, nickname, avatar, theme, favourites, recent games, collections, visibility policy and isolated saves. All use one canonical, shared ROM library without duplicating games.
- Existing installation upgrades to a default manager/player with all existing saves intact; migration must be atomic/reversible and not break frozen legacy SaveBinding aliases.
- Manager can create/edit/delete profiles and administer Junior content; Standard sees all ROMs by default but can hide titles just for that profile (hidden does not delete cartridge or saves); Junior can see/launch only manager-approved canonical cartridge IDs, default deny even on new imports.
- Switching player uses dedicated Choose Player entry and persistent safe profile control; leaving Junior for any unrestricted profile or manager settings requires parent gate. Standard does not implicitly equal Manager.
- Avoid equating local player profile with cloud login. Optional online account linkage is later, explicit and per-profile; offline player switching remains complete.

### Junior carousel — One cartridge on screen
- My Playroom / My Games / Favourites and child collection categories display exactly **one** large cartridge cover at a time, centred, not a row of cards, grid, or multi-cover shelf.
- Swipe left/right switches exactly one cartridge on touch. D-pad left/right and optionally shoulder buttons provide equivalent controller/TV navigation. Clear previous/next affordances; small index indicator optional; avoid rendering adjacent game covers.
- Portrait handheld: centre large cover, brief title, system icon, Play or Continue, avatar and subdued Grown-Ups entry. Landscape handheld: dominant cover with controls alongside or beneath; no tall wasted header or cramped bottom. Tablet: same composition with wider whitespace, not a multi-column library. TV: one large cover within safe areas, strong focus ring and D-pad/Confirm/Back usability.
- Play launches directly from selected cartridge, skipping needless extra screens. Continue appears only for valid profile-owned game state. Return from gameplay restores carousel position. If game becomes disallowed/unavailable, select a safe remaining game without showing its prior metadata.
- One approved game: hide arrows/dots. No approved games: friendly empty state plus parent gate. Many games: parent-defined child collections/favourites enable narrower swiping without changing single-cartridge contract.

### Player save ownership — SAVE-01
- All profiles have separate battery/in-game saves, memory cards, savestates, autosaves, screenshots, and resume records, keyed by immutable profile ID and canonical cartridge ID plus emulator identity/type/slot as applicable. One game can belong to three players without any shared mutable save pointer.
- Before profile switches during active game: pause, save if supported, end/isolate session; never resume gameplay under the wrong player.
- Save inventory records player owner, game/canonical identity, platform, engine/core and version, save type/slot, timestamps, hashes/size, and storage location. Preserve engine-specific save formats and legacy aliases. Do not claim savestate formats are cross-engine compatible.
- Existing adult saves migrate safely to default profile, without unexpected overwrite or loss, with backups and rollback.

### Save Centre and portability — SAVE-02
- New Save Centre accessible to authorised standard player for own data and manager for child/cross-profile data. Actions: Export Save(s), Import Save(s), Copy to Another Player, View Backup/Recover, Manage Storage.
- Local export: use Android SAF to user-selected provider, versioned manifest and archive with content hashes and source metadata; never package ROMs. Support individual game and entire player backup with explicit size/display. Do not silently upload.
- Import: choose destination player, scan/validate manifest, identify game/core/type, preview conflicts and missing games, stage and atomically commit, offer restore-as-new or replace after secure backup and confirmation. On unsupported format/corrupt archive abort without harming existing saves; prevent path traversal and malicious file entry.
- Between-profile transfers always explicit copy by authorised player/manager; source remains intact; replacement destination requires backup/conflict approval. Never infer destination solely from filename.
- Cloud-ready design SAVE-03: optional linked account/storage upload/download is separate future feature, requiring explicit opt-in, verified auth, encryption, conflict/version rules and deletion/retention. No automatic cloud backup included in MVP.
- Engine-specific compatibility must be tested for GB/GBC/GBA, PS1, PSP, DS and supported later consoles, including PS2/PS3, GameCube/Wii/Wii U where present. Saves may be portable only within compatible engine/format; report honest restrictions.

### Parent management and permissions
- Parent gate protects access to unrestricted player switch, ROM import/delete, settings, other profiles' save data, transfers, account linking and management. PIN recovery must be verified, not a visible bypass.
- Parent collection editor uses shared library with allowlist per Junior. Standard's personal hidden list is never interpreted as Junior approval. Removal from Junior never deletes source ROM or saves.
- Deleting profile requires explicit options to archive/export/save data vs delete, and cannot silently remove a third party's saves.

### Added/revised screens
JR-S01 universal Choose Player. JR-S07/JR-S08/JR-S11 child one-cartridge experience. JR-S14 manager dashboard. JR-S19 Profile Manager; JR-S20 Standard Hidden Games; JR-S21 Save Centre; JR-S22 Export wizard; JR-S23 Import Preview/Conflicts; JR-S24 Copy Save to Player; JR-S25 Save Integrity/Recovery; JR-S26 Account Linking (future). The child never sees unrestricted screens.

### QA and sequencing
- At least three distinct players with separate progress and different visibility; switching safely closes active sessions.
- Junior shows exactly one cartridge on every device and supports swipe/controller traversal; orientation change and gameplay return preserve selected game.
- No unauthorized ROM can launch from Junior via resume, shortcut, deep link, recent history or universal menu.
- Save export/import byte round-trips, checksum/integrity, conflict and malicious archive checks; cross-profile copy proves no mutation of source.
- Upgrades/rollback preserve existing game identities and saves with existing ARCH-LIB-01 SaveBinding and GC-01 compatibility.
- Dependencies: PROF-01 (player model), LIB-PR-01 (visibility), SAVE-01 (ownership), SAVE-02 (portability), JR-01 (restricted experience), SAVE-03 (optional online backup later). Do not interrupt current emulation release lane.
