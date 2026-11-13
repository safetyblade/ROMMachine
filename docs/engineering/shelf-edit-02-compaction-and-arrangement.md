# SHELF-EDIT-02 — Compact, organise and undo
Tracking: https://github.com/safetyblade/ROMMachine/issues/183

## Reproduced issue in code
A drop onto empty end-of-row visual padding previously used `DropPosition.spaces` to add persisted `ShelfCell(gameKey=null)` vacancies. These survive subsequent removal of indexed cartridge records because successful refresh deliberately preserves user-created spaces. "Reserved" also wraps poorly in narrow columns.

## Focused correction
- Visual row padding is never persisted as a reserved slot after drag. Drag to empty space simply moves the game at the section destination.
- Explicit Insert space / Insert empty row remains available.
- Editing dock changes to **Done · Tools · Undo**; Tools retains Add section and adds **Remove all empty spaces**, **Auto-arrange A–Z**, **Auto-arrange by console**.
- Remove all empty spaces deletes presentation-only reserved cells and empty rows, maintaining game order.
- A-Z and console group each section independently; they remove gaps and never move games into a different section.
- Display compact **Empty** in place of wrapping "Reserved". Existing per-cell Remove empty space still exists.
- All edits are atomic through existing ShelfStore and included in the existing Undo history. ROMs, Room records, save states, VMU and cartridge key identities untouched.

## Required owner device gate
- My Games with persistent Reserved spaces: Tools -> Remove all empty spaces -> restored tight shelf, Undo -> previous arrangement.
- Tools -> Auto-arrange A-Z / by console -> order and sections correct; Undo functional.
- Drag into visually empty trailing shelf position; reopen: **no new persistent space**.
- Delete ROM then successful library refresh: no new blank generated; explicit pre-existing blank may remain until removed by user.
- App restart, install-over, low-width phone, large tablet and TV navigation; no lost saves/cartridges or forced visual rearrangement when browsing.
- CI lint, unit tests and integrated APK first; runtime/device acceptance must not be inferred from code review.
