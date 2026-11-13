# DISPLAY-01 — Marcus independent acceptance matrix
**Status:** all cases NOT TESTED until integrated native APK.

| Device/profile | Control | Game screen | Menu/navigation | Dual screen | Pass gate |
| --- | --- | --- | --- | --- | --- |
| Phone portrait | Touch | Original CARTDECK shell | Existing native settings | DS two screens and stylus | No regression |
| Phone landscape | Touch only | Fill default with usable overlay | Open Display, select Native | DS blocked / rotate prompt | Frames fill actual panel, buttons usable |
| Phone landscape | Gamepad | Fill default, no compulsory touch | Gamepad shortcut + menu | DS blocked | No ghost keys |
| Tablet portrait 600dp | Touch | Tablet shell/game safe area | Responsive shelf & details | DS portrait supported | No tiny cards |
| Tablet landscape 600/840dp | Touch/gamepad | Fill default, native option | Accessible from controllers/touch | DS blocked | Correct orientation & focus |
| Actual Android TV | Basic remote D-pad/OK/Back | No touch UI, actual Fullscreen Fill | Whole app and in-game menu accessible | DS clearly unavailable | Remote-only menu reachable and exits safely |
| Actual Android TV | Gamepad | Fullscreen Fill/Native toggle | Menu shortcut or MENU, Save/Load & Exit | DS clearly unavailable | No lost gamepad ownership |
| 1080p and 4K TV | Gamepad | 4:3 Fill stretched without crop, Native letterbox | Display mode change live | N/A | Native GL surface screenshot shows actual content and correct source ratio |
| Game systems | GB/GBC/GBA, SNES, PS1/PS2, PSP, GC, Dreamcast, Wii, Xbox where supported | No engine regression | Standard native controllers & settings retained | N/A | Real gameplay and save/resume |
| DS/3DS | Portrait handheld/tablet | Existing two-screen/stylus | No forced TV/landscape mode | Native DS unchanged | No corrupted touch/save |

## Engineering proof requirements
- Unit: screen class classification, landscape defaults, DS eligibility, touchscreen visibility; focus/navigation tests; tablet width changes.
- Native host: test both Fit and Fill in `VideoLayout` with 4:3 and 16:9 geometry at 1080p/4K; verify no crop and pointer mapping compatibility; Android host must export toggle to Kotlin, and timing remains healthy.
- Integrated: green build + real ARM64 APK and install-over; presenter/remote focus, MENU/long-press D-pad select path, screen rotation/split-screen, controller hotplug.
- Owner and Marcus record platform, dimensions, source commit, APK version/SHA, state PASS/FAIL/NOT TESTED. Do not close #162 from simulator-only checks.

## Universal controller-driven fullscreen check (owner update)
| Scenario | Expected visible controls | Frame placement | Menu path |
| --- | --- | --- | --- |
| Phone landscape, enabled gamepad | No Standard/PivotArc/GripArc, no physical shell | Entire safe display area; actual Fill after renderer patch | Gamepad shortcut |
| Tablet landscape, enabled gamepad | None | Entire safe display area; actual Fill after renderer patch | Gamepad shortcut |
| Phone/tablet landscape, no enabled pad | Touch controls return immediately | Fullscreen background with functional overlay | Touch menu |
| Phone portrait, enabled pad (native shell mode) | Buttons hidden; do not auto-enter fullscreen | Original selected viewport until fullscreen explicitly chosen | Hardware menu |
| Phone/tablet portrait, explicit fullscreen and enabled pad | None, no shell decorations | Entire safe display; Fill or Native as selected | Hardware menu |
| TV with or without gameplay controller | No touch UI ever | Entire safe display; Fill or Native as selected | Remote/controller |
| Gamepad hot-unplug mid-game | Touch-only phone/tablet controls restored; TV stays clean | No emulator restart or lost hold state | Touch/remote menu |

**CI must explicitly test the GripArc exception and decorative shell, not only the existing Standard PadKit visibility flow.**
