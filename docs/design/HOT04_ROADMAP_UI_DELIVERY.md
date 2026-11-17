# HOT-04 — Roadmap UI delivery, 17 November 2026

Sources: controlled Drive roadmap (1wlURBkCoASXTAOFbXrNQWzODEbnVuMJBUz4l68Vac4Q), docs/engineering/CARTDECK_MASTER_ROADMAP.md, HOT03 design, JR01 final interaction contract. PR #294/312/317 confirmed merged. HOT is a scoped UI branch, not a fourth accountable delivery lane; PERF/GC/CARTMOD remain accountable. No engine, build, database, save, permission or native input policy changes.

| Work | Code delivered | Remaining |
| --- | --- | --- |
| EX-01 #155 | Front title and console badge bounded in distinct zones | visual audit with all editions/font sizes |
| EX-02 #303 | Horizontal named-section pager, tabs, independent saved scroll state, full layout in arrange/filter | Controller focus, long-press edit and cross-section navigation verification; GC-TV connection and app section navigation follow-up |
| EX-03 #155 | Explicit browser artwork search from Setup, existing local upload/replace/reset | In-app authorised image provider, preview and direct selection not built; no automatic download |
| HOT03 CARTMOD / PERF-PC | Actual workspace, route and Shelf/CARTMOD dock, existing importer in workspace, Settings shortcut | Installed executable and guest runtime remain PERF; no new runtime or promise of readiness |
| EX-04 #145–148 | Existing library/recovery actions preserved | Safe import/delete/archive, profile hiding, portable backup require real transactions |
| EX-05 #212 / #148 | Callback workshop/action, source editor and patch review components | Not mounted: durable My Sources/search, verified patch/deployment, edition registry and storage protection |
| EX-06 #199 etc | Existing appearance controls protected | Per-cartridge finishes/section colours not expanded during cosmetic hold; require scoped persistence |
| EX-07 #135/#172/#224 | Existing merged gameplay controls and display UIs preserved | GripArc measured solver/input coverage, action resolver, Wii controller profiles, controller ownership/pointer and multiport belong to GC and PERF; not cosmetic-only |
| EX-08 #193/#194/#210 | GEM list and rating editor components; 1–10 / 1–5 scale supplied by integration | Not mounted: catalogue/verified identity/licensing, rating store/DeckFit integration |
| EX-09 #165/#166 | Native TV browse title/palette, shared shelf section paging | Native TV shelf parity/ten-foot focus, tablet/device verification, rendering/input pipeline remain GC/PERF |
| EX-10 | Existing appearance/lighting protected | No new lighting or promotion per controlling roadmap |
| PROF/JR #289/#288 | Player chooser, single-cover playroom, Grown-Ups components | Not mounted: profile service, per-player visibility and saves, verified parent gate, input and launcher auth |
| SAVE #290/#293 | SafeSave inventory and operation preview components | Not mounted: real inventories/manifests, atomic import/export/recovery, authorised transfers; Vault future backend |

ProductSurfaces.kt and ProductWorkflows.kt intentionally contain UI-only callback presentation components, not navigation-enabled feature stubs. A caller must supply actual authorised data/actions, not sample records or success states. No inactive top-level product destinations. Settings PC shortcut retained to actual CARTMOD route. Current media import remains experimental and not evidence of a working game install.

Before full release: reconcile master; run ktlint and app Kotlin compile/unit/Compose tests; check selected section, filtered/manual layout restoration, grid density, five-column portrait and front/spine, existing tools/Undo, controller focus and no background shelf leakage, ART external browser cancellation and local artwork persistence, PC importer availability and original media retention, return navigation, TV remote UI, install-over/save continuity. Existing tests updated and focused UI tests added, none executed in this connector session. No APK, merge or QA pass claimed.