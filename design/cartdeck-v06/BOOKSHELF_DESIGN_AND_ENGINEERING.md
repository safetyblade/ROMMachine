# CARTDECK — Bookshelf Materials and V1→V2 Delivery Contract

**Source:** `bookshelf-palette.json` (12 independent semantic bookshelf tokens per Original/Pink edition). This design layer is separate from cartridge finish. Hardware family / cartridge shape remains locked in design direction; changing the room, back panel or rail colour must not change any game, ROM, input or save data.

## Why its own palette

The original approved palettes colour a handheld console and cartridges, but the shelf is now a **continuous physical environment**, not a row of cards on the Android background. It needs coordinated but different materials: recessed backboard, horizontal shelf deck/lip, machined edge highlights, inset grooves, soft shadows, section labels, deliberate vacant cells, and drag/focus treatment.

| Role | Original | Pink |
|---|---|---|
| Backdrop | `#0B131B` | `#221722` |
| Recessed back panel | `#14212D` | `#382536` |
| Shelf deck | `#293847` | `#69445D` |
| Front lip | `#485B6C` | `#9E6B88` |
| Machined highlight | `#5D7885` | `#D99CBA` |
| Deep groove | `#0C1923` | `#261827` |
| Vacant slot | `#1D2C39` | `#4B3045` |
| Section text | `#EAF4F2` | `#FFF3F7` |
| Focus / drop target | `#4ED8C4` | `#FFE5F1` |

These are proposed **design tokens**, not already-applied Kotlin UI colours. Asset CI validates contrast; Android screenshots, dynamic-theme toggling, accessible contrast, drag focus and phone-sized rails require native QA.

## Screen direction

- **Home:** no tagline. Compact CARTDECK identity, Add Cartridges responsive to empty/populated state, one actual vertical bookcase. A compact Continue shortcut is optional, not a second shelf. Favourites and Search stay separate tabs.
- **Shelf:** cartridge spines physically stand in multiple slots on one continuous horizontal metal rail; scrolling moves the page **up and down only**, with a new rail below each row. Sections and deliberate vacancies are real visible entities in the arrangement.
- **Arrange mode:** pickup/drag shows a brighter focusOutline on the back panel, raised shadow and insertion preview. Dragging into an empty slot is explicit; release commits the intended position atomically, Undo available. Same layout on restart/rescan.
- **Shelf colour system:** changing the CARTDECK edition changes cartridges **and the shelf**, with their own complementary but coordinated material palette. Personal custom colour editors may override shelf materials separately, preserving contrast and hardware geometry.
- **Cartridge front:** local custom image → existing indexed image URL → original fallback; default contain, never blanket crop. One cover/detail, explicit Open/Resume.
- **Selection spectacle:** after shelf grid and drag model stabilise, lift selected cartridge from its **actual grid bounds**, rotate to the front and settle; Back restores scroll, accessible reduced-motion static alternative. Do not couple to game loading.

## Correct source and integration boundaries

`HomeScreen.kt`, `HomeViewModel.kt` and `CartridgePagedShelf.kt` currently contain horizontal `LazyRow` or 10-result carousel logic; source UI is not ready for a persistent library. Render using lazy vertical rows / adaptive grid with visible shared rails and stable keys. Keep `GameInteractor.onGamePlay`, `Game.displayCoverUrl()`, `CustomCoverManager` and indexed game metadata.

`GameSearchDao.kt` directly binds raw user input to FTS4 `MATCH`, while `SearchViewModel.kt` already debounces but fabricates a 500ms loading delay. Preserve Paging and existing Search tab, safely compile FTS prefix terms and index both original/custom titles, use actual loading states.

**No destructive database migration:** versioned presentation-only shelf storage; stable system/file reference reconciliation for rescans; transactionally persist ordered sections, cells and vacancies. Protect existing game, cover, settings and save files, package signature and app data. Search/Favourites stay independent of shelf order and category labels.

## Implementation staging (Issue #43)

- P1-A1R / #40: restore original cover lookup; remove tagline; correct physical spine size/alignment, remove duplicated Continue rack. **Verify merged status on latest master first.**
- P1-A2A / #43: single vertical shelf; section/add/rename/reorder/remove, persisted empty cells, drag/drop and a clear edit mode, lazy large-library loading; **render Original/Pink bookshelf palette here**, not later.
- P1-A2B / #43: live prefix and multi-token search, safe SQL/FTS handling, both original and custom titles, separate vertically scrolling search results, real paging load state.
- P1-A2C / #40 + #43: animated lift/turn/cover and more tactile spine presentation, after stable layout; no fake loading delay and reduced motion.

One reviewable PR per phase, Android build, screenshots, in-place installed upgrade and independent Cassia/Sienna/Daniel/Marcus checks. Successful palette JSON/unit tests are not runtime acceptance.
