> **LATEST OWNER REFINEMENT — 22 NOV 2026:** Read [GC01_INPUT_MOTION_GEM_JUNIOR_OWNER_LOCK_2026-11-22.md](GC01_INPUT_MOTION_GEM_JUNIOR_OWNER_LOCK_2026-11-22.md) first. It supersedes this document's controller conflict default (automatic reciprocal **SWAP**, not Replace/Cancel default); Junior verification UX (simple local parent **PIN** for exit/settings, no mandatory device credential); and combines app-side Wii + 3DS motion/virtual inputs under **GC**, with native sensor/backend bridges retained in **PERF**. GEM user ratings remain whole numbers. SafeSave Export All remains as specified. Historical conflicting details below are not controlling.

# CARTDECK GC-01 — Owner decisions, SafeSave all-console dashboard and direct controller remapping
**Owner instruction:** Jonathan Prestwich, 22 November 2026 (AEDT)  
**Status:** **OWNER-APPROVED product direction** OD-01 through OD-06 as refined here; **NOT** engineering implementation, architecture sign-off, physical QA, merge approval or release sign-off.  
**Branch:** existing draft PR #362, \`engineering/gc01-landscape-surround-parity-20261121\`.

**Supersedes conflicting "NOT APPROVED / recommended" text** in \`GC01_TEAM_DECISION_RECOMMENDATIONS_2026-11-22.md\` for OD-01...OD-06. Prior decisions are historical, not pending. Original GC A–H issue #262 remains the overall completion criterion; independent QA #248, SafeSave #290, GEM #194 and controller #308/#172 retain traceability. Engineering must verify current runtime/source before writing implementation.

## 1. Ratified owner decisions

| ID | Approved direction | Implementation boundary |
| --- | --- | --- |
| **OD-01 Display** | Original image proportions / Aspect Fit is the recommended default; an explicit optional renderer-backed Stretch to Fill may be offered with clear distortion warning, **no crop**, and all existing saved preferences preserved. | Daniel to reconcile existing current-source renderer and historical #167 before implementing a changed default. No silent migration or AndroidView-only false fullscreen. |
| **OD-02 Junior** | Manager authentication before Junior unrestricted exit/elevation and recovery must be real: device-backed credential where available, otherwise securely verified rate-limited Manager PIN with enrolled recovery proof. | Secure actual Android/TV fallback, retain save data, no bypass and no OS-wide kiosk claim. |
| **OD-03 DeckFit precision** | **ALWAYS WHOLE NUMBERS**. My DeckFit is integer 1–5, My GemRating integer 1–10. No half-star/half-point input, display, store or later migration planned; null means Unrated. | Every UI, schema, export, import, catalogue and test must enforce this forever unless Jonathan explicitly changes the product requirement. Editorial DeckFit estimates may also be integer only or Not yet assessed. |
| **OD-04 Controller mapping** | Controller detection and **live visual, direct two-press mapping** described in §2 below are mandatory. Three-layer normalization/guest/Custom > opted-in verified Modern > Authentic precedence from the design review is accepted, subject to non-destructive existing binding migration. | Do not replace a working mapping without confirmation; preserve native engine ownership, Back/menu escape, player ports and existing saved profile. |
| **OD-05 Unassigned backups** | Safe, validated backups for absent/ambiguous titles remain private, visible in **SafeSave → Unassigned Backups**, exportable in the all-backup operation, not silently auto-linked or auto-deleted; a separate explicit verified binding is required before restore to an installed game. | No ROM/license content, guessed identity, hidden backup, access leak or executable guest storage. |
| **OD-06 Save database** | Retain the already-created **SafeSave v1 snapshot/index store**, add only necessary non-destructive profile/binding records to main Room and maintain crash-safe cross-store operation/recovery journalling. | Inspect source before choosing actual Room migration version/transaction design; prove upgrade, old snapshots, legacy aliases and original save state continuity. |

The owner also expressly expands SafeSave's **visible per-console save/state counts** and **Export All** to the maximum verifiable coverage, with all exceptions documented for further investigation instead of blocking valid saves.

## 2. OD-04 — precise "press original, press replacement" controller mapping

### 2.1 Player journey
**Settings → Controllers → [actual connected controller] → Map Buttons → choose Console/Game → capture mode.** On a native TV or DeX, everything must be usable with that gamepad or the remote, no touch/pointer required. Show the gamepad's actual reported name, current physical input layout, supported controls and selected guest/game port.

**Direct two-press binding, fully visual:**

1. Instruction: **"Press the button you want to change."**
2. Player presses **L2** on the connected physical pad. The controller diagram **lights up L2 immediately**, shows the original native console input/action it currently triggers, and freezes that as the source mapping row. Real analogue triggers must have a deliberate threshold/hysteresis so merely resting a finger or drifting does not capture; differentiate click and axis.
3. Instruction: **"Now press the button you want to use instead."**
4. Player presses the replacement button, e.g. **X**. Its diagram position lights up; preview reads **"When you press X, the game will receive L2"** (for a guest whose L2 is valid). This is a *physical-source binding change* and must NOT change the native game's required L2 semantics.
5. If X was assigned, show **Swap / Replace (unassign conflicting mapping) / Cancel**, with visual before-and-after and warning if the change leaves an essential native guest control unmapped. Default to **Cancel/no write** until explicit decision; only allow intentional multi-bind where backend safely supports it.
6. **Test Mapping** receives the replacement physical X press/release and shows **actual outgoing guest L2 down/up** on the active engine/port; check persistence and no guest event leakage while the editor is open.
7. **Save**, explicit confirmation and toast; **Undo**, **Reset This Game**, **Reset Device Defaults**. A partially completed capture never auto-saves; disconnect, Back or app interruption cancels draft without changing stored mappings.

An alternate **tap a diagram button / choose from list** route remains available for accessibility and hardware that cannot emit its current source key. The two-press flow is the primary gamepad UX, not a touchscreen-only wizard.

### 2.2 Visual requirements
- High-contrast full controller outline or capability-accurate generic fallback; real-time glow/animation for observed button/axis, clear current vs proposed binding and detected physical controller label.
- Show mapped native guest button **AND** verified game action separately. Example: "Your controller X → PS2 L2 → Brake (only if verified title-specific action)." Do not conflate physical Xbox A, printed PlayStation Cross and native button ID.
- A labelled status flow **Step 1 / Step 2 / Test / Save** with clear active stage, timeout/cancel and safe controller Back/Guide/menu escape.
- Full source-native inventory including L1/L2/R1/R2, L3/R3, Start/Select, DPAD, face buttons, both sticks/axes, trigger analogue values and genuine special functions.
- Controller profile, chosen game/cartridge, guest port P1/P2 and mapping scope (This Game vs Device Default) visible before Save.
- Unknown gamepad first-use discovery is press-to-identify by *observed Android source*, not assume ABXY lettering; appropriate default position-based layout retained for known controllers.
- **Input capture exclusivity:** one active owner; no shelf/game/Menu activation while mapping; flush held inputs and cancel uncommitted drafts on focus loss/reconnect.
- Retain per-game Authentic, Modern Function (verified exact game/region/edition and opt-in), Custom. No global A/B reversal.

### 2.3 Mapping acceptance cases
- Press physical L2 to select, press X to replace, confirm conflict, Test physically sees X produce correct guest L2 and releases it, Save, reopen and reconnect: result persists.
- For analogue L2/RT, capture edge/threshold rather than repeated noisy axis events; test full/partial, neutral and release.
- Two identical branded controllers on P1/P2 must not overwrite each other's stored mappings or accidentally reassign menu ownership.
- Modern Function mapped Accelerate must output the correct original guest key held, independent of controller face-button printed label.
- TV-only remote can navigate all mapping screens; only actual supported gameplay controller is assigned guest port.
- Unknown button, non-emittable Guide/Home, Wii IR/gyro, 3DS tilt, DS stylus or peripheral-only input is labelled capability-dependent, not faked by a generic button mapping.

## 3. OD-05/OD-06 — SafeSave console dashboard and ALL export

### 3.1 Required top-level Save Centre
**SafeSave** is a visible major product destination (not Advanced or a hidden developer option). Its home shows:
- **Total Games With Saves**: count distinct installed game/cartridge identities with eligible discovered native save groups; do not multiply per-file components.
- **Total Save States**: manual, autosave and resume grouped in distinct counters; never conflate files and slots.
- **Whole Cards/VMUs**: physical/shared banks grouped by actual engine and owner; disclose that they may contain several games.
- **Unassigned Backups**: separately count valid orphaned imported archives, their size and status.
- **Backups**: last successfully verified local backup, archive count/total size, warnings and incomplete coverage.
- Large primary **Back Up Everything / Export All** action; a narrower **Back Up Game / Console / Player / Card** and **Restore / Import**.

**Example presentation, ILLUSTRATIVE ONLY — never hardcode these numbers:**

| Console | Games with in-game saves | Manual states | Auto/resume states | Whole cards | Backup status |
|---|---:|---:|---:|---:|---|
| **Xbox** | 5 | 0 if unsupported | 0 if unsupported | 0 | 5 supported games; savestate unavailable |
| **PS2** | 5 | 3 if supported | 2 if supported | 2 complete cards | Two cards contain multiple games |
| **GBA** | 12 | 4 | 3 | 0 | All verified snapshot types eligible |
| **Unassigned** | — | — | — | — | 2 verified orphaned archives |

The display must **derive counts live** from installed cartridges, actual native save-group memberships, state slots and verified archive index. "5 saves in Xbox" must not mean five arbitrary files when one game uses five files. Primary wording **"5 games with saves"**; secondary detail **"12 save files"** where useful. No fabricated number or false full coverage.

Each console is a focusable row accessible through phone/touch, gamepad and TV remote. Selecting PS2 shows each game/native save, memory cards, state slots and source/verification status; selecting Xbox shows native guest save groups and an honest "emulator save states unavailable" message where Xenia lacks them.

### 3.2 Export Everything: support as far as technically possible
Pressing **Export All** does not fail just because one engine is unverified or cannot create save states. It:
1. Discovers every registered active console/backend, each authorised player (Manager gate for other people), game-save group, manual state slot, auto/resume state and complete shared memory-card/VMU bank.
2. Classifies **SUPPORTED_VERIFIED**, **DETECTED_UNVERIFIED**, **SKIPPED_LOCKED**, **UNSUPPORTED**, **EXCLUDED_MIXED_CONTENT** and **LEGACY_UNASSIGNED** separately. Include supported data **only after actual adapter/path/hash checks**.
3. Displays a plain-English pre-export preview: "Exporting [N] games with saves, [S] states, [C] cards, [P] players"; plus **"Exceptions requiring research [N]"** with system, exact missing capability/reason. The owner may export all eligible groups without waiting for research on unsupported groups.
4. Safely quiesces writers, snapshots each group, verifies hashes, serializes versioned bounded .cartsave payload using Android SAF or local backup, and writes a manifest with all omitted/excluded system reasons.
5. Shows an outcome **"All currently supported progress exported"** (only when true), counts included/excluded and exact external provider verification status; do not say **"EVERY save protected"** if any detected item is excluded. A partial export creates a valid recovery receipt; restore is group-atomic and excludes unverified/unsafe content.
6. Includes previously imported, valid **Unassigned Backups** as a separate optional/exported inventory group with original unmodified backup bytes and metadata; disclose original imported archive nesting/duplication and deduplicate safely by validated content hash. No unbounded recursive archive inclusion.

### 3.3 Exception Research Register
Populate an **Exceptions** tab directly in Save Centre, viewable on all platforms, grouped by console and engine. Each row displays:
- Save type requested (native, manual state, auto/resume, whole card), actual scanned count and coverage classification.
- Specific block reason: adapter absent, guest data mixed with NAND/HDD, no savestate API, locked/engine active, unsupported core version, identity ambiguous, corrupted source or insufficient storage.
- Consequence for last Export All, user-safe fallback and whether another verified method exists.
- **Research status**: Needs adapter / Needs engine support / Needs proof / Source excluded / Resolved.
- Engineering reference #290 or related issue, evidence date, confirmed version and appropriate repro (no private paths/secrets).
- No invented completion times, no auto-claim of full console support.

The Exceptions view is an **observable truth ledger**, not an excuse to leave functionality unimplemented. Continue implementing every viable supported adapter. Where save-only extraction cannot be safely proven (e.g. mixed-content Xbox HDD, NAND), preserve actual user data and flag unsupported with next research action.

### 3.4 Persistence and restoration
- Retain SafeSave v1 snapshots and separate index as approved, but **index every discovered supported engine and state type**, never PSP-only. Preserve main Room canonical UUID + legacy aliases, per-profile ownership and non-destructive migrations.
- In-game saves and emulator savestates remain distinct, with pinned engine/ROM/version compatibility for load.
- A shared memory card export is the whole bank and prompts before affecting multiple titles.
- Imported unassigned backups remain locally visible and eligible for Export All, read-only until verified manual match.
- Exports/imports use streamed bounded archives, SHA-256, safe paths, no mixed OS/NAND/HDD or ROM content; restore always backups existing progress first, journals and rolls back on failure.
- An archive's checksum is integrity only; does not authenticate its sender/identity. Show accurate warnings on external .cartsave import.
- Interface must remain usable offline with actual Android SAF providers. Stored source under legacy Play sync remains protected from concurrent mutation.

## 4. GEM — owner update and next catalogue-source sprint

**OD-03 is permanent whole-number precision.** Roxy's next two-hour source pass is a **targeted game-database expansion**, not another DeckFit scoring revision. Existing B7-A combined bundled candidate: **88,246 release records, 30 mapped systems, not unique games or physically tested**. Remaining declared missing domains: **FinalBurn Neo (FBNeo), MAME2003Plus and DOS**.

**Recommended FIRST candidate for Roxy:** pinned **FinalBurn Neo arcade DAT** from an upstream/core-aligned, independently verifiable source, checked against the exact CARTDECK shipped FBNeo core. Compare source options from finalburnneo/FBNeo and libretro/libretro-database; distinguish each source's actual dataset licence from emulator/core non-commercial licence. The libretro-database repo advertises **CC BY-SA 4.0** generally while FBNeo itself is non-commercial; this does not establish every third-party derived DAT is freely redistributable. **Do not import assets, ROMs, BIOS, artwork or external downloads, and do not publish a new APK/dataset without source-specific permission verification.**

**Deliverable before next GC Codex run:** provenance/date/commit/license/citations; parser/metadata schema compatibility; exact core name + arcade shortname/clone/parent identities; no title-only automatic matches, standalone FBNeo namespace; counts before/after, duplicates/quarantine, test fixtures, reproducible offline staging, source archive digest and compatibility with existing 88,246 unchanged IDs. If FBNeo DAT licensing blocks redistribution, **do not ingest into app**; return licence findings and choose a rights-cleared alternative (MAME2003Plus DAT or a properly attributed DOS metadata source) for the same preflight. No game binaries or artwork acquisition.

**Scheduling note:** A GitHub issue comment is a task handoff, not an active independent Roxy job. If an automation is scheduled it performs an explicit future research/check, not an unobserved background ingestion process. Source insertion remains a separately evidenced engineering action after permitted data provenance.

## 5. Remaining minor open policy choices — recommendations, not blockers to approved work

The six previous OD owner decisions are now CLOSED at product level; engineering implementation and physical QA remain OPEN. The following are new design minutiae needing a written recommended default, not a general re-scoping round:

| Follow-up | Recommended baseline | Reason |
|---|---|---|
| **FG-01 Auto backup and retention** | Always offer **manual Back Up Everything**. No surprise silent deletion/rotation of existing save versions. When engine quiesce and storage capacity are verified, offer opt-in backup-on-successful-game-exit. Warn and skip when storage low, show archive space used and user-controlled cleanup. | Auto-copy of huge native guest files could fill device; user must control storage. |
| **FG-02 Mapping conflict choices** | Explicit **Swap / Replace / Cancel** with pre-save visual, **Cancel default**, no silent duplicate binding. Unsupported missing native control is blocking warning. | A direct two-press mapper must not break L2 or other essential controls unexpectedly. |
| **FG-03 Export archive security** | Default local Export All prompts a privacy warning because player/game progress and possible personal game metadata are included; no mandatory online login or password. Optional encryption only if tested/approved and compatible with recoverable imports. | Maintain offline, portable restoration without implying archive hashes are encryption/authentication. |
| **FG-04 Cross-core savestate transfer** | Always require a verified compatible core/engine state format and version for load. For mismatches, import as inert backup if safe; native in-game save portability only when real adapter proof exists. | Prevent false restorability and corrupted progress. |
| **FG-05 Data and metadata licensing** | Each source must pass copyright/data-source provenance, allowed redistribution/attribution, pack version and independent rollback before shipping. A catalogue gap is visible until lawful data is found. | Roxy can research freely; not every source can ship bundled. |

**No other unresolved high-level GC product decision was identified in the inspected A–H design.** Active **implementation gaps** (SafeSave adapters/UI, native map dispatch, Wii, GripArc, GEM, physical TV/DeX, signature audio/framing and full console QA) must not be relabelled as design problems to avoid implementation.

## 6. Ready-to-execute engineering handoff

Read this owner-approved document first, then original #262 A–H and approved Sienna M00–M24, previous GC cross-feature and team supplement, #290 technical source audit, #308 controller design, #194 GEM. Resolve source conflicts narrowly, never restart a general design cycle.

Next GC pass must deliver:
1. **Visible SafeSave console totals**, in-game saves vs states vs shared cards vs unassigned backups, real All export with declared exceptions and recoverable files.
2. **Two-press visually live controller mapper**: press old input L2, then desired replacement physical X, preview correct native output, handle conflicts and test actual pressed/released game key, Save with persistence; gamepad/TV/DeX complete journey.
3. Offline GEM, Roxy whole-number ratings and later verified catalogue expansion.
4. All remaining **original GC A–H** features and explicit system evidence.
5. Same-head signed ARM64 APK and unit/Room/native/UI security evidence. Physical Marcus/owner acceptance remains NOT TESTED until performed; no merge without Jonathan.

No docs-only delivery may be presented as completion of this handoff.
