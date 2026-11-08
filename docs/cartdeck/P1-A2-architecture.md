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
