# P1-A2 architecture review packet

Controlling issue: #43. Corrections: #40. Bookshelf material authority: #44.

## Shelf presentation boundary (slice 2)

`ShelfLayout` is version 1 presentation state in app-private `files/cartdeck/shelf-layout-v1.json`. It contains section ordering, stable cell identities, intentional vacancies, full empty rows and appearance. It does not alter game records, ROM paths, grants, scanner, emulator cores, saves or custom-cover files. A system ID and exact source URI hash identifies a cartridge independently of Room numeric IDs. Missing cartridges remain reserved and regain their cells on return. New identities append to Unsorted. Deleting a section rehomes every cell to Unsorted.

Writes read the latest revision under a path-wide mutex and use Android AtomicFile with rollback. Unsupported/corrupt files disable edits and expose retry; they are never silently replaced. Undo is bounded to ten session operations and reconciles newly scanned identities. Cancellation cannot leave a half-written presentation file. Undo is intentionally session-local, while committed layout survives process death.

The UI is a LazyColumn of responsive physical rows. Only visible spines and image requests compose. Persistent cell identities survive changes in column count; an explicit empty-row marker remains a full row on phone or tablet. Scroll restoration anchors a cell identity rather than a numeric row index. Drag uses root-space visible targets and edge scrolling. Accessible move, section and vacancy controls provide alternatives to gestures.

Original/Pink shelf finishes use all twelve canonical roles from PR #44. Overrides are presentation-only. Text and focus colors are adjusted for contrast. Hardware retinting touches vector paths only, never cover image pixels or gameplay colors.

## Review gate

Daniel's architecture approval and Marcus's independent regression checks are outstanding. A passing build or asset test is not evidence of their approval. Keep runtime PRs in draft until the complete package and Android evidence are reviewed. Do not close #40/#43 based on this slice.

Required independent scenarios: indexed/custom/reset/missing artwork; large/long titles; narrow/landscape/tablet/200% text; cross-section drag and edge scroll; intentional gaps/rows after force-stop and rescan; corrupt layout recovery; original/custom palette contrast; real save/resume; signing-compatible in-place update without uninstall or clear-data.

## Additive title search (slice 3)

Database 10 → 11 adds only `fts_game_titles` and four maintenance triggers. Both `title` and `customName` are indexed as distinct columns with Unicode tokenisation and diacritic removal. Legacy `fts_games`, Game rows/IDs, datafiles, cover paths and custom metadata remain intact. Fresh database callbacks create both indexes. The application no longer enables destructive fallback for unknown migration paths: unsupported databases fail open rather than wiping a library.

`TitleSearch` extracts bounded Unicode letter/number tokens (1024 input characters, 16 tokens, 64 characters/token), emits generated quoted FTS4 prefix literals joined with AND, and binds every query/ranking parameter. User quotes, parentheses, wildcards, column expressions and SQL operators cannot enter MATCH syntax. Ranking is exact original/custom title, then whole-input prefix, then stable display title/ID. Paging bounds result loads; debounce is 200ms with latest-query cancellation. UI loading/error/retry follows Paging LoadState without a timer. Favourites and Search keep separate navigation routes and models.

Migration validation uses the exported real v10 Room schema, opens it with migration 10→11, compares every seeded Game field, verifies original/custom partial matches and legacy index continuity, and checks private cover/save sentinel bytes. This proves the additive database migration under test; it does not substitute for an owner-device update or real gameplay save/resume. Daniel must review this schema/index boundary before merge.

## Physical pickup (slice 4)

The library theme owns a root-coordinate registry of visible cartridge bounds. Registrations carry owner tokens so disposal of an old cell cannot erase its replacement after a move/reflow. Pickup snapshots the selected ID and its measured cell bounds. Animated copies do not register as shelf cells. The held source is hidden only once the measured detail target is ready, avoiding a blank selection while Room loads.

An in-root modal layer lifts, travels and rotates the spine into the independently rendered real cover. The forward timeline is 500ms; Back returns over 350ms only if the source registration still matches. Missing sources and screen/font/density changes resolve to a static detail instead of travelling to stale coordinates. System animator disable, saved reduced-motion preference, low-power and low-RAM modes bypass motion. Background semantics and transition pointer input are blocked; detail controls become available after settling. Selection is ID-based, rapid repeated selection is ignored, and Open remains the only gameplay dispatch with duplicate-tap consumption.

The unchanged emulator/scanner/save/custom-cover boundaries still require Marcus's independent gameplay regression and Daniel's architecture approval. QA emulation can establish native rendering and synthetic update continuity; it cannot certify owner-device performance or an existing real gameplay save.
