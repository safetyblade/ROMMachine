# Elliot + Marcus — executable engineering and independent verification handoff
**Prepared 15 Nov 2026; NOT an executed build/QA.** Do not compete with active Wii U emulation Codex work.

## Live-lane status at preparation
Latest observed active Wii U change PR #263: loader release, Vulkan surface oldSwapchain and controller-resize swapchain handoff after owner stall on APK 90655157. PR #260 addresses Cemu native mutex teardown and sideways rotation, PR #257 pacing/audio and queue stalls. PR #259 X360 offline gamer/save profile candidate and PR #258 PS2 SDL JNI mismatch exist. No assumption that these PRs have merged or passed hardware. Review actual states at handoff.
Current committed GC-01 source branch: engineering/gameplay-consistency-gc01; docs only, no gameplay runtime changes. Keep it parked until emulation lane's accepted boundary.

## Required exact-source preflight commands
git fetch --all --prune; git checkout master; git pull --ff-only; git rev-parse HEAD; git submodule status --recursive; git branch -a -vv; git status --short;
Inspect git log and PR heads for #200 #231 #252 #257 #258 #259 #260 #263 and any later changes. Trace the app module sources using rg -n 'GameMenuActivity|GameViewModelInput|initializeGamePadKeysFlow|GLRetroView|AlertDialog|InputDevice|Cartdeck|oldSwapchain|SaveBinding|Controller' lemuroid-app/ and native/ (or verified actual paths). Record exact file/function/method ownership; do not guess path names.
Identify full combined ARM64 debug workflow and run Gradle lint/JVM/Room tests and actual packaged APK build on the exact candidate SHA. Confirm versionCode/appId/debug signing compatibility before offering an install-over. Keep real game tests only on legal user-owned assets.

## Work packages (all eight tracked; no cherry-pick)
GC-01A app-owned native menu, engine action adapter and distinct native save vs savestate operations: #191 #200.
GC-01B pixel-aspect/viewport fit, top-line bleed, tablet/TV and DS layout: #74 #162 #165 #166 #167.
GC-01C device capabilities/calibration/port/held/chords/remap/reconnect: #177 #179 #181 #231.
GC-01D Wii profile input and supported pointer/motion declaration: #85.
GC-01E reproduce/fix PivotArc+GripArc fail and original controls: #135.
GC-01F action-verified exact title Modern Function resolver: #172 #176–181.
GC-01G durable Xbox360 profile and save continuity: #197 #255 #259.
GC-01H PS1/PSP and all existing engine regression #56 #93 #114 #261; coordinate separate WiiU/PS2/X360 runtime work.
At each slice require architecture approved input/menu/render/storage contract, Sienna UX review, same-SHA CI, explicit affected engine regression and immutable rollback. Do not switch emulation core without a justified tested fallback.

## Acceptance matrix — Marcus fills actual observations, never inferred from source
| Test / fixture | Source/CI expectation | Physical evidence required | Status now |
| --- | --- | --- | --- |
| GB/GBC/GBA known working games | Existing control/menu/save tests stay green | same library UUID, reopen SRAM and state separately | NOT TESTED on GC-01 |
| Native original Xbox | Branded menu no engine AlertDialog; input release | true title running frame/audio, pause/resume/quit, physical pad | NOT TESTED |
| PS1 | Existing/default vs refreshed core fallback | actual launch/gameplay and memory-card state | NOT TESTED |
| PSP | same-hash standalone PPSSPP A/B for mod #261; Soulcalibur #114 | 3D, texture labels, save, pause; no false ISO diagnosis | NOT TESTED |
| Wii | Sideways/Remote+Nunchuk/Classic capability | actual mapped buttons + true limits on pointer/motion | NOT TESTED |
| PivotArc + GripArc | layout & native inventory solver fixtures | Samsung previously FAIL; must retest left/right, held, chording | FAIL previously / GC-01 NOT TESTED |
| Physical BT + TV | gamepad classification + UI focus tests | controller connect/disconnect, player ports, touch fallback and TV remote | NOT TESTED |
| DS | dual-frame viewport tests | two visible screens and accurate stylus coordinates | NOT TESTED |
| Xbox360 | account/adoption/profile source tests | genuine Xenia profile write/read and install-over, same game progress | NOT TESTED |
| WiiU/PS2 | keep existing source/CI stable, no bypass | independent native host freeze/resize/pause/save after live lane | LIVE EMULATION / NOT GC-01 TESTED |

## Exact APK + hardware test record per build
BaseSHA / HeadSHA / submodules; CI workflow URL and check outcomes; APK artifact link, SHA-256, appId, signer fingerprint, versionCode/ABI; Samsung exact model/OS, other tablet/TV available; controller descriptor and mapping; legally supplied game/edition hash; installed prior version and same-signing install-over; timestamp of launch, playable frame/audio, controller, pause/menu/save/reopen; logs/tombstone/screenshots; actual PASS/FAIL/NOT TESTED and issue URL; exit criteria for rollback.

## Reversible release discipline
Baseline protect library UUID, Room v13 and earlier migrations, frozen SaveBinding, RAM/SRAM/memory cards, profile, artwork, shelves, appearances and SAF grants. No uninstall, clear-data, version downgrade or signature change. Every changed runtime boundary uses a narrowly scoped PR, no parallel branch force merge, no dependency/core upgrades without evidence. Stop after regression, hold release, revert code or opt out via tested feature flag and preserve user data.

## Sequencing / authority
1. Finish and stabilise current WiiU/emulation Codex lane (do not interpret PR as finished). Verify actual release/control handoff; documented exact integrated APK.
2. Cassia register explicitly reviewed, Daniel/Sienna designs explicitly reviewed, Elliot verified source and test path ready.
3. Start GC-01A and parallel GC-01H investigation, then other packages dependency aware; shared source merges serially.
4. Marcus tests on available physical Samsung, Bluetooth pad, tablet/TV; unavailable equipment is NOT TESTED, not PASS. Hold consolidated #248 campaign for overall release.
5. Cassia recommends and Jonathan alone accepts; do not self-certify.
**Gate now: DELIVERY AND QA PROTOCOL PREPARED; CI / GAMEPLAY / INDEPENDENT REVIEW PENDING.**
