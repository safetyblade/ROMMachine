# P1-EMU-WIIU recovery

Baseline: `a3407cee11fbb8e78002200103f1b08aa2a4663d`, successful
[master APK run](https://github.com/safetyblade/ROMMachine/actions/runs/43587058727).
This lane is **INCOMPLETE ENGINEERING**, not bulk-QA-ready.

Recovered selectively from `feat/wiiu-vulkan-private-apk` at
`95dfdfd88241ddcd2e4d2f80e25945761c6fe0c8`: the Vulkan presenter,
Libretro runner, isolated debug Activity, native staging and private APK CI.
Those files were absent from the baseline despite merged PRs #240/#241.
No old branch tree or application database was merged wholesale.

Implemented beyond recovery:

- Real AAudio signed-stereo output using the core-reported rate, bounded
  writes, partial-consumption reporting, format validation, device-error
  diagnostics and synchronized stream reconfiguration/cleanup.
- Both analog sticks, stick clicks, hat/triggers and full-output touch
  coordinates wired through the pinned Cemu input callback ABI.
- Native audio tests execute the actual sink against a deterministic AAudio
  device double; they test failure/short-write behavior, not speaker playback.
- Explicit vcpkg NDK discovery and chainloaded toolchain selection. The
  nested triplet probe otherwise searches the absent Xamarin NDK location.
- Host hash checked alongside core hash when staging the private bundle.

Local Android NDK 29 compilation of the Vulkan host plus AAudio succeeds.
This proves source compatibility and linkage, not Vulkan initialization,
guest images, sound, input acceptance or save correctness on a device.

Remaining implementation gates: normal cartridge Open/Return integration,
resource onboarding, shared touch shell/menu, controller choice and screen
selection, lifecycle/recreated-surface acceptance, complete native dependency
and provenance verification, full application JVM/Room/APK checks and signed
installable artifact. The private probe still uses `files/wiiu-lab/fixture.wua`
and does not make catalogue cartridges executable.

The source-built Cemu/core must be packaged by the dedicated workflow, never a
placeholder. The separate application ID is `dev.prestwich.cartdeck.wiiulab.debug`;
the standard APK, Dolphin's Wii NAND and current shelf identities are unchanged.
No save-state support is claimed. Future Resume must reopen persistent NAND saves.

Physical QA #248 must eventually verify lawful media/resources, stock Xclipse
Vulkan initialization, valid changing game images, audible output, full input,
GamePad touch/screen choice, save/exit/reopen and install-over/rollback. No APK
hash or download is supplied until packaging and its verification succeed.

## Cartridge-host continuation

The opt-in `dev.prestwich.cartdeck.wiiulab.debug` build routes Wii U shelf Open
into its isolated Cemu process. Ordinary builds retain the catalogue gate. It
uses the existing CARTDECK shell, Classic touch layout, physical controller
aggregation and menu with independent Wii U preferences. GamePad/Pro selection
uses the pinned core's actual controller IDs. TV/GamePad switching sends the
configured Tab edge through the core's keyboard input. Aspect-fit pointer
coordinates exclude letterbox bars. Save-state actions remain absent.

Media uses read-only SAF staging, including RPX companion trees. NAND is under
`native/wiiu/cartridges/<immutable UUID>/saves/Cemu/mlc01` (the precise `Cemu`
subdirectory follows the pinned core's save-directory handling); changing source
URI or disc cache does not move it. System resources are separate. The menu can
import owner-provided keys and a Cemu resource directory after a failed launch;
it refuses a top-level mlc01 resource import. No keys or firmware are bundled.
Engine packages hash both the real Cemu core and frontend host; an adapter source
revision changes package identity when only the host changes. Rollback selects
a complete previous pair and retains NAND. Required native exports, BuildID,
16 KB ELF alignment, dependency closure and installed dependency licence texts
are checked before staging. The host preserves stock Vulkan and reports GPU,
image callbacks and submitted audio separately from gameplay acceptance.

Local native evidence: pinned Cemu plus the documented manifest-option patch
linked to a 27,351,600-byte AArch64 ELF, SHA256
`f64d8f073b075cb0a7aabda35feee861955a4bca9f98e14274ad715fa006778c`.
The updated real Android host also links and passes ABI/export checks. This
working-tree continuation still requires its own clean CI/APK evidence and
physical acceptance. Earlier APK run43592446783 passed at8e8cf143; that artifact
contains the earlier probe, not the cartridge integration described above.

### Physical QA sequence (#248)

Install the opt-in APK alongside the baseline; add lawful Wii U media through
the normal library. Open from cartridge detail. For encrypted media, use only
owner-owned keys/resources, return to Shelf and reopen after import. Test a
container and an extracted RPX with code/content/meta. Verify visible changing
game images and audible output, all Nintendo face/shoulder buttons, both sticks,
stick clicks, physical reconnect, Standard/GripArc and PivotArc fallback. Switch
TV/GamePad and verify touch positions including bars. Open the menu, background
and foreground, rotate/recreate the surface, then resume. Save in-game, return
to Shelf and reopen. Check the same save after an APK update and a complete-pair
rollback. Verify ordinary Wii games and their existing NAND remain unchanged.
Copy the native report on any Vulkan, load, audio or shutdown failure. Record
Xclipse performance separately; neither an image callback nor a compilation pass
establishes playability.
