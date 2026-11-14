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
