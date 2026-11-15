# GC-01 PREP-C — Elliot Shaw engineering readiness & Marcus independent QA handoff

**Review date:** 15 November 2026. **Readiness:** PREPARATION COMPLETE; IMPLEMENTATION/GC-01 CI/DEVICE QA NOT STARTED. Owner has approved the complete Sienna UX scope. Source-only findings are distinguished from test evidence. Cassia #264 and design #265 are closed as planning/design gates. Programme implementation remains #262.

## 1. Verified current source and CI — evidence ledger
| Evidence | Source / identifier | Actual result | Does NOT establish |
|---|---|---|---|
| Master | `7362f442f88dc2de2260030ab11fc08e24fb8271` | Latest inspected master; PR #263 merged | Wii U device gameplay pass |
| Master combined APK | [Actions run #43678592274](https://github.com/safetyblade/ROMMachine/actions/runs/43678592274) | **SUCCESS** on same master SHA; Gradle ktlintCheck, freeBundleDebug unit tests, assemble and app/native packaging verifiers completed successfully | GC-01 code implemented, game render/save or input QA |
| Published artifact | [Run #43678592274 artifacts](https://github.com/safetyblade/ROMMachine/actions/runs/43678592274) | `CARTDECK_CONNECTED_PS2_WIIU_X360_ARM64_DEBUG` artifact ID **14146834742**, unexpired at inspection; 189,867,081 bytes ZIP archive | APK SHA256, local installed signature or actual owner device pass until extracted |
| Native symbols | Same run | `connected-emulator-native-symbols` ID **14146944334** (397,637,499-byte ZIP) | no runtime exceptions |
| Source/branch conflict | GitHub compare `master...engineering/gameplay-consistency-gc01` | `diverged`, GC01 docs branch behind master 4 commits, ahead 12 at inspection; seven changed files docs/spec/prototype only | safe fast-forward merge to master |
| Upstream PRs | #252 #257 #258 #259 #260 #263 | GitHub reported **MERGED**, native source available | accepted PS2/WiiU/X360 game playback |
| Working input/menu foundation | existing `SharedGameMenu.kt`, `GameMenuCapabilities.kt`, `GameViewModelInput.kt`, `XboxGameMenu.kt`, `Xbox360Activity.kt` | File and call-path anchors found | same pause/controller support in all engines |
| Library continuity foundation | `CartridgeSaveBinding.kt`, `Xbox360Profiles.kt`, Room test workflow | Atomic/cartridge identity and native profile source present | full GC-01 upgrade and save roundtrip on Samsung |

The actual connected APK workflow `.github/workflows/cartdeck-connected-engines-apk.yml` runs `ktlintCheck :lemuroid-app:testFreeBundleDebugUnitTest :lemuroid-app:assembleFreeBundleDebug` with `cartdeck.dev.standaloneId=true`, `cartdeck.p0.phoneArm64Only=true`, `cartdeck.p1emu.enabled=true`, `cartdeckLibretroDroidVersion=0.14.0-cartdeck3`, pinned verified native bundle and version code from `scripts/ci/engineering_version_code.sh`. It then runs `scripts/ci/verify_standalone_apk.sh`, `verify_native_bundle_apk.py` and `verify_connected_apk.py`, publishing preserved engine evidence and exact source SHA. Do not substitute a plain Gradle APK lacking those native payloads.

## 2. Direct code targets and invariants — READY FOR IMPLEMENTATION
| GC-01 | Edit/reuse path(s) on a fresh branch from latest master | Source-backed task and tests |
|---|---|---|
| A Menu | `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/{GameMenuCapabilities,SharedGameMenu,GameMenuActivity,GameMenuHomeScreen,XboxGameMenu}.kt`; `.../shared/GameMenuContract.kt`; `.../tv/gamemenu/TVGameMenu{Activity,Fragment}.kt` | Preserve shared Compose already used; extend truthful capability outcomes and native recovery, never invent second menu or fake savestate. Test `GameMenuCapabilitiesTest`, `SharedGameMenuTest`, `LibretroGameMenuTest`. |
| B Display | `.../mobile/feature/game/{MobileGameScreen,GameScreenLayout,CartdeckViewportPolicy}.kt`; `.../shared/game/wiiu/WiiuViewport.kt`; `spikes/wiiu/vulkan-host/{CartdeckVulkanPresenter,SwapchainContinuity}.h/.cpp` | Fit actual native content ratio inside measured safe rect, no crop/stretch; retain DS/stylus; preserve Cemu window lifetime. Test viewport/console layout classes and real screenshots. |
| C Physical controller | `.../shared/game/viewmodel/GameViewModelInput.kt`, `.../shared/input/{GameplayControllerPresence,ControllerHardwareProfile}.kt`, `.../mobile/feature/gamemenu/{PhysicalControllerMappingPanel,XboxPhysicalMappingPanel}.kt` | Reuse one per-host input authority, no duplicate listener; capture/test/held/axis/chords/multiport/reconnect; reserve menu escape. Extend mapped input gate and session tests. |
| D Wii | `.../shared/input/WiiControllerCompatibility.kt`, `GameViewModelInput.selectWiiController`, `docs/engineering/wii-cartridge-controller.md` | Only core-advertised modes; real button actions and no fake IR/motion. Use Wii compatibility tests and game fixtures. |
| E GripArc/PivotArc | `.../mobile/feature/game/MobileGameScreen.kt`, `.../mobile/feature/gamemenu/GripArcEditorScreen.kt`, `lemuroid-touchinput/src/main/java/com/swordfish/touchinput/radial/settings/NativeControlInventory.kt` | Native-control inventory never lost; Standard+GripArc/PivotArc+GripArc both sides and G1–G4; safe session-only fallback. Test combined solver/geometry and actual Samsung defect. |
| F Modern Function | existing `InputDeviceManager` mapping path and documented title matrix #172/#176–181, stable Game/cartridge identity | Verified exact release action semantics only; explicit opt-in/Undo/authentic fallback. No generic swaps. |
| G Xbox360 profile | `.../shared/game/xbox360/{Xbox360Activity,Xbox360Profiles,Xbox360Pad}.kt`, existing Xenia profile assets | Preserve offline XUID and native save; no claim of manual savestates. `Xbox360ProfilesTest` plus real game save/reopen/install-over. |
| H Emulation quality | current PS1/PSP engine pins, #56/#93/#114/#261, native host journal/recovery and #247 | Baseline vs revised core A/B; same exact ROM/mod/asset/hash; PS1/PSP 3D, labels, controls, saves and no regression on GB/GBA/DS/Wii/DC. |

**Native host trace remaining at code kickoff, not a design blocker:** locate the exact activity/command threading path for Cemu/WiiU and ARMSX2 PS2 *on the latest implementation SHA*, plus their JNI callbacks. Engine registry `NativeEngineRegistry.kt` includes hosts `LIBRETRO, HAKUX_SDL3, XENIA_ANDROID, CEMU_VULKAN, ARMSX2_ANDROID`; host execution is not interchangeable. The source-only review did not dynamically demonstrate the PS2/WiiU pause hooks. If missing, mark those specific capabilities unavailable with truthful fallback pending source-proven adapter.

## 3. Engineering implementation control — no circular planning
1. **Start only after current live emulation owner handoff:** snapshot exact new master SHA; preserve complete combined baseline APK run/artifact, signer/version and rollback instruction. Avoid rebasing the *old* docs-only branch to rewrite native runtime; use a fresh `engineering/gc01-native-gameplay` branch from the accepted master and link this complete doc.
2. Integrate one narrow shared menu/capability outcome seam first, reusing `GameMenuPauseGate`, `GameMenuContract` and each current UI host. Each PR includes executable tests, no core swaps, no package or database mutation.
3. Implement viewport/native touch transforms and controllers through established ownership; update exact host adapters, preserve Vulkan window/swapchain. Each slice must include regression check of already functioning consoles.
4. Integrate the whole A–H scope in same-SHA complete connected ARM64 build workflow; no isolated source green masquerading as integrated APK.
5. Elliot reports for each change: requirement ID, old/new source path, code PR, unit/integration checks, exact CI URL+SHA, APK artifact ID, app ID/version/signature, rollback route, remaining issues. Marcus independently records actual hardware PASS/FAIL/NOT TESTED; he does not inherit engineering PASS.

## 4. Marcus independent physical verification matrix (UNEXECUTED)
Statuses below refer to GC-01 candidate, not historical owner play. Fixture must be lawful user-supplied; record title/region/edition, media/patch hash and device.
| Test ID | Fixture | Exact demonstrated outcome required | GC-01 status |
|---|---|---|---|
| QA-A01 | GB/GBA + PS1 core | Native menu open/close/Back without stuck input, real savestate slot write and load when supported | NOT TESTED |
| QA-A02 | Original Xbox + X360/PS2/WiiU if playable | Same CARTDECK menu; native in-game save explanation, no fake savestate, recovery | NOT TESTED |
| QA-B01 | GB/GBC/GBA, 4:3 PS1/DC, PSP 16:9, DS | Maximal FIT, no crop/stretch, no top bleed; DS screens+stylus coordinates | NOT TESTED |
| QA-B02 | WiiU Vulkan active title | Menu, physical controller resize, orientation without swapchain/black-frame stall | NOT TESTED |
| QA-C01 | Samsung Bluetooth Xbox/PS/generic pad (actual available hardware recorded) | Connection, mapping, press+release, analog, triggers, diagonal/chord, hotplug and touch return | NOT TESTED |
| QA-D01 | Wii title and compatible modes | Core-advertised attachment, per-cart save, game control/motion truth | NOT TESTED |
| QA-E01 | Samsung Standard+GripArc and PivotArc+GripArc, both sides | Native control inventory, 48dp taps, G1–G4 and session fallback; prior owner PivotArc defect remains historical FAIL | NOT TESTED on new APK |
| QA-F01 | Each EXACT verified title/edition matrix entry | Confirm actual Modern Function action/Undo/Authentic fallback, not just mapped letter | NOT TESTED |
| QA-G01 | Real X360 game with stable offline XUID | in-game save, force stop/reopen, app install-over, same progress/profile | NOT TESTED |
| QA-H01 | PS1 and PSP failure fixtures incl #261 mod | Same-hash standalone baseline A/B, playable graphics, save and no premature file-blame | NOT TESTED |
| QA-P01 | Android TV remote, tablet, phone orientation where devices exist | Focus, 200% font, no touch overlay on TV, back/exit, accessible safe zones | NOT TESTED |
| QA-DATA | Owner's existing installed app/library | same-signature install-over, 2+ imported carts, save SRAM/slots and profiles, Room/SAF/art/shelf/appearance retained | NOT TESTED |

**Marcus proof record template per test:** Date/time, QA ID, test reporter, run/commit SHA, APK URL/artifact ID, SHA-256 APK, package/versionCode/signature, device manufacturer/model/Android API, game hash and source permission, test steps with timestamps, screenshots or video+logcat/tombstone, observed frame/audio/input/save, result PASS/FAIL/BLOCKED/NOT TESTED, exact bug issue and rollback recommendation. A compile is neither a test of game pixels nor a player acceptance gate. Missing tablet or TV equipment = NOT TESTED, not an inferred PASS.

## 5. Rollback and preservation — mandatory
Pre-install record current application ID/signer/version, working APK SHA, exact master SHA and current owner save/library state, private SAF grants. Never uninstall, clear app data, replace signature or downgrade Room. No modify/overwrite original saveBinding (especially user-confirmed ambiguous legacy SRAM), per-engine PS2 cards/Wii NAND/X360 HDD+offline XUID/PSP savedata, shelf/appearance/cover/ROM edits. On engine regression revert bounded adapter code or turn off *already verified* feature flag; do not claim restoring older incompatible APK works by downgrading versionCode. Verify upgrade and rollback path in controlled test environment before offering to owner.

## 6. Gate decision and limits
**Elliot: ENGINEERING EXECUTION READINESS = COMPLETE** (current master, merged PRs, successful connected baseline CI, artifact IDs, detailed paths, code coverage, scope, test/rollback specified).
**Marcus: INDEPENDENT VERIFICATION PLAN = COMPLETE; HARDWARE EXECUTION = NOT TESTED.** No device access or installed user data is available to this repository audit. 
**GC-01 implementation/release = NOT STARTED/NOT ACCEPTED.** #266 can be closed strictly as the readiness handoff, not as an eight-workstream delivered gate. #262 remains OPEN for actual code and #248 OPEN for independent QA.
