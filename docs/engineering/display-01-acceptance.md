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
