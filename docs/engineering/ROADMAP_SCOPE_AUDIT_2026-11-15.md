# CARTDECK — ROADMAP-TO-EXECUTION AUDIT AND PREWORK GATES
Date 15 Nov 2026 | Status AUDIT / NOT CERTIFICATION
Owner Jonathan | Director Cassia | Engineering Elliot | Architecture Daniel | UX Sienna | QA Marcus

## Authoritative inputs / priority rules
1. Owner 15 Nov directive: Priority 1 Gameplay Consistency, native CARTDECK-owned in-game menus and no cropping or stretching. Latest specific decision supersedes older Fill-to-stretch requirement.
2. Cassia controlled Drive Roadmap: https://docs.google.com/document/d/1wlURBkCoASXTAOFbXrNQWzODEbnVuMJBUz4l68Vac4Q/edit . Includes dated historical updates; latest owner-approved 14–15 Nov priority controls.
3. GitHub engineering mirror: docs/engineering/CARTDECK_MASTER_ROADMAP.md (13 Nov v2.6, HISTORICAL for later changes; older Fill-to-stretch instructions no longer govern).
4. Owner 14 Nov closeout #247, baseline control #246, independent bulk QA #248, post-emu experience #253, library #145, current gameplay #262. Latest source, CI and owner device evidence supersede stale issue assertions. Explicit source-to-roadmap reconciliation remains REQUIRED and NOT COMPLETE.

## Audited roadmap coverage — status of PREWORK, not feature completion
| Cassia workstream | Primary evidence to inspect | GC-01 prior inclusion | Missing before declaring scoped or implementation-ready |
| --- | --- | --- | --- |
| P0 integrated baseline, branch reconciliation, update/rollback | #246 #145 #248; full master CI | General instruction | Exact current SHA, branch collisions, APK/signature and rollback-tested data snapshot |
| P1 Gameplay consistency universal menu | #191 PR #200 #262 | Detailed requirement | Complete engine-by-engine host capability and menu action call-path, Daniel/Sienna approval, per-engine state/keyboard/TV and error testing |
| P1 Screen/aspect and app/TV | #74 #162 #165 #166 #167 #224 | Partial | Renderer-specific geometry and pixel-aspect tests; dual screen/gesture; full shelf/tablet/Leanback scope; current no-stretch decision propagation |
| P1 physical controller/Modern Function/Wii/GripArc | #85 #135 #172 #176–181 #224 #231 | Partial | Actual hardware issue reproduction, device descriptors, verified exact title action matrix, input arbiter proof, native engine port parity |
| P1 emulation quality and new engines | #56 #82 #84 #85 #93 #114 #143 #197 #218 #232 #233 #247 #261, recent PRs #249–257 | Partial references | Engine/runtime/packaging/CI/current handset A/B per-console, specific regressions, same-game save continuity and priority race reconciliation |
| P2 cartridge shell and shelf | #155 #183 #211 #253; PR #215 | Protection only | Front visibility, licensed logo policy, horizontal sections, scroll vs drag, reflow/undo/facing persistence UX, actual install-over acceptance |
| P2 artwork and ROM management | #7 #115 #145–148 #253 | Protection only | Image search/upload/reset/source priority; SAF ownership; hide/delete/undo/archive/backup; recover/transaction/invariants and permission handling |
| P2 CartMods | #212 #213 #253 | Missing | Patch/archive/PSP texture source and licensing, edition/save isolation, UI + rollback, coverage beyond research |
| P2 offline GEM, ratings, DeckFit | #193 #194 #210 #237 #247 #253 | Protection only | Offline static catalogue provenance, no GEM web-search conflation, source/edition matching, user GemRating, DeckFit criteria, schema/accessibility, safe integration |
| P3 appearance and promotion | #75 #199 #223 #227 #242 #247 #253 | Protection only | Existing approved Original/Pink/Green/StealthAcid and appearance/lighting regression matrix; no unapproved feature expansion |
| Longer-term/conditional consoles and discovery | #60–62 #83 #86 #195 #198 #247 #253 | Not in prompt | Explicit parked/conditional classification; no implied current-playable promise, separate source/licensing/technical feasibility gates |
| Cross-stream independent QA and delivery | #246 #247 #248 | General instruction | Same-SHA consolidated exact APK, owner installed data upgrade, physical Samsung/Bluetooth/tablet/TV results; no completed full QA evidence |

## Minimum PREWORK before authorising implementation for any stream
- Enumerate every requirement and the exact controlling owner decision, source file/function, issue and PR, acceptance test, and whether already implemented/partial/blocked/unknown.
- Enumerate dependencies to active PRs, native toolchains, existing saves/Room and engine packages; identify conflict and approved fallback.
- Provide source paths, runtime sequence/call graph, architecture choice with alternatives/tradeoffs, Sienna UX states/wireframes where required, Daniel explicit signed review and approval.
- Resolve known competing implementations before applying broad patches. Research source licences, native ABI, hardware capability and legal test fixtures where applicable.
- Define deterministic failure, regression and adversarial test cases, same-SHA integrated APK build, upgrade/rollback procedure and Marcus hardware QA handoff.
- Record what cannot be prevalidated and why; use status NOT COMPLETED rather than checked because referenced. Owner does not serve as substitute for team preflight.
- A crosswalk is not itself proof of completed architecture, source trace or hardware QA.

## Controlled scope boundaries
GC-01 executes eight approved Priority 1 gameplay areas, with full testable implementation; it MUST inspect cross-stream risks but may not silently ship shelf redesign, mods, GEM, visual expansion or new consoles merely because they are linked. Those are separate Cassia-ranked programme waves, each subject to the same execution discipline. Parallel research is fine; mainline changes remain gated and staged.
Conflicting display history: old #167/v2.6 Fill stretched images; latest 15 Nov owner rule is NO CROP / NO STRETCH, maximum safe FIT preserving correct pixel aspect. Get owner approval before any stretch option, not default.
No universal PSP/PS2/WiiU/X360 gameplay PASS inferred from native builds or catalogue listing.
No automatic catalogue downloads/scraping; GEM is offline static, separate from optional user-added web source search per 15 Nov decisions.

## Team deliverables before calling ROADMAP FULLY SCOPED
1. Cassia-approved single master requirement register spanning the streams above, with clear priority and deferred classification.
2. Daniel-approved architecture contracts and integration/dependency graph, per-runtime diagrams and protected data contracts.
3. Sienna-approved native gameplay UX and shell/shelf/TV interaction specifications per active stream.
4. Elliot verified source and CI audit, proof of feasible native APIs, exact issue-to-PR map and independently runnable implementation instructions.
5. Marcus traceable acceptance matrix and device/fixture availability; no hardware PASS until actual gameplay tests.
6. Jonathan confirms any new prioritisation or conflict resolution and is sole final product acceptance.
