# PS2-NEXT-B — CARTDECK embedded Android host, controlled test lane

Status: **code present for debug-only JNI probe; a successful native build, installable APK, BIOS/resources integration and gameplay are not yet proven**. Parent [#218](https://github.com/safetyblade/ROMMachine/issues/218), feasibility [#222](https://github.com/safetyblade/ROMMachine/issues/222).

## Implementation boundary

The normal CARTDECK Play! PS2 launcher, all other consoles, game library, save-state logic and release configuration are unchanged.

The Android-only native prototype uses an actual embedded (not external-app) JNI interface with the original native symbol class name `kr.co.iefriends.pcsx2.NativeApp`, because the pinned upstream ARMSX2 binary exports JNI methods with that name. The minimal bridge and `com.armsx2.BiosInfo` constructor live exclusively under `lemuroid-app/src/debug/`; **they are not proof of a complete upstream Java/Kotlin callback contract.** Build the full callback inventory before releasing or moving them to main.

The unlisted `Armsx2RuntimeProbeActivity` runs in private `:ps2next` Android process on debug variants only. Its code:
- loads exactly `libemucore_4k.so` or `libemucore_16k.so` based on Android runtime page size, never Linux or emulator APK binaries;
- checks real native load success, then initialises an independent data root and user BIOS directory;
- attaches a real Android `SurfaceView` to the engine, requiring a published surface before starting a native VM thread;
- uses the upstream JNI `runVMThread`, pause, resume, shutdown and exact `setPadButton` key/index values for both sticks;
- refuses unknown pad buttons (the native code wrongly defaults unknown indices to Cross);
- blocks missing private test ISO, BIOS, resources or native libs with explicit `PS2_NEXT_BLOCKED` logs;
- refuses arbitrary ContentResolver access (the debug probe accepts only the explicitly staged private fixture);
- does not mark VM return, native load, or first frame as successful gameplay.

The upstream Java class performs much more than the minimal debug bridge. Its JNI callbacks for SAF sibling lookup, settings, sound, auto-tune, PS2 touch/physical controls, runtime resources and memory-card management must be separately reviewed before the integration can support real library launch. In particular, missing JNI callbacks cannot be declared harmless simply because a source build passes.

## Permanent library and storage contract

`Armsx2Storage.paths(filesDir, game.cartridgeId)` derives independent per-game roots from the already persisted immutable UUID:
- `files/native/ps2/armsx2/cartridges/<cartridgeId>` — ARMSX2 game data, renderer config and native `memcards` files;
- `files/native/ps2/armsx2/bios` — BIOS provided legitimately by the user;
- `files/native/ps2/armsx2/cartridges/<cartridgeId>/resources` — independently reviewed source resource payload.

Storage never indexes by title or mutable file URI. It does not read or modify Play!'s `Play Data Files/vfs/mc0` / `mc1`, nor does it migrate memory cards. Real engine preference/rollback remains unchanged until a working native core and private APK are verified.

## Private hardware probe preconditions

Only use a lawful test disc and the owner's own PS2 BIOS. The probe's app-private fixture is `files/ps2-next-lab/fixture.iso`; no external URI, ROM search or download is accepted. The app-private ARMSX2 BIOS and resources must already exist at the locations above for the fixed debug-only cartridge UUID `c3882b50-8b41-47cf-8219-f00b70c79e20`.

A tester can use ADB with their installed debug package's own `run-as` access to stage a lawful test fixture; the native core must come from the source-built, SHA-verified, licence-reviewed ARM64 artifact staged into an explicitly configured private engineering build. Do not use a standalone ARMSX2 APK, no BIOS/ROM in the repository, no unsupported APK sideload advice. In the debug package, the test-only Activity name is `com.swordfish.lemuroid.app.debug.Armsx2RuntimeProbeActivity`.

The debug-only bridge does not yet package ARMSX2's complete native transitive dependencies. A missing `libSDL3.so`, `liblibrashader_capi.so`, `libc++_shared.so`, or any native ABI/16KiB mismatch must block rather than be hidden by a fallback. The native build and exact dynamic-dependency closure are the immediate prerequisite.

## Release gates

1. Native: reproducible upstream ARM64 compile; correct 4K/16K page model, 16KiB ELF, JNI exports, SHA/BuildID, dependency closure, licence SBOM. Use the separate CI workflow.
2. Debug host: compile, JVM tests, missing-prerequisite blocks, user-supplied BIOS and source resource staging. Test full JNI callback contract on device before calling it stable.
3. Device: actual Soulcalibur III match on Samsung SM-S721B, observed input/audio, emulator clock, 15-minute thermal and graphics tests, genuine in-game memory-card save/reload.
4. Production: a fully embedded host, gated per-cartridge engine selection, resilient native process crash return, separate assets and reversible Play! fallback. Only then switch any default.

**No release acceptance or gameplay success is claimed by merging native JNI/host scaffolding.**
